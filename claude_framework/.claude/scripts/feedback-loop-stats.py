#!/usr/bin/env python3
"""
feedback-loop-guardian 보조 스크립트 — 통계·상한 검증·Stale 감지.

용도:
  - 일일 발화 카운트
  - Stale proposed 제안 감지 (frontmatter status:proposed && age >= N)
  - 머지된 refactor/feedback/* PR 의 효과 측정 (재발 빈도)
  - 자동 .md 수정 시도 감지 (안전 게이트)
  - 비용 상한 체크

사용법:
  python3 feedback-loop-stats.py daily-count [date]
  python3 feedback-loop-stats.py stale-proposals [--days 7]
  python3 feedback-loop-stats.py auto-edit-detect [--since 24.hours]
  python3 feedback-loop-stats.py budget-check [--max-tokens N] [--max-runs N]

종료 코드:
  0 = 정상
  2 = 임계 초과 (가디언이 high 경고로 처리)
"""

from __future__ import annotations

import argparse
import datetime
import re
import subprocess
import sys
from pathlib import Path


PROPOSALS_DIR = Path("docs/feedback-loop/proposals")
DAILY_LIMIT = 5
DEFAULT_STALE_DAYS = 7


def daily_count(date: str | None = None) -> int:
    target = date or datetime.date.today().strftime("%Y%m%d")
    if not PROPOSALS_DIR.exists():
        return 0
    files = list(PROPOSALS_DIR.glob(f"{target}-*.md"))
    return len(files)


def parse_frontmatter(path: Path) -> dict:
    try:
        content = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return {}
    m = re.match(r"^---\s*\n(.*?)\n---", content, re.DOTALL)
    if not m:
        return {}
    fm = {}
    for line in m.group(1).splitlines():
        if ":" in line:
            key, _, val = line.partition(":")
            fm[key.strip()] = val.strip()
    return fm


def stale_proposals(days: int = DEFAULT_STALE_DAYS) -> list[dict]:
    if not PROPOSALS_DIR.exists():
        return []
    cutoff = datetime.date.today() - datetime.timedelta(days=days)
    stale = []
    for path in PROPOSALS_DIR.glob("*.md"):
        if path.name == "README.md":
            continue
        fm = parse_frontmatter(path)
        if fm.get("status") != "proposed":
            continue
        m = re.match(r"^(\d{8})-", path.stem)
        if not m:
            continue
        try:
            file_date = datetime.datetime.strptime(m.group(1), "%Y%m%d").date()
        except ValueError:
            continue
        if file_date <= cutoff:
            stale.append(
                {
                    "file": str(path),
                    "trigger": fm.get("trigger", "?"),
                    "age_days": (datetime.date.today() - file_date).days,
                    "risk": fm.get("risk", "?"),
                }
            )
    return stale


def git(args: list, cwd: Path = Path(".")) -> str:
    try:
        r = subprocess.run(
            ["git"] + args,
            capture_output=True,
            text=True,
            cwd=str(cwd),
            timeout=10,
            check=False,
        )
        return r.stdout if r.returncode == 0 else ""
    except (subprocess.SubprocessError, FileNotFoundError):
        return ""


AUTO_BOT_AUTHORS = {"claude-bot", "process-reviewer", "feedback-loop-guardian"}


def auto_edit_detect(since: str = "24.hours.ago") -> list[dict]:
    """최근 N시간 내에 prompt 자산이 수정됐는데 author 가 자동화 봇이면 high 경고."""
    paths = ["agents", "skills", "commands", ".claude/harness-rules.json"]
    log = git(
        ["log", f"--since={since}", "--pretty=format:%H|%an|%s", "--diff-filter=M", "--"] + paths
    )
    flagged = []
    for line in log.splitlines():
        parts = line.split("|", 2)
        if len(parts) != 3:
            continue
        sha, author, subject = parts
        if author.lower() in {a.lower() for a in AUTO_BOT_AUTHORS}:
            flagged.append({"sha": sha, "author": author, "subject": subject})
    return flagged


def budget_check(max_runs_today: int = 10) -> dict:
    """오늘 process-reviewer 발화 횟수 + 일일 상한 비교."""
    today = daily_count()
    return {
        "today_runs": today,
        "daily_limit": DAILY_LIMIT,
        "max_runs_today": max_runs_today,
        "exceeded_limit": today >= DAILY_LIMIT,
        "near_max": today >= max_runs_today,
    }


def cmd_daily_count(args) -> int:
    n = daily_count(args.date)
    print(f"{n}")
    if n > DAILY_LIMIT:
        sys.stderr.write(
            f"[feedback-loop-stats] 일일 상한({DAILY_LIMIT}) 초과: {n} 건\n"
        )
        return 2
    return 0


def cmd_stale(args) -> int:
    stale = stale_proposals(args.days)
    if not stale:
        print("Stale proposed 없음")
        return 0
    print(f"Stale proposed {len(stale)}건:")
    for s in stale:
        print(f"  - {s['file']}  trigger={s['trigger']}  age={s['age_days']}d  risk={s['risk']}")
    return 2 if any(s["age_days"] >= args.days * 2 for s in stale) else 0


def cmd_auto_edit(args) -> int:
    flagged = auto_edit_detect(args.since)
    if not flagged:
        print("자동 .md 수정 시도 없음 (정상)")
        return 0
    print(f"⚠️ 자동 .md 수정 {len(flagged)}건 감지 — 안티패턴 위반:")
    for f in flagged:
        print(f"  - {f['sha'][:8]}  {f['author']}  {f['subject']}")
    return 2


def cmd_budget(args) -> int:
    info = budget_check(args.max_runs)
    print(
        f"오늘 발화: {info['today_runs']}/{info['daily_limit']}  "
        f"상한 초과: {info['exceeded_limit']}  near_max: {info['near_max']}"
    )
    return 2 if info["exceeded_limit"] else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_daily = sub.add_parser("daily-count")
    p_daily.add_argument("--date", default=None)

    p_stale = sub.add_parser("stale-proposals")
    p_stale.add_argument("--days", type=int, default=DEFAULT_STALE_DAYS)

    p_auto = sub.add_parser("auto-edit-detect")
    p_auto.add_argument("--since", default="24.hours.ago")

    p_budget = sub.add_parser("budget-check")
    p_budget.add_argument("--max-runs", type=int, default=10)

    args = parser.parse_args()

    if args.cmd == "daily-count":
        return cmd_daily_count(args)
    if args.cmd == "stale-proposals":
        return cmd_stale(args)
    if args.cmd == "auto-edit-detect":
        return cmd_auto_edit(args)
    if args.cmd == "budget-check":
        return cmd_budget(args)
    return 2


if __name__ == "__main__":
    sys.exit(main())
