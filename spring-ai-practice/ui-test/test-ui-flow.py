"""
UI 자동 검증 스크립트.
- 로그인 (shopper-llm)
- 채팅창에 "재고 있는 블랙 옷 추천해줘" 입력
- 응답 캡처
- catalog-only-llm 으로 재로그인 → 주문 상태 질의 → 권한 부족 안내 확인
- 매 단계 스크린샷 저장
"""
import asyncio
import os
from pathlib import Path
from playwright.async_api import async_playwright

OUT = Path("spring-ai-practice/docs/screenshots")
OUT.mkdir(parents=True, exist_ok=True)


async def step(page, name, action_text=None):
    print(f"  → {name}")
    if action_text:
        print(f"     {action_text}")
    await page.screenshot(path=str(OUT / f"{name}.png"), full_page=True)


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(viewport={"width": 900, "height": 720})
        page = await context.new_page()

        # 콘솔 로그
        page.on("console", lambda msg: print(f"[console] {msg.type}: {msg.text}"))

        print("▶ 1. UI 로드")
        await page.goto("http://localhost:8080/")
        await page.wait_for_selector("#login-view", state="visible")
        await step(page, "01-login-view")

        print("▶ 2. shopper-llm 로그인")
        # 셀렉트 박스는 기본값 shopper-llm
        await page.fill("#client-secret", "dev-secret-1")
        await page.click("#login-btn")
        await page.wait_for_selector("#chat-view", state="visible", timeout=10000)
        await page.wait_for_timeout(500)
        await step(page, "02-chat-view-shopper")

        print("▶ 3. 채팅: '블랙 옷 추천해줘' (LLM 응답 시간 가변)")
        await page.fill("#message-input", "재고 있는 블랙 옷 추천해줘")
        await page.click("#send-btn")
        # 응답 대기 (Ollama 가 띄워졌으면 응답이 오고, 아니면 502 에러 메시지)
        # 어느 쪽이든 "…" 가 사라지면 진행
        for _ in range(60):  # 최대 60초
            text = await page.text_content("#messages")
            if text and "…" not in text:
                break
            # 첫 메시지가 "…" 인지 확인 — 보낸 직후엔 사용자 메시지만 있고 …는 마지막 노드
            last_msg = await page.evaluate("document.querySelector('#messages').lastElementChild.textContent")
            if last_msg and last_msg.strip() != "…":
                break
            await page.wait_for_timeout(1000)
        await page.wait_for_timeout(500)
        await step(page, "03-chat-response-shopper")

        # 응답 내용 출력
        last_msg = await page.evaluate("document.querySelector('#messages').lastElementChild.textContent")
        print(f"     [응답] {last_msg[:200]}")

        print("▶ 4. 로그아웃 → catalog-only-llm 로그인")
        await page.click("#logout-btn")
        await page.wait_for_selector("#login-view", state="visible")
        await page.select_option("#client-id", "catalog-only-llm")
        await page.fill("#client-secret", "dev-secret-2")
        await page.click("#login-btn")
        await page.wait_for_selector("#chat-view", state="visible", timeout=10000)
        await page.wait_for_timeout(500)
        await step(page, "04-chat-view-catalog-only")

        print("▶ 5. 채팅: '주문 ORD-1001 상태 알려줘' (order:read 없음 → 권한 부족 안내 기대)")
        await page.fill("#message-input", "주문 ORD-1001 배송 상태 알려줘")
        await page.click("#send-btn")
        for _ in range(60):
            last_msg = await page.evaluate("document.querySelector('#messages').lastElementChild.textContent")
            if last_msg and last_msg.strip() != "…":
                break
            await page.wait_for_timeout(1000)
        await page.wait_for_timeout(500)
        await step(page, "05-chat-response-catalog-only")

        last_msg = await page.evaluate("document.querySelector('#messages').lastElementChild.textContent")
        print(f"     [응답] {last_msg[:300]}")

        await browser.close()
        print("✓ UI 검증 완료 — 스크린샷 5장 저장")


asyncio.run(main())
