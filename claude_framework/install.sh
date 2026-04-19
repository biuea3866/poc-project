#!/usr/bin/env bash
# claude-framework 플러그인 로컬 설치 스크립트.
#
# Claude Code 플러그인 시스템은 다음 3가지가 모두 필요:
#   1. 마켓플레이스 등록: ~/.claude/plugins/marketplaces/<mp>/.claude-plugin/marketplace.json
#   2. 마켓플레이스 레지스트리: ~/.claude/plugins/known_marketplaces.json
#   3. 플러그인 설치: ~/.claude/plugins/cache/<mp>/<name>/<version>/
#   4. 플러그인 레지스트리: ~/.claude/plugins/installed_plugins.json
#
# 사용법:
#   bash install.sh                  # 기본 설치 (심볼릭 링크)
#   bash install.sh --copy           # 복사
#   bash install.sh --uninstall      # 제거
set -euo pipefail

PLUGIN_NAME="claude-framework"
MARKETPLACE="${CLAUDE_FRAMEWORK_MARKETPLACE:-claude-framework-local}"
SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_ROOT="$HOME/.claude/plugins"
MP_DIR="$PLUGINS_ROOT/marketplaces/$MARKETPLACE"
CACHE_DIR="$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME"
KNOWN_MP="$PLUGINS_ROOT/known_marketplaces.json"
INSTALLED="$PLUGINS_ROOT/installed_plugins.json"
MODE="${1:-}"

log() { printf "\033[0;36m[claude-framework]\033[0m %s\n" "$*" >&2; }
err() { printf "\033[0;31m[ERROR]\033[0m %s\n" "$*" >&2; exit 1; }

read_version() {
  python3 -c "import json; print(json.load(open('$SOURCE_DIR/.claude-plugin/plugin.json'))['version'])"
}

install_marketplace() {
  mkdir -p "$(dirname "$MP_DIR")"
  [ -e "$MP_DIR" ] && { log "기존 마켓플레이스 제거: $MP_DIR"; rm -rf "$MP_DIR"; }

  if [ "$MODE" = "--copy" ]; then
    log "마켓플레이스 복사: $MP_DIR"
    cp -R "$SOURCE_DIR" "$MP_DIR"
  else
    log "마켓플레이스 심볼릭 링크: $MP_DIR → $SOURCE_DIR"
    ln -s "$SOURCE_DIR" "$MP_DIR"
  fi

  [ -f "$MP_DIR/.claude-plugin/marketplace.json" ] || err "marketplace.json 누락"

  # known_marketplaces.json 갱신
  python3 >&2 <<PYEOF
import json, pathlib, datetime
p = pathlib.Path("$KNOWN_MP")
data = json.loads(p.read_text()) if p.exists() else {}
data["$MARKETPLACE"] = {
    "source": { "source": "local", "path": "$SOURCE_DIR" },
    "installLocation": "$MP_DIR",
    "lastUpdated": datetime.datetime.utcnow().isoformat() + "Z"
}
p.write_text(json.dumps(data, indent=2, ensure_ascii=False))
print(f"registered marketplace: $MARKETPLACE → $MP_DIR")
PYEOF
}

install_plugin() {
  local version; version="$(read_version)"
  local target="$CACHE_DIR/$version"
  mkdir -p "$CACHE_DIR"
  [ -e "$target" ] && { log "기존 설치 제거: $target"; rm -rf "$target"; }

  if [ "$MODE" = "--copy" ]; then
    log "플러그인 복사: $target"
    cp -R "$SOURCE_DIR" "$target"
  else
    log "플러그인 심볼릭 링크: $target → $SOURCE_DIR"
    ln -s "$SOURCE_DIR" "$target"
  fi

  local now; now="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
  python3 >&2 <<PYEOF
import json, pathlib
p = pathlib.Path("$INSTALLED")
data = json.loads(p.read_text()) if p.exists() else {"version": 2, "plugins": {}}
key = "$PLUGIN_NAME@$MARKETPLACE"
data.setdefault("plugins", {})[key] = [{
    "scope": "user",
    "installPath": "$target",
    "version": "$version",
    "installedAt": "$now",
    "lastUpdated": "$now"
}]
p.write_text(json.dumps(data, indent=2, ensure_ascii=False))
print(f"registered plugin: {key} @ $target")
PYEOF
  echo "$target"
}

uninstall() {
  # marketplace.json의 기존 @local 잘못 등록 제거도 함께 수행
  python3 >&2 <<PYEOF
import json, pathlib
for p, key_candidates in [
    (pathlib.Path("$INSTALLED"), ["$PLUGIN_NAME@$MARKETPLACE", "$PLUGIN_NAME@local"]),
    (pathlib.Path("$KNOWN_MP"), ["$MARKETPLACE", "local"])
]:
    if not p.exists(): continue
    data = json.loads(p.read_text())
    target = data.get("plugins", data) if p.name == "installed_plugins.json" else data
    changed = False
    for key in key_candidates:
        if key in target:
            del target[key]
            changed = True
    if changed:
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False))
        print(f"cleaned {p.name}")
PYEOF

  [ -d "$MP_DIR" ] || [ -L "$MP_DIR" ] && { rm -rf "$MP_DIR"; log "마켓플레이스 제거"; }
  [ -d "$CACHE_DIR" ] && { rm -rf "$CACHE_DIR"; log "플러그인 캐시 제거"; }
  # 구버전 경로들 정리
  for stale in \
    "$PLUGINS_ROOT/cache/local/claude-framework" \
    "$PLUGINS_ROOT/$PLUGIN_NAME"
  do
    [ -e "$stale" ] && { rm -rf "$stale"; log "구버전 경로 정리: $stale"; }
  done
  log "완료"
}

verify() {
  local path="$1"
  log "검증 중..."
  local missing=0
  for rel in \
    .claude-plugin/plugin.json \
    agents/pipeline-runner.md \
    skills/kotlin-spring-impl/SKILL.md \
    commands/init.md
  do
    [ -e "$path/$rel" ] || { printf "\033[0;31m  누락: %s\033[0m\n" "$rel" >&2; missing=$((missing + 1)); }
  done
  [ $missing -eq 0 ] && log "핵심 리소스 확인 완료" || err "$missing 개 누락"
}

print_next_steps() {
  cat >&2 <<EOF

============================================
 claude-framework 설치 완료
============================================

마켓플레이스: $MARKETPLACE
플러그인:    $PLUGIN_NAME@$MARKETPLACE
캐시 경로:   $CACHE_DIR/$(read_version)

다음 단계:
  1. VSCode/Claude Code 완전 재시작 (Cmd+Q 후 재실행 — 재로드 아님)
  2. 새 세션에서 확인:
     /plugin list                              # claude-framework 표시되어야 함
     /init --stack=kotlin                     # 슬래시 커맨드 자동완성
     @be-tech-lead, @prd-analyst              # 에이전트 호출

문제 발생 시:
  - 로그: VSCode 출력 패널 → Claude 채널
  - 재설치: bash install.sh --uninstall && bash install.sh
EOF
}

case "$MODE" in
  --uninstall)
    uninstall
    ;;
  --copy|"")
    install_marketplace
    path="$(install_plugin)"
    verify "$path"
    print_next_steps
    ;;
  -h|--help)
    grep '^#' "$0" | head -15
    ;;
  *)
    err "알 수 없는 옵션: $MODE (--copy|--uninstall)"
    ;;
esac
