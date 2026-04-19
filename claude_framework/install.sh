#!/usr/bin/env bash
# claude-framework 플러그인 로컬 설치 스크립트.
#
# Claude Code 플러그인 시스템 규격에 맞춰:
#   - ~/.claude/plugins/cache/<marketplace>/<name>/<version>/ 에 파일 배치
#   - ~/.claude/plugins/installed_plugins.json 에 등록
#
# 사용법:
#   bash install.sh                          # 현재 디렉토리를 플러그인으로 설치
#   bash install.sh --copy                   # 복사 (링크 대신)
#   bash install.sh --git <url>              # 원격 저장소에서 클론
#   bash install.sh --uninstall              # 제거
set -euo pipefail

PLUGIN_NAME="claude-framework"
MARKETPLACE="${CLAUDE_FRAMEWORK_MARKETPLACE:-local}"
SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_ROOT="$HOME/.claude/plugins"
REGISTRY="$PLUGINS_ROOT/installed_plugins.json"
MODE="${1:-}"

log() { printf "\033[0;36m[claude-framework]\033[0m %s\n" "$*" >&2; }
err() { printf "\033[0;31m[ERROR]\033[0m %s\n" "$*" >&2; exit 1; }

read_version() {
  local manifest="$1/.claude-plugin/plugin.json"
  [ -f "$manifest" ] || err "plugin.json 없음 — $manifest"
  python3 -c "import json; print(json.load(open('$manifest'))['version'])"
}

ensure_dirs() {
  mkdir -p "$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME"
}

register_plugin() {
  local version="$1"
  local install_path="$2"
  local now
  now="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"

  python3 >&2 <<PYEOF
import json, os, pathlib
registry_path = pathlib.Path("$REGISTRY")
if registry_path.exists():
    data = json.loads(registry_path.read_text())
else:
    data = {"version": 2, "plugins": {}}
key = "$PLUGIN_NAME@$MARKETPLACE"
entry = {
    "scope": "user",
    "installPath": "$install_path",
    "version": "$version",
    "installedAt": "$now",
    "lastUpdated": "$now"
}
# 기존 엔트리 대체
data.setdefault("plugins", {})[key] = [entry]
registry_path.write_text(json.dumps(data, indent=2, ensure_ascii=False))
print(f"registered: {key} @ $install_path")
PYEOF
}

unregister_plugin() {
  [ -f "$REGISTRY" ] || return 0
  python3 <<PYEOF
import json, pathlib
path = pathlib.Path("$REGISTRY")
data = json.loads(path.read_text())
key = "$PLUGIN_NAME@$MARKETPLACE"
if key in data.get("plugins", {}):
    del data["plugins"][key]
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False))
    print(f"unregistered: {key}")
PYEOF
}

install_to_cache() {
  local version; version="$(read_version "$SOURCE_DIR")"
  local target="$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME/$version"
  ensure_dirs
  [ -e "$target" ] && { log "기존 설치 제거: $target"; rm -rf "$target"; }

  if [ "$MODE" = "--copy" ]; then
    log "복사 설치: $SOURCE_DIR → $target"
    cp -R "$SOURCE_DIR" "$target"
  else
    log "심볼릭 링크: $target → $SOURCE_DIR"
    ln -s "$SOURCE_DIR" "$target"
  fi
  register_plugin "$version" "$target"
  echo "$target"
}

install_git() {
  local url="$1"
  [ -n "$url" ] || err "git URL 필요"
  local tmp; tmp="$(mktemp -d)"
  log "클론: $url → $tmp"
  git clone --depth 1 "$url" "$tmp/repo"

  local src_root
  if [ -d "$tmp/repo/claude_framework/.claude-plugin" ]; then
    src_root="$tmp/repo/claude_framework"
  elif [ -d "$tmp/repo/.claude-plugin" ]; then
    src_root="$tmp/repo"
  else
    err "플러그인 매니페스트를 찾을 수 없음"
  fi

  local version; version="$(read_version "$src_root")"
  local target="$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME/$version"
  ensure_dirs
  [ -e "$target" ] && { log "기존 설치 제거"; rm -rf "$target"; }
  mv "$src_root" "$target"
  rm -rf "$tmp"
  register_plugin "$version" "$target"
  echo "$target"
}

uninstall() {
  local deleted=0
  if [ -d "$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME" ]; then
    rm -rf "$PLUGINS_ROOT/cache/$MARKETPLACE/$PLUGIN_NAME"
    log "캐시 제거"
    deleted=1
  fi
  # 구버전 심볼릭 링크(~/.claude/plugins/claude-framework) 정리
  if [ -L "$PLUGINS_ROOT/$PLUGIN_NAME" ]; then
    rm -f "$PLUGINS_ROOT/$PLUGIN_NAME"
    log "구버전 링크 제거"
    deleted=1
  fi
  unregister_plugin
  [ $deleted -eq 0 ] && log "설치된 흔적 없음"
}

verify() {
  local path="$1"
  log "검증 중..."
  [ -f "$path/.claude-plugin/plugin.json" ] || err "manifest 없음"
  local missing=0
  for rel in \
    agents/pipeline-runner.md \
    agents/be-tech-lead.md \
    skills/kotlin-spring-impl/SKILL.md \
    skills/prd-analysis/SKILL.md \
    commands/init.md \
    commands/parallel-tickets.md \
    common/be-code-convention.md \
    .claude/harness-check.py \
    .claude/resource-resolver.py
  do
    if [ ! -e "$path/$rel" ]; then
      printf "\033[0;33m  누락: %s\033[0m\n" "$rel"
      missing=$((missing + 1))
    fi
  done
  [ $missing -eq 0 ] && log "모든 핵심 리소스 확인" || err "$missing 개 누락"
}

print_next_steps() {
  local path="$1"
  cat <<EOF

============================================
 claude-framework 설치 완료
============================================

경로: $path
등록: $REGISTRY

1. Claude Code 재시작 (또는 새 세션)

2. 플러그인 로드 확인:
   - Claude Code 세션에서 아래 명령이 자동완성에 나타나야 함:
     /init
     /analyze-prd
     /parallel-tickets
   - 에이전트 호출도 가능:
     @pipeline-runner, @be-tech-lead, @prd-analyst

3. 어느 프로젝트에서든:
   \$ cd /path/to/your-project
   /init --scan --classify-repos

4. 제거:
   bash install.sh --uninstall

상세 가이드: $path/PLUGIN.md
EOF
}

case "$MODE" in
  --uninstall)
    uninstall
    log "완료"
    ;;
  --git)
    path="$(install_git "${2:-}")"
    verify "$path"
    print_next_steps "$path"
    ;;
  --copy|"")
    [ "$MODE" = "--copy" ] || MODE="--link"
    path="$(install_to_cache)"
    verify "$path"
    print_next_steps "$path"
    ;;
  -h|--help)
    grep '^#' "$0" | head -15
    ;;
  *)
    err "알 수 없는 옵션: $MODE (--copy|--git <url>|--uninstall)"
    ;;
esac
