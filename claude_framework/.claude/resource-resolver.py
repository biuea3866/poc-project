#!/usr/bin/env python3
"""
claude_framework 리소스 리졸버.

에이전트/스킬의 `extends: claude-framework:<name>` frontmatter를 만나면,
플러그인 원본을 찾아 프로젝트 오버라이드와 병합해 최종 프롬프트를 생성한다.

사용:
  python3 resource-resolver.py agent <name>
  python3 resource-resolver.py skill <name>
  python3 resource-resolver.py list              # 모든 리소스 나열 (plugin + project)
  python3 resource-resolver.py diff agent <name> # 오버라이드 vs 원본 diff

우선순위:
  1. $CWD/.claude/<type>s/<name>.md          (프로젝트 로컬, extends 지원)
  2. ~/.claude/plugins/claude-framework/.claude/<type>s/<name>.md  (플러그인)
  3. FRAMEWORK_ROOT/.claude/<type>s/<name>.md  (개발 모드 fallback)
"""

import difflib
import json
import os
import re
import sys
from pathlib import Path


FRAMEWORK_ROOT = Path(__file__).parent.parent.resolve()
PLUGIN_ROOT = Path(os.path.expanduser("~/.claude/plugins/claude-framework"))


def _resource_path(kind: str, name: str, base: Path) -> Path:
    if kind == "skill":
        return base / ".claude" / "skills" / name / "SKILL.md"
    return base / ".claude" / f"{kind}s" / f"{name}.md"


def _resolve_paths(kind: str, name: str) -> dict:
    cwd = Path.cwd().resolve()
    return {
        "project": _resource_path(kind, name, cwd),
        "plugin": _resource_path(kind, name, PLUGIN_ROOT)
            if PLUGIN_ROOT.exists()
            else _resource_path(kind, name, FRAMEWORK_ROOT),
        "dev": _resource_path(kind, name, FRAMEWORK_ROOT),
    }


_FRONTMATTER_RE = re.compile(r"^---\s*\n(.*?)\n---\s*\n(.*)$", re.DOTALL)


def parse_frontmatter(text: str) -> tuple[dict, str]:
    """YAML 프론트매터(단순 키-값) + 본문 분리."""
    match = _FRONTMATTER_RE.match(text)
    if not match:
        return {}, text
    raw_front, body = match.group(1), match.group(2)
    front: dict = {}
    for line in raw_front.splitlines():
        if not line.strip() or line.strip().startswith("#"):
            continue
        if ":" not in line:
            continue
        key, _, value = line.partition(":")
        front[key.strip()] = value.strip().strip('"').strip("'")
    return front, body


def serialize_frontmatter(front: dict, body: str) -> str:
    lines = ["---"]
    for key, value in front.items():
        lines.append(f"{key}: {value}")
    lines.append("---\n")
    return "\n".join(lines) + body


def read_resource(path: Path) -> tuple[dict, str] | None:
    if not path.exists():
        return None
    with open(path, "r", encoding="utf-8") as f:
        return parse_frontmatter(f.read())


def resolve(kind: str, name: str) -> tuple[dict, str]:
    """
    리소스 로드 + extends 병합.
    반환: (frontmatter, merged_body)
    """
    paths = _resolve_paths(kind, name)

    # 1. 프로젝트 로컬 우선
    project = read_resource(paths["project"])
    if project:
        front, body = project
        extends = front.get("extends", "")
        if extends.startswith("claude-framework:"):
            parent_name = extends.split(":", 1)[1]
            parent = read_resource(paths["plugin"]) or read_resource(paths["dev"])
            if parent:
                parent_front, parent_body = parent
                merged_body = f"{parent_body}\n\n---\n## 프로젝트 확장 ({name})\n\n{body}"
                merged_front = {**parent_front, **{k: v for k, v in front.items() if k != "extends"}}
                return merged_front, merged_body
        return front, body

    # 2. 플러그인 fallback
    plugin = read_resource(paths["plugin"]) or read_resource(paths["dev"])
    if plugin:
        return plugin

    raise FileNotFoundError(f"{kind}:{name} not found in project/plugin/dev")


def cmd_get(kind: str, name: str) -> int:
    front, body = resolve(kind, name)
    sys.stdout.write(serialize_frontmatter(front, body))
    return 0


def cmd_list() -> int:
    cwd = Path.cwd().resolve()
    roots = [
        ("project", cwd),
        ("plugin", PLUGIN_ROOT if PLUGIN_ROOT.exists() else FRAMEWORK_ROOT),
    ]
    for label, root in roots:
        for kind in ("agent", "command"):
            base_dir = root / ".claude" / f"{kind}s"
            if not base_dir.exists():
                continue
            for md in sorted(base_dir.glob("*.md")):
                if md.name == "README.md":
                    continue
                sys.stdout.write(f"[{label}] {kind}: {md.stem}\n")
        skills_dir = root / ".claude" / "skills"
        if skills_dir.exists():
            for skill_md in sorted(skills_dir.glob("*/SKILL.md")):
                sys.stdout.write(f"[{label}] skill: {skill_md.parent.name}\n")
    return 0


def cmd_diff(kind: str, name: str) -> int:
    paths = _resolve_paths(kind, name)
    project = read_resource(paths["project"])
    plugin = read_resource(paths["plugin"]) or read_resource(paths["dev"])

    if not project:
        sys.stdout.write(f"{kind}:{name}은 프로젝트에서 오버라이드되지 않았습니다.\n")
        return 0
    if not plugin:
        sys.stdout.write(f"{kind}:{name}은 플러그인에 없습니다 (프로젝트 전용).\n")
        return 0

    plugin_text = serialize_frontmatter(*plugin)
    project_text = serialize_frontmatter(*project)
    diff = difflib.unified_diff(
        plugin_text.splitlines(keepends=True),
        project_text.splitlines(keepends=True),
        fromfile=f"plugin/{kind}:{name}",
        tofile=f"project/{kind}:{name}",
    )
    sys.stdout.writelines(diff)
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        sys.stderr.write(__doc__)
        return 1
    cmd = sys.argv[1]
    if cmd == "list":
        return cmd_list()
    if cmd == "diff" and len(sys.argv) >= 4:
        return cmd_diff(sys.argv[2], sys.argv[3])
    if cmd in ("agent", "skill", "command") and len(sys.argv) >= 3:
        return cmd_get(cmd, sys.argv[2])
    sys.stderr.write(f"unknown command: {cmd}\n{__doc__}")
    return 1


if __name__ == "__main__":
    sys.exit(main())