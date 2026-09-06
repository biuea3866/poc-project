#!/usr/bin/env python3
"""k6 요약을 한 줄로 출력하고, 실패율이 5% 를 넘으면 exit 1 로 재측정을 알린다."""
import json
import sys

m = json.load(open(sys.argv[1]))["metrics"]
fail = m.get("http_req_failed", {}).get("value", 0)
print(f"  TPS={m['http_reqs']['rate']:.1f}  avg={m['http_req_duration']['avg']:.0f}ms  fail={fail*100:.1f}%")
sys.exit(1 if fail > 0.05 else 0)
