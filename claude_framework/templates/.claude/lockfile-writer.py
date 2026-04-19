#!/usr/bin/env python3
"""
claude-framework.lock.json 생성/갱신.

/init, /plugin update 후 프로젝트 로컬에 버전/오버라이드 스냅샷 기록.

사용:
  python3 lockfile-writer.py update   # 현재 상태 스캔 → lock 갱신
  python3 lockfile-writer.py show     # 현재 lock 상태 출력
"""

import datetime
import json
import os
import sys
from pathlib import Path


FRAMEWORK_ROOT = Path(__file__).parent.parent.resolve()
PLUGIN_ROOT = Path(os.path.expanduser("~/.claude/plugins/claude-framework"))
PLUGIN_MANIFEST = PLUGIN_ROOT / ".claude-plugin" / "plugin.json"
DEV_MANIFEST = FRAMEWORK_ROOT / ".claude-plugin" / "plugin.json"


def _plugin_version() -> str:
    manifest = PLUGIN_MANIFEST if PLUGIN_MANIFEST.exists() else DEV_MANIFEST
    if not manifest.exists():
        return "unknown"
    try:
        with open(manifest, "r", encoding="utf-8") as f:
            return json.load(f).get("version", "unknown")
    except Exception:
        return "unknown"


def _collect_overrides(cwd: Path) -> dict:
    claude = cwd / ".claude"
    if not claude.exists():
        return {
            "overridden_agents": [],
            "overridden_skills": [],
            "overridden_commands": [],
        }
    agents = sorted(
        p.stem for p in (claude / "agents").glob("*.md")
        if p.name != "README.md"
    ) if (claude / "agents").exists() else []
    skills = sorted(
        p.parent.name for p in (claude / "skills").glob("*/SKILL.md")
    ) if (claude / "skills").exists() else []
    commands = sorted(
        p.stem for p in (claude / "commands").glob("*.md")
        if p.name != "README.md"
    ) if (claude / "commands").exists() else []
    return {
        "overridden_agents": agents,
        "overridden_skills": skills,
        "overridden_commands": commands,
    }


def _count_custom_rules(cwd: Path) -> dict:
    project_rules_path = cwd / ".claude" / "harness-rules.json"
    local_rules_path = cwd / ".claude" / "harness-rules.local.json"
    result = {"project_rules": 0, "local_rules": 0, "disabled_plugin_rules": []}
    for label, path in (("project_rules", project_rules_path), ("local_rules", local_rules_path)):
        if not path.exists():
            continue
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
            rules = (data.get("forbidden_patterns") or {}).get("rules") or []
            layer_rules = (data.get("layer_dependency") or {}).get("rules") or []
            result[label] = len(rules) + len(layer_rules)
            result["disabled_plugin_rules"].extend(data.get("_rule_disabled", []) or [])
        except Exception:
            continue
    result["disabled_plugin_rules"] = sorted(set(result["disabled_plugin_rules"]))
    return result


def write_lock() -> int:
    cwd = Path.cwd().resolve()
    lock_path = cwd / ".claude" / "claude-framework.lock.json"
    if not (cwd / ".claude").exists():
        sys.stderr.write("ERROR: .claude/ 디렉토리가 없습니다. /init 먼저 실행하세요.\n")
        return 1

    overrides = _collect_overrides(cwd)
    rules_stat = _count_custom_rules(cwd)

    lock = {
        "plugin_version": _plugin_version(),
        "written_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "project_path": str(cwd),
        "overridden_agents": overrides["overridden_agents"],
        "overridden_skills": overrides["overridden_skills"],
        "overridden_commands": overrides["overridden_commands"],
        "custom_rules_count": {
            "project": rules_stat["project_rules"],
            "local": rules_stat["local_rules"],
        },
        "disabled_plugin_rules": rules_stat["disabled_plugin_rules"],
    }

    with open(lock_path, "w", encoding="utf-8") as f:
        json.dump(lock, f, indent=2, ensure_ascii=False)
        f.write("\n")
    sys.stdout.write(f"lockfile written: {lock_path}\n")
    return 0


def show_lock() -> int:
    cwd = Path.cwd().resolve()
    lock_path = cwd / ".claude" / "claude-framework.lock.json"
    if not lock_path.exists():
        sys.stderr.write("lockfile이 없습니다. 'update' 커맨드로 생성하세요.\n")
        return 1
    with open(lock_path, "r", encoding="utf-8") as f:
        sys.stdout.write(f.read())
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        sys.stderr.write(__doc__)
        return 1
    cmd = sys.argv[1]
    if cmd == "update":
        return write_lock()
    if cmd == "show":
        return show_lock()
    sys.stderr.write(f"unknown command: {cmd}\n{__doc__}")
    return 1


if __name__ == "__main__":
    sys.exit(main())