#!/usr/bin/env python3
"""
Closet 로드맵 오케스트레이터.

전체 티켓 큐를 관리하고, 각 티켓에 대해 워크플로우 파이프라인을 자동 실행한다.
의존성이 없는 티켓들은 병렬로 실행 가능.

사용법:
  python3 harness-orchestrator.py add <id> <title> [--priority N]  — 티켓 추가
  python3 harness-orchestrator.py add-bulk                          — stdin JSON으로 일괄 추가
  python3 harness-orchestrator.py queue                             — 전체 큐 조회
  python3 harness-orchestrator.py next                              — 다음 티켓 1개 시작 (순차)
  python3 harness-orchestrator.py next-batch                        — 병렬 가능한 티켓 일괄 시작
  python3 harness-orchestrator.py current                           — 현재 작업 중인 티켓
  python3 harness-orchestrator.py done [ticket_id]                  — 티켓 완료 (병렬 시 ID 필수)
  python3 harness-orchestrator.py skip [ticket_id] [reason]         — 티켓 스킵
  python3 harness-orchestrator.py worker-prompt <ticket_id>         — 병렬 워커 프롬프트 생성
  python3 harness-orchestrator.py progress                          — 전체 진행률
  python3 harness-orchestrator.py agent-prompt                      — 에이전트용 전체 실행 프롬프트
  python3 harness-orchestrator.py reset                             — 초기화

오케스트레이션 플로우:
  순차: next → (pipeline) → done → next → ...
  병렬: next-batch → worker agents (worktree) → done <id> → next-batch → ...
"""

import json
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
QUEUE_FILE = SCRIPT_DIR / "orchestrator-queue.json"
WORKFLOW_SCRIPT = SCRIPT_DIR / "harness-workflow.py"


def load_queue():
    if QUEUE_FILE.exists():
        with open(QUEUE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return default_queue()


def save_queue(q):
    q["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(QUEUE_FILE, "w", encoding="utf-8") as f:
        json.dump(q, f, indent=2, ensure_ascii=False)


def default_queue():
    return {
        "tickets": [],
        "current_index": -1,
        "completed": [],
        "skipped": [],
        "started_at": None,
        "updated_at": None,
    }


def set_workflow_ticket(ticket_id, title, url=None):
    """워크플로우를 reset → set-ticket 실행."""
    subprocess.run(
        ["python3", str(WORKFLOW_SCRIPT), "reset"],
        cwd=PROJECT_ROOT, capture_output=True,
    )
    args = ["python3", str(WORKFLOW_SCRIPT), "set-ticket", ticket_id, title]
    if url:
        args.append(url)
    subprocess.run(args, cwd=PROJECT_ROOT, capture_output=True)


def cmd_add(args):
    """티켓 추가."""
    if len(args) < 2:
        print("Usage: add <id> <title> [--priority N] [--url URL] [--deps id1,id2]", file=sys.stderr)
        sys.exit(1)

    ticket_id = args[0]
    title = args[1]
    priority = 0
    url = None
    deps = []

    i = 2
    while i < len(args):
        if args[i] == "--priority" and i + 1 < len(args):
            priority = int(args[i + 1])
            i += 2
        elif args[i] == "--url" and i + 1 < len(args):
            url = args[i + 1]
            i += 2
        elif args[i] == "--deps" and i + 1 < len(args):
            deps = args[i + 1].split(",")
            i += 2
        else:
            i += 1

    q = load_queue()

    # 중복 체크
    for t in q["tickets"]:
        if t["id"] == ticket_id:
            print(f"이미 존재하는 티켓: {ticket_id}", file=sys.stderr)
            sys.exit(1)

    q["tickets"].append({
        "id": ticket_id,
        "title": title,
        "url": url,
        "priority": priority,
        "deps": deps,
        "status": "pending",
        "added_at": datetime.now(timezone.utc).isoformat(),
    })

    if not q["started_at"]:
        q["started_at"] = datetime.now(timezone.utc).isoformat()

    save_queue(q)
    print(f"티켓 추가: {ticket_id} — {title} (priority: {priority})")


def cmd_add_bulk(args):
    """stdin JSON으로 일괄 추가. Notion 검색 결과를 파이프할 수 있음."""
    try:
        raw = sys.stdin.read()
        data = json.loads(raw)
    except (json.JSONDecodeError, KeyError):
        print("JSON 파싱 실패. [{\"id\":\"...\",\"title\":\"...\"},...] 형식으로 입력하세요.", file=sys.stderr)
        sys.exit(1)

    tickets = data if isinstance(data, list) else data.get("tickets", [])
    q = load_queue()

    existing_ids = {t["id"] for t in q["tickets"]}
    added = 0

    for item in tickets:
        tid = item.get("id") or item.get("ticket_id")
        title = item.get("title") or item.get("name", "Untitled")
        if not tid or tid in existing_ids:
            continue

        q["tickets"].append({
            "id": tid,
            "title": title,
            "url": item.get("url"),
            "priority": item.get("priority", 0),
            "deps": item.get("deps", []),
            "status": "pending",
            "added_at": datetime.now(timezone.utc).isoformat(),
        })
        existing_ids.add(tid)
        added += 1

    if not q["started_at"]:
        q["started_at"] = datetime.now(timezone.utc).isoformat()

    save_queue(q)
    print(f"{added}개 티켓 추가 완료 (총 {len(q['tickets'])}개)")


def cmd_queue(args):
    """전체 큐 조회."""
    q = load_queue()
    tickets = q["tickets"]

    if not tickets:
        print("큐가 비어있습니다. `add` 또는 `add-bulk` 으로 티켓을 추가하세요.")
        return

    current_idx = q["current_index"]

    print(f"로드맵 큐 ({len(tickets)}개 티켓)")
    print(f"{'─'*60}")

    for i, t in enumerate(tickets):
        if t["status"] == "done":
            marker = "✅"
        elif t["status"] == "skipped":
            marker = "⏭️"
        elif i == current_idx:
            marker = "▶️"
        else:
            marker = "⏳"

        deps = f" (deps: {','.join(t['deps'])})" if t.get("deps") else ""
        print(f"  {marker} {i+1}. [{t['id']}] {t['title']}{deps}")


def _get_available_tickets(q):
    """의존성이 충족된 pending 티켓 목록 반환."""
    completed_ids = set(q["completed"])
    in_progress_ids = {t["id"] for t in q["tickets"] if t["status"] == "in_progress"}
    available = []

    for i, t in enumerate(q["tickets"]):
        if t["status"] != "pending":
            continue
        deps_met = all(d in completed_ids for d in t.get("deps", []))
        if deps_met:
            available.append((i, t))

    return available


def cmd_next(args):
    """다음 pending 티켓 1개 시작 (순차 모드)."""
    q = load_queue()
    available = _get_available_tickets(q)

    if not available:
        remaining = sum(1 for t in q["tickets"] if t["status"] == "pending")
        if remaining > 0:
            print(f"의존성 미충족 pending 티켓 {remaining}개.")
        else:
            print("모든 티켓 완료!")
        return

    next_idx, next_ticket = available[0]
    next_ticket["status"] = "in_progress"
    q["current_index"] = next_idx
    save_queue(q)

    set_workflow_ticket(next_ticket["id"], next_ticket["title"], next_ticket.get("url"))

    total = len(q["tickets"])
    done = sum(1 for t in q["tickets"] if t["status"] == "done")

    print(f"[오케스트레이터] 다음 티켓 시작 ({done+1}/{total})")
    print(f"  ID: {next_ticket['id']}")
    print(f"  제목: {next_ticket['title']}")
    print(f"  워크플로우: idle → ticket (자동 전환 완료)")
    print(f"  → 테스트 케이스부터 작성하세요.")


def cmd_next_batch(args):
    """병렬 실행 가능한 티켓 일괄 시작 + 워커 프롬프트 생성."""
    q = load_queue()
    available = _get_available_tickets(q)

    if not available:
        remaining = sum(1 for t in q["tickets"] if t["status"] == "pending")
        if remaining > 0:
            print(f"의존성 미충족 pending 티켓 {remaining}개.")
        else:
            print("모든 티켓 완료!")
        return

    # 모두 in_progress로 전환
    for idx, ticket in available:
        ticket["status"] = "in_progress"
        ticket["worker"] = f"worker-{ticket['id'].lower()}"
    save_queue(q)

    total = len(q["tickets"])
    done = sum(1 for t in q["tickets"] if t["status"] == "done")

    print(f"[오케스트레이터] {len(available)}개 티켓 병렬 시작 ({done}/{total} 완료)")
    print(f"{'─'*60}")

    for _, ticket in available:
        role = _detect_role(ticket)
        print(f"  🔧 [{ticket['id']}] {ticket['title']}")
        print(f"     역할: {role['name']} | 워커: {ticket['worker']}")

    print(f"\n{'─'*60}")
    print(f"각 티켓의 워커 에이전트 프롬프트:")
    print(f"  python3 .claude/harness-orchestrator.py worker-prompt <ticket_id>")
    print(f"\n에이전트 스폰 방법:")
    print(f"  Agent tool로 각 티켓을 worktree 격리된 에이전트에 할당")
    print(f"  완료 시: python3 .claude/harness-orchestrator.py done <ticket_id>")


def _detect_role(ticket):
    """티켓 제목/ID에서 역할을 자동 감지한다."""
    title = (ticket.get("title") or "").lower()
    tid = (ticket.get("id") or "").lower()
    combined = f"{tid} {title}"

    role_patterns = {
        "be": {
            "name": "BE 엔지니어",
            "keywords": ["api", "domain", "service", "entity", "repository", "kafka", "consumer",
                         "outbox", "querydsl", "migration", "flyway", "jpa", "batch", "scheduler",
                         "facade", "controller", "backend", "be-", "인증", "인가", "재고", "주문",
                         "결제", "배송", "리뷰", "검색", "회원"],
            "pipeline": "implementation",
            "agent_ref": "closet-ecommerce/.analysis/implementation/PIPELINE.md",
        },
        "fe": {
            "name": "FE 엔지니어",
            "keywords": ["page", "component", "ui", "ux", "화면", "프론트", "next.js", "react",
                         "tailwind", "shadcn", "fe-", "frontend", "웹", "모바일", "디자인 구현"],
            "pipeline": "implementation",
            "agent_ref": "closet-ecommerce/.analysis/implementation/PIPELINE.md",
        },
        "devops": {
            "name": "DevOps 엔지니어",
            "keywords": ["docker", "k8s", "kubernetes", "helm", "ci/cd", "github actions",
                         "monitoring", "prometheus", "grafana", "infra", "인프라", "배포",
                         "devops", "terraform", "pipeline", "alertmanager"],
            "pipeline": "implementation",
            "agent_ref": "closet-ecommerce/.analysis/implementation/PIPELINE.md",
        },
        "qa": {
            "name": "QA 엔지니어",
            "keywords": ["test", "테스트", "qa", "e2e", "부하", "성능", "검증", "tc-",
                         "test case", "테스트 케이스", "보안 테스트"],
            "pipeline": "verification",
            "agent_ref": "closet-ecommerce/.analysis/verification/PIPELINE.md",
        },
        "pm": {
            "name": "PM/PO",
            "keywords": ["prd", "기획", "요구사항", "kpi", "a/b", "로드맵", "릴리즈",
                         "pm-", "기획서", "유저 스토리", "프로모션 기획"],
            "pipeline": "prd",
            "agent_ref": "closet-ecommerce/.analysis/prd/PIPELINE.md",
        },
        "design": {
            "name": "디자이너",
            "keywords": ["figma", "디자인", "와이어프레임", "ui 설계", "디자인 시스템",
                         "프로토타입", "목업", "스타일 가이드"],
            "pipeline": "prd",
            "agent_ref": "closet-ecommerce/.analysis/prd/PIPELINE.md",
        },
    }

    # 우선순위 순서: 먼저 매칭된 역할이 우선 (be는 마지막)
    priority_order = ["pm", "design", "qa", "devops", "fe", "be"]
    best_role = None
    best_score = 0

    for role_id in priority_order:
        role = role_patterns[role_id]
        score = sum(1 for kw in role["keywords"] if kw in combined)
        if score > best_score:
            best_score = score
            best_role = role

    return best_role or role_patterns["be"]  # 기본값: BE


def cmd_worker_prompt(args):
    """병렬 워커 에이전트에 전달할 프롬프트 생성."""
    if not args:
        print("Usage: worker-prompt <ticket_id>", file=sys.stderr)
        sys.exit(1)

    ticket_id = args[0]
    q = load_queue()
    ticket = None
    for t in q["tickets"]:
        if t["id"] == ticket_id:
            ticket = t
            break

    if not ticket:
        print(f"Ticket {ticket_id} not found", file=sys.stderr)
        sys.exit(1)

    role = _detect_role(ticket)
    pipeline_ref = role["agent_ref"]

    # 역할별 구현 지침 생성
    role_instructions = {
        "BE 엔지니어": """## BE 구현 절차
1. **테스트 먼저 작성** (Kotest BehaviorSpec, Given/When/Then)
2. **엔티티 + enum** — 비즈니스 로직 캡슐화, 상태 전이는 enum 내부
3. **Repository** — QueryDSL CustomRepository + Impl 패턴 (@Query 금지)
4. **Service** — 얇게. 엔티티 메서드 호출 + Repository 저장만
5. **Facade** — 오케스트레이션만. if/else 없음
6. **Controller** — Facade만 의존
7. **Kafka Consumer** — DTO 직접 매핑, Facade/Service 경유 (Repository 직접 호출 금지)
8. **DDL/Flyway** — FK/ENUM/JSON/BOOLEAN 금지, DATETIME(6), COMMENT 필수

## 코드 컨벤션
- 시간: ZonedDateTime (LocalDateTime 금지)
- 토픽: ClosetTopics 상수 사용 (하드코딩 금지)
- FQCN 금지 → import 사용
- Consumer도 엔드포인트 → Facade/Service 경유""",

        "FE 엔지니어": """## FE 구현 절차
1. Next.js 14 App Router + TypeScript
2. Tailwind CSS + Shadcn/ui 컴포넌트
3. 서버 컴포넌트 우선, 클라이언트는 'use client' 명시
4. API 연동: fetch + error boundary
5. 반응형 디자인 (모바일 우선)""",

        "DevOps 엔지니어": """## DevOps 구현 절차
1. Docker Compose 서비스 추가/수정
2. GitHub Actions 워크플로우
3. Prometheus/Grafana 대시보드
4. 알림 규칙 (Alertmanager)""",

        "QA 엔지니어": """## QA 절차
1. 테스트 케이스 작성 (Given/When/Then)
2. 정상/예외/엣지 케이스 분류
3. API 테스트 + E2E 시나리오
4. 부하 테스트 (k6)""",

        "PM/PO": """## PM 절차
1. PRD 작성 (기능 요구사항, AC, KPI)
2. 유저 스토리 정의
3. 우선순위 결정
4. 릴리즈 계획""",

        "디자이너": """## 디자인 절차
1. 와이어프레임/프로토타입
2. 디자인 시스템 준수
3. 접근성 체크리스트
4. Figma 디자인 파일 작성""",
    }

    instructions = role_instructions.get(role["name"], role_instructions["BE 엔지니어"])

    print(f"""## 워커 에이전트 프롬프트 — {ticket['id']}

**역할**: {role['name']}
**티켓**: [{ticket['id']}] {ticket['title']}
**파이프라인 참조**: {pipeline_ref}

{instructions}

## 작업 절차

1. 파이프라인 가이드를 읽으세요:
   `Read {pipeline_ref}`

2. 티켓의 요구사항을 분석하세요

3. **테스트를 먼저 작성**하세요 (TDD: Red → Green → Refactor)

4. 구현하세요

5. 빌드 + 테스트 검증:
   ```
   JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home \\
   ./gradlew compileKotlin && ./gradlew test
   ```

6. feature 브랜치에서 커밋하세요:
   ```
   git checkout -b feature/{ticket['id'].lower()}
   git add . && git commit
   ```

7. 완료 후 보고하세요.

## 주의사항
- 하네스 규칙을 준수하세요 (harness-rules.json 참조)
- 다른 티켓의 코드를 수정하지 마세요
- 기존 테스트가 깨지지 않도록 주의하세요""")


def cmd_current(args):
    """현재 작업 중인 티켓."""
    q = load_queue()
    idx = q["current_index"]

    if idx < 0 or idx >= len(q["tickets"]):
        print("현재 작업 중인 티켓 없음. `next` 로 시작하세요.")
        return

    t = q["tickets"][idx]
    total = len(q["tickets"])
    done = sum(1 for x in q["tickets"] if x["status"] == "done")

    print(f"현재 티켓 ({done+1}/{total}):")
    print(f"  ID: {t['id']}")
    print(f"  제목: {t['title']}")
    print(f"  상태: {t['status']}")


def cmd_done(args):
    """티켓 완료. 순차 모드: 인자 없이. 병렬 모드: done <ticket_id>."""
    q = load_queue()

    # 병렬 모드: ticket_id 지정
    if args:
        ticket_id = args[0]
        for t in q["tickets"]:
            if t["id"] == ticket_id:
                t["status"] = "done"
                t["completed_at"] = datetime.now(timezone.utc).isoformat()
                t.pop("worker", None)
                if ticket_id not in q["completed"]:
                    q["completed"].append(ticket_id)
                save_queue(q)

                total = len(q["tickets"])
                done_count = sum(1 for x in q["tickets"] if x["status"] == "done")
                in_progress = sum(1 for x in q["tickets"] if x["status"] == "in_progress")
                remaining = sum(1 for x in q["tickets"] if x["status"] == "pending")

                print(f"[오케스트레이터] {ticket_id} 완료! ({done_count}/{total})")

                if in_progress > 0:
                    print(f"  병렬 진행중: {in_progress}개")
                if remaining > 0:
                    # 새로 풀린 티켓이 있는지 확인
                    newly_available = _get_available_tickets(q)
                    if newly_available:
                        print(f"  새로 시작 가능: {len(newly_available)}개")
                        for _, nt in newly_available:
                            print(f"    → [{nt['id']}] {nt['title']}")
                elif in_progress == 0:
                    print(f"  모든 티켓 완료!")
                return

        print(f"Ticket {ticket_id} not found", file=sys.stderr)
        sys.exit(1)

    # 순차 모드: current_index 기반
    idx = q["current_index"]
    if idx < 0 or idx >= len(q["tickets"]):
        print("현재 작업 중인 티켓 없음. 병렬 모드라면: done <ticket_id>")
        return

    t = q["tickets"][idx]
    t["status"] = "done"
    t["completed_at"] = datetime.now(timezone.utc).isoformat()
    q["completed"].append(t["id"])
    q["current_index"] = -1
    save_queue(q)

    total = len(q["tickets"])
    done_count = sum(1 for x in q["tickets"] if x["status"] == "done")
    remaining = total - done_count - sum(1 for x in q["tickets"] if x["status"] == "skipped")

    print(f"[오케스트레이터] {t['id']} 완료! ({done_count}/{total})")

    if remaining > 0:
        print(f"  남은 티켓: {remaining}개 → 자동으로 다음 티켓 시작합니다.")
        print()
        cmd_next([])
    else:
        print(f"  모든 티켓 완료!")


def cmd_skip(args):
    """티켓 스킵. skip [ticket_id] [reason]."""
    q = load_queue()

    # ticket_id 지정된 경우
    if args and any(t["id"] == args[0] for t in q["tickets"]):
        ticket_id = args[0]
        reason = " ".join(args[1:]) if len(args) > 1 else "skipped"
        for t in q["tickets"]:
            if t["id"] == ticket_id:
                t["status"] = "skipped"
                t["skip_reason"] = reason
                t.pop("worker", None)
                q["skipped"].append({"id": ticket_id, "reason": reason})
                save_queue(q)
                print(f"[오케스트레이터] {ticket_id} 스킵 ({reason})")
                return
    else:
        # 순차 모드
        idx = q["current_index"]
        if idx < 0 or idx >= len(q["tickets"]):
            print("현재 작업 중인 티켓 없음.")
            return

        t = q["tickets"][idx]
        reason = " ".join(args) if args else "skipped"
        t["status"] = "skipped"
        t["skip_reason"] = reason
        q["skipped"].append({"id": t["id"], "reason": reason})
        q["current_index"] = -1
        save_queue(q)

        print(f"[오케스트레이터] {t['id']} 스킵 ({reason})")
        print(f"  → 자동으로 다음 티켓 시작합니다.")
        print()
        cmd_next([])


def cmd_progress(args):
    """전체 진행률."""
    q = load_queue()
    tickets = q["tickets"]

    if not tickets:
        print("큐가 비어있습니다.")
        return

    total = len(tickets)
    done = sum(1 for t in tickets if t["status"] == "done")
    skipped = sum(1 for t in tickets if t["status"] == "skipped")
    in_progress = sum(1 for t in tickets if t["status"] == "in_progress")
    pending = sum(1 for t in tickets if t["status"] == "pending")

    pct = done * 100 // total if total > 0 else 0
    bar_len = 30
    filled = bar_len * done // total if total > 0 else 0
    bar = "█" * filled + "░" * (bar_len - filled)

    print(f"로드맵 진행률: [{bar}] {pct}%")
    print(f"  ✅ 완료: {done}")
    print(f"  🔧 진행중: {in_progress}")
    print(f"  ⏳ 대기: {pending}")
    print(f"  ⏭️  스킵: {skipped}")
    print(f"  총: {total}")

    if q.get("started_at"):
        print(f"  시작: {q['started_at'][:10]}")


def cmd_agent_prompt(args):
    """에이전트에게 전달할 로드맵 실행 프롬프트 생성."""
    q = load_queue()
    tickets = q["tickets"]
    total = len(tickets)
    done = sum(1 for t in tickets if t["status"] == "done")
    pending_tickets = [t for t in tickets if t["status"] == "pending"]

    if not pending_tickets:
        print("수행할 티켓이 없습니다.")
        return

    ticket_list = "\n".join(
        f"  {i+1}. [{t['id']}] {t['title']}"
        for i, t in enumerate(pending_tickets)
    )

    print(f"""## 로드맵 자동 수행 지침

총 {total}개 티켓 중 {done}개 완료, {len(pending_tickets)}개 남음.

### 남은 티켓
{ticket_list}

### 각 티켓 수행 절차

아래 루프를 모든 티켓이 완료될 때까지 반복하세요:

1. **다음 티켓 가져오기**
   ```
   python3 .claude/harness-orchestrator.py next
   ```
   → 워크플로우 자동 설정됨 (idle → ticket)

2. **테스트 작성 (Red)**
   - 티켓의 AC/테스트 케이스를 Kotest BehaviorSpec으로 작성
   - *Test.kt 또는 *Spec.kt 파일
   → 워크플로우 자동 전환: ticket → testing

3. **구현 (Green)**
   - 테스트를 통과하는 최소 구현 작성
   - 엔티티 캡슐화, Service 얇게, QueryDSL 사용
   → 워크플로우 자동 전환: testing → implementing

4. **빌드 + 테스트 검증**
   ```
   JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home \\
   ./gradlew compileKotlin && ./gradlew test
   ```

5. **리뷰**
   - 리뷰어 에이전트를 스폰하여 코드 리뷰
   ```
   python3 .claude/harness-workflow.py review-request
   ```
   - 리뷰 통과 시:
   ```
   python3 .claude/harness-workflow.py review-approve
   ```

6. **PR 생성 + 머지**
   - feature 브랜치에서 PR 생성 + 머지
   - main 직접 푸시 금지

7. **티켓 완료 → 다음 자동 시작**
   ```
   python3 .claude/harness-orchestrator.py done
   ```
   → 자동으로 다음 티켓의 워크플로우가 설정됨

### 주의사항
- 하네스 훅이 각 단계를 강제합니다 (테스트 없이 구현 불가, 리뷰 없이 PR 불가)
- 빌드 실패 시 3회까지 재시도 후 skip
- 모든 코드는 프로젝트 컨벤션을 따릅니다 (ZonedDateTime, QueryDSL, 엔티티 캡슐화 등)""")


def cmd_reset(args):
    q = default_queue()
    save_queue(q)
    subprocess.run(
        ["python3", str(WORKFLOW_SCRIPT), "reset"],
        cwd=PROJECT_ROOT, capture_output=True,
    )
    print("[오케스트레이터] 큐 초기화 + 워크플로우 리셋 완료")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    cmd = sys.argv[1]
    args = sys.argv[2:]

    commands = {
        "add": cmd_add,
        "add-bulk": cmd_add_bulk,
        "queue": cmd_queue,
        "next": cmd_next,
        "next-batch": cmd_next_batch,
        "worker-prompt": cmd_worker_prompt,
        "current": cmd_current,
        "done": cmd_done,
        "skip": cmd_skip,
        "progress": cmd_progress,
        "agent-prompt": cmd_agent_prompt,
        "reset": cmd_reset,
    }

    if cmd not in commands:
        print(f"Unknown command: {cmd}", file=sys.stderr)
        print("Available:", ", ".join(commands.keys()))
        sys.exit(1)

    commands[cmd](args)


if __name__ == "__main__":
    main()
