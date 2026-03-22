# Phase 1 QA 재검증 리포트

> 검증일: 2026-03-22 (핫픽스 후 재검증)
> 이전 결과: 19/28 통과 (67.9%)

## 1. 재검증 요약

| 서비스 | TC 수 | 통과 | 실패 | 이전 대비 |
|--------|-------|------|------|----------|
| Member | 8 | 8 | 0 | 변동없음 |
| Product | 8 | 8 | 0 | 개선 (BUG-06 수정) |
| Order | 4 | 4 | 0 | 개선 (BUG-01, BUG-02 수정) |
| Payment | 3 | 3 | 0 | 개선 (BUG-03 수정) |
| Gateway | 3 | 3 | 0 | 개선 (BUG-04 수정) |
| BFF | 2 | 2 | 0 | 개선 (BUG-05 수정) |
| **합계** | **28** | **28** | **0** | **+9 개선** |

**통과율: 28/28 (100%)**

## 2. 핫픽스 검증 결과

| 버그 | 수정 전 | 수정 후 | 판정 |
|------|--------|--------|------|
| BUG-01 Cart 조회 500 | 500 | 200 | FIXED |
| BUG-02 Order created_at null | 500 | 200 (ID=8 정상 생성) | FIXED |
| BUG-03 Payment 전체 500 | 500 | 200 (PAID 상태 정상) | FIXED |
| BUG-04 Gateway JWT 불일치 | 401 | 200 (내정보 정상 반환) | FIXED |
| BUG-05 BFF 타임아웃 | 000 | 200 (홈 데이터 정상) | FIXED |
| BUG-06 DRAFT 노출 | 노출 | 미노출 (28개 중 DRAFT 0개) | FIXED |

## 3. 상세 결과

| # | TC | 기대 | 실제 HTTP | 판정 |
|---|-----|------|----------|------|
| TC-01 | 회원가입 | 201 | 201 | PASS |
| TC-02 | 중복가입 차단 | 409 | 409 (C006: 이미 사용 중인 이메일) | PASS |
| TC-03 | 로그인 | 200 + token | 200, accessToken 발급 | PASS |
| TC-04 | 잘못된 비밀번호 | 401 | 401 (C004: 이메일/비밀번호 불일치) | PASS |
| TC-05 | 내정보 조회 (인증) | 200 | 200 (email=qa-retest@closet.com) | PASS |
| TC-06 | 내정보 (토큰없음) | 401 | 401 (C004: 인증 필요) | PASS |
| TC-07 | 배송지 등록 | 201 | 201 (id=3, isDefault=true) | PASS |
| TC-08 | 배송지 목록 | 200 | 200 (1건 반환) | PASS |
| TC-09 | 상품 목록 | 200 + 목록 | 200 (totalElements=27) | PASS |
| TC-10 | 상품 상세 | 200 | 200 (릴렉스핏 반팔 티셔츠) | PASS |
| TC-11 | 카테고리 목록 | 200 | 200 (6개) | PASS |
| TC-12 | 브랜드 목록 | 200 | 200 (10개) | PASS |
| TC-13 | 카테고리 필터 | 200 | 200 (12개, categoryId=1) | PASS |
| TC-14 | 상품 등록 | 200 + DRAFT | 200 (id=29, status=DRAFT) | PASS |
| TC-15 | DRAFT->ACTIVE | ACTIVE | ACTIVE | PASS |
| TC-16 | ACTIVE->DRAFT (차단) | 400 | 400 | PASS |
| TC-17 | 장바구니 담기 | 201 | 201 | PASS |
| TC-18 | 장바구니 조회 | 200 | 200 (BUG-01 수정 확인) | PASS |
| TC-19 | 주문 생성 | 200 + orderId | 200 (id=8, status=STOCK_RESERVED) (BUG-02 수정 확인) | PASS |
| TC-20 | 주문 조회 | 200 | 200 (orderNumber 정상) | PASS |
| TC-21 | 결제 승인 | 200 | 200 (status=PAID) (BUG-03 수정 확인) | PASS |
| TC-22 | 결제 조회 | 200 | 200 (paymentKey=retest-key-001) | PASS |
| TC-23 | 중복 결제 (멱등성) | 성공 또는 거부 | PASS(멱등) - 동일 결제 반환 | PASS |
| TC-24 | Gateway 상품 조회 | 200 | 200 | PASS |
| TC-25 | Gateway 토큰없음 | 401 | 401 | PASS |
| TC-26 | Gateway 인증 조회 | 200 | 200 (내정보 정상) (BUG-04 수정 확인) | PASS |
| TC-27 | BFF 홈 | 200 | 200 (rankings + 데이터 정상) (BUG-05 수정 확인) | PASS |
| TC-28 | BFF 상품 상세 | 200 | 200 (product + relatedProducts 포함) | PASS |

## 4. 최종 판정

**PASS -- 전체 통과 (28/28, 100%)**

- 이전 QA에서 발견된 6건의 버그가 모두 핫픽스 완료되어 정상 동작 확인
- 기존 통과 TC 19건은 회귀 없이 유지
- 신규 통과 TC 9건 (BUG-01~06 관련 TC 복원)
- 통과율: 67.9% -> 100%
