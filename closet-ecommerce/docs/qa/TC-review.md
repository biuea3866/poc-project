# Review 테스트 케이스

> 도메인: 리뷰 (review-service)
> 작성일: 2026-04-05
> 총 16건

## 테스트 케이스

| TC-ID | 분류 | 시나리오 | 사전조건 | 절차 | 기대결과 | 우선순위 |
|-------|------|----------|----------|------|----------|----------|
| REV-001 | 리뷰 작성 | 텍스트 리뷰 작성 | 회원 `MBR-001`, 주문 `ORD-001` 구매확정 완료 | 1. POST `/api/v1/reviews` (productId, orderId, rating: 4, content: "좋은 상품입니다") 호출 | 201 Created, 리뷰 생성, `event.closet.review` 이벤트 발행 | P1 |
| REV-002 | 리뷰 작성 | 포토 리뷰 작성 | 회원 `MBR-001`, 구매확정 완료, 이미지 3장 업로드 | 1. POST `/api/v1/reviews` (rating: 5, content, imageUrls: [3장]) 호출 | 201 Created, 리뷰 + 이미지 3장 저장, 포토 리뷰로 분류 | P1 |
| REV-003 | 리뷰 작성 | 사이즈 후기 리뷰 작성 | 회원 `MBR-001`, 의류 상품 구매확정 완료 | 1. POST `/api/v1/reviews` (rating: 4, content, sizeFit: LARGE, heightRange: "170-175") 호출 | 201 Created, 사이즈 후기 정보 포함 저장 | P1 |
| REV-004 | 리뷰 작성 | 구매확정 없이 리뷰 작성 시도 | 회원 `MBR-001`, 주문 `ORD-002` 배송중 상태 | 1. POST `/api/v1/reviews` (orderId: ORD-002) 호출 | 409 Conflict, `ORDER_NOT_CONFIRMED` 에러 코드 | P1 |
| REV-005 | 리뷰 작성 | 동일 주문상품 중복 리뷰 작성 | 회원 `MBR-001`, 이미 리뷰 작성 완료 | 1. POST `/api/v1/reviews` (동일 orderId, productId) 호출 | 409 Conflict, `REVIEW_ALREADY_EXISTS` 에러 코드 | P1 |
| REV-006 | 리뷰 수정 | 7일 이내 리뷰 수정 | 리뷰 작성 3일 경과 | 1. PUT `/api/v1/reviews/{reviewId}` (rating: 3, content: "수정합니다") 호출 | 200 OK, 리뷰 내용/별점 수정 반영 | P2 |
| REV-007 | 리뷰 수정 | 7일 초과 리뷰 수정 거부 | 리뷰 작성 8일 경과 | 1. PUT `/api/v1/reviews/{reviewId}` 호출 | 409 Conflict, `REVIEW_EDIT_PERIOD_EXPIRED` 에러 코드 | P2 |
| REV-008 | 리뷰 삭제 | 본인 리뷰 삭제 | 회원 `MBR-001`이 작성한 리뷰 | 1. DELETE `/api/v1/reviews/{reviewId}` (memberId: MBR-001) 호출 | 200 OK, 리뷰 소프트 삭제, 상품 평균 별점 재계산 | P1 |
| REV-009 | 리뷰 삭제 | 타인 리뷰 삭제 시도 | 회원 `MBR-002`가 `MBR-001`의 리뷰 삭제 시도 | 1. DELETE `/api/v1/reviews/{reviewId}` (memberId: MBR-002) 호출 | 403 Forbidden, `REVIEW_NOT_OWNED` 에러 코드 | P1 |
| REV-010 | 포인트 적립 | 텍스트 리뷰 100P 적립 | 회원 `MBR-001`, 텍스트 리뷰 작성 | 1. 리뷰 작성 완료 후 포인트 적립 이벤트 확인 | `event.closet.review` 포인트 적립 이벤트 발행, 100P 적립 | P1 |
| REV-011 | 포인트 적립 | 포토 리뷰 300P 적립 | 회원 `MBR-001`, 포토 리뷰 작성 (이미지 1장 이상) | 1. 포토 리뷰 작성 완료 후 포인트 적립 이벤트 확인 | `event.closet.review` 포인트 적립 이벤트 발행, 300P 적립 | P1 |
| REV-012 | 포인트 적립 | 사이즈 후기 추가 +50P 적립 | 회원 `MBR-001`, 사이즈 후기 포함 리뷰 작성 | 1. 사이즈 후기 포함 리뷰 작성 완료 후 포인트 확인 | 기본 적립(100P 또는 300P) + 사이즈 후기 보너스 50P 추가 적립 | P2 |
| REV-013 | 포인트 적립 | 일일 상한 초과 시 적립 제한 | 회원 `MBR-001`, 당일 리뷰 포인트 1,000P 적립 완료 | 1. 추가 리뷰 작성 시도 | 리뷰 작성은 성공, 포인트 적립은 일일 상한(1,000P) 초과로 미적립, 안내 메시지 반환 | P2 |
| REV-014 | 포인트 적립 | 리뷰 삭제 시 포인트 회수 | 100P 적립된 리뷰 존재 | 1. DELETE `/api/v1/reviews/{reviewId}` 호출 | 리뷰 삭제, 적립된 100P 회수 이벤트 발행 | P1 |
| REV-015 | 리뷰 집계 | 상품 평균 별점 및 분포 조회 | 상품 `PRD-001`에 리뷰 10건 (별점 분포: 5점 4건, 4점 3건, 3점 2건, 2점 1건) | 1. GET `/api/v1/reviews/products/{productId}/summary` 호출 | 200 OK, 평균 별점 4.0, 별점별 분포 (5:4, 4:3, 3:2, 2:1, 1:0), 총 리뷰 수 10 | P1 |
| REV-016 | 도움이 됐어요 | 1회 투표 및 취소 | 회원 `MBR-002`, 리뷰 `REV-001` 도움이 됐어요 미투표 | 1. POST `/api/v1/reviews/{reviewId}/helpful` (memberId: MBR-002) 2. 재호출 | 첫 호출: helpfulCount +1, 재호출: helpfulCount -1 (토글 방식) | P2 |
