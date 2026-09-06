#!/usr/bin/env python3
"""케이스의 3회 반복이 모두 정상(실패율 5% 이하)이면 exit 0 — 재측정 불필요."""
import json
import pathlib
import sys

out, name = pathlib.Path(sys.argv[1]), sys.argv[2]
for rep in (1, 2, 3):
    f = out / f"{name}-rep{rep}.json"
    if not f.exists():
        sys.exit(1)
    try:
        m = json.load(open(f))["metrics"]
    except Exception:
        sys.exit(1)
    if m.get("http_req_failed", {}).get("value", 0) > 0.05:
        sys.exit(1)
sys.exit(0)
