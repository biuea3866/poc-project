#!/usr/bin/env python3
"""k6 summary-export JSON 들을 실험별 비교표로 묶는다.

3회 반복의 중앙값을 대표값으로 쓰고 최소~최대를 함께 적는다.
반복 간 변동폭이 두 비교군의 차이보다 크면 "차이 없음" 으로 읽어야 하기 때문이다.
"""
import json
import statistics
import sys
from pathlib import Path

GROUPS = [
    ("실험 A — 범위 조회 (파티션 키 포함), ORDER BY created_date", [
        ("A-date-plain", "비파티션 PK(id)"),
        ("A-date-composite", "비파티션 PK(id, created_date)"),
        ("A-date-partitioned", "파티션"),
    ]),
    ("실험 A' — 같은 조회, ORDER BY id", [
        ("A-id-plain", "비파티션 PK(id)"),
        ("A-id-composite", "비파티션 PK(id, created_date)"),
        ("A-id-partitioned", "파티션"),
    ]),
    ("실험 A'' — 한 해 전체 집계 (COUNT/SUM)", [
        ("AGG-plain", "비파티션 PK(id)"),
        ("AGG-composite", "비파티션 PK(id, created_date)"),
        ("AGG-partitioned", "파티션"),
    ]),
    ("실험 B — PK 점 조회 (파티션 키 미포함)", [
        ("B-plain", "비파티션 PK(id)"),
        ("B-composite", "비파티션 PK(id, created_date)"),
        ("B-partitioned", "파티션 (7개 파티션 탐색)"),
    ]),
    ("실험 C — JOIN, 댓글 테이블 프루닝 유무 (결과 집합 동일)", [
        ("C-nokey", "댓글 날짜 조건 없음 (7개 파티션)"),
        ("C-withkey", "댓글 날짜 조건 포함 (1개 파티션)"),
    ]),
]


def load(outdir: Path, name: str):
    tps, avg, p95 = [], [], []
    for f in sorted(outdir.glob(f"{name}-rep*.json")):
        d = json.load(open(f))["metrics"]
        tps.append(d["http_reqs"]["rate"])
        avg.append(d["http_req_duration"]["avg"])
        p95.append(d["http_req_duration"]["p(95)"])
    if not tps:
        return None
    fails = []
    for f in sorted(outdir.glob(f"{name}-rep*.json")):
        d = json.load(open(f))["metrics"]
        fails.append(d.get("http_req_failed", {}).get("value", 0))
    return {
        "n": len(tps),
        "tps": statistics.median(tps), "tps_min": min(tps), "tps_max": max(tps),
        "avg": statistics.median(avg), "avg_min": min(avg), "avg_max": max(avg),
        "p95": statistics.median(p95),
        "fail": max(fails),
    }


def main(dataset: str):
    outdir = Path(__file__).parent / "results" / dataset
    print(f"# 측정 결과 — {dataset}\n")
    print("각 값은 3회 반복의 중앙값이고, 괄호는 최소~최대다.\n")
    for title, cases in GROUPS:
        rows = [(label, load(outdir, name)) for name, label in cases]
        if all(r is None for _, r in rows):
            continue
        print(f"## {title}\n")
        print("| 구성 | TPS (req/s) | 평균 레이턴시 | P95 | 실패율 |")
        print("| --- | --- | --- | --- | --- |")
        for label, r in rows:
            if r is None:
                print(f"| {label} | 측정 없음 | | | |")
                continue
            print(f"| {label} | {r['tps']:.1f} ({r['tps_min']:.1f}~{r['tps_max']:.1f}) "
                  f"| {r['avg']:.1f}ms ({r['avg_min']:.1f}~{r['avg_max']:.1f}) "
                  f"| {r['p95']:.1f}ms | {r['fail']*100:.2f}% |")
        base = rows[0][1]
        if base:
            for label, r in rows[1:]:
                if r:
                    delta = (r["tps"] / base["tps"] - 1) * 100
                    print(f"\n- {label}: 첫 행 대비 TPS {delta:+.1f}%")
        print()


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "uniform")
