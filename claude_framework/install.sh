#!/usr/bin/env bash
# claude-framework 플러그인 로컬 설치 스크립트.
#
# 2-단계 설치:
#   1) 이 스크립트: 플러그인 파일 배치만 수행 (Claude Code 내부 JSON 건드리지 않음)
#   2) Claude Code 내장 커맨드: /plugin marketplace add <path>
#      → known_marketplaces.json과 installed_plugins.json을 정식 스키마로 등록
#
# 사용법:
#   bash install.sh             # 기본 설치 (준비)
#   bash install.sh --uninstall # 정리
set -euo pipefail

PLUGIN_NAME="claude-framework"
SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_ROOT="$HOME/.claude/plugins"
MODE="${1:-}"

log() { printf "\033[0;36m[claude-framework]\033[0m %s\n" "$*" >&2; }
err() { printf "\033[0;31m[ERROR]\033[0m %s\n" "$*" >&2; exit 1; }

verify_manifests() {
  [ -f "$SOURCE_DIR/.claude-plugin/plugin.json" ] || err "plugin.json 없음"
  [ -f "$SOURCE_DIR/.claude-plugin/marketplace.json" ] || err "marketplace.json 없음"
  [ -d "$SOURCE_DIR/agents" ] || err "agents/ 없음"
  [ -d "$SOURCE_DIR/skills" ] || err "skills/ 없음"
  [ -d "$SOURCE_DIR/commands" ] || err "commands/ 없음"
  log "매니페스트/리소스 검증 통과"
}

cleanup_stale_entries() {
  # 이전 install.sh 버전이 만들었을 수 있는 손상된 entry 정리
  python3 >&2 <<PYEOF
import json, pathlib
for fname, stale_keys in [
    ("known_marketplaces.json", ["local", "claude-framework-local", "$PLUGIN_NAME"]),
    ("installed_plugins.json", ["$PLUGIN_NAME@local", "$PLUGIN_NAME@claude-framework-local", "$PLUGIN_NAME@$PLUGIN_NAME"])
]:
    p = pathlib.Path("$PLUGINS_ROOT") / fname
    if not p.exists(): continue
    data = json.loads(p.read_text())
    target = data.get("plugins", data) if fname == "installed_plugins.json" else data
    changed = False
    for key in stale_keys:
        if key in target:
            del target[key]
            changed = True
            print(f"  removed stale: {fname} / {key}")
    if changed:
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False))
PYEOF

  # 이전 install.sh 가 만든 심볼릭 링크/디렉토리 정리
  for stale in \
    "$PLUGINS_ROOT/marketplaces/claude-framework-local" \
    "$PLUGINS_ROOT/marketplaces/local" \
    "$PLUGINS_ROOT/cache/claude-framework-local" \
    "$PLUGINS_ROOT/cache/local/claude-framework" \
    "$PLUGINS_ROOT/$PLUGIN_NAME"
  do
    if [ -e "$stale" ] || [ -L "$stale" ]; then
      rm -rf "$stale"
      log "정리: $stale"
    fi
  done
}

uninstall() {
  log "claude-framework 설치 흔적 정리 중..."
  cleanup_stale_entries
  log "Claude Code 내 마켓플레이스 제거도 필요하다면:"
  log "  /plugin marketplace remove claude-framework-local"
  log "완료"
}

prepare() {
  verify_manifests
  cleanup_stale_entries
}

print_next_steps() {
  cat >&2 <<EOF

============================================
 claude-framework 준비 완료 (Step 1/2)
============================================

이 스크립트는 파일 배치까지만 처리했습니다. Claude Code 내부 레지스트리는
공식 커맨드로 등록해야 스키마 검증을 통과합니다.

## Step 2: Claude Code에서 실행 (사용자가 직접)

  1. VSCode Cmd+Q로 완전 종료 후 재실행
  2. 새 Claude 세션 열기
  3. 아래 커맨드 실행:

     /plugin marketplace add $SOURCE_DIR

     → known_marketplaces.json에 정식 등록됨
     → Claude Code가 marketplace.json을 읽어 카탈로그 노출

  4. 플러그인 설치:

     /plugin install claude-framework@claude-framework-local

     또는

     /plugin install claude-framework

  5. 확인:

     /plugin list
     → "claude-framework @ claude-framework-local" 표시
     /init                # 슬래시 커맨드 자동완성
     @be-tech-lead        # 에이전트 호출

## 제거

  /plugin uninstall claude-framework
  /plugin marketplace remove claude-framework-local
  bash install.sh --uninstall   # 로컬 파일 정리

상세: $SOURCE_DIR/PLUGIN.md
EOF
}

case "$MODE" in
  --uninstall)
    uninstall
    ;;
  "")
    prepare
    print_next_steps
    ;;
  -h|--help)
    grep '^#' "$0" | head -12
    ;;
  *)
    err "알 수 없는 옵션: $MODE (--uninstall)"
    ;;
esac
