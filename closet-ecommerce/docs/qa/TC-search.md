# Search 테스트 케이스

> 도메인: 검색 (search-service)
> 작성일: 2026-04-05
> 총 15건

## 테스트 케이스

| TC-ID | 분류 | 시나리오 | 사전조건 | 절차 | 기대결과 | 우선순위 |
|-------|------|----------|----------|------|----------|----------|
| SCH-001 | 키워드 검색 | 한글 키워드 검색 | 상품 "나이키 에어맥스 화이트" 인덱싱 완료 | 1. GET `/api/v1/search?keyword=나이키` 호출 | 200 OK, "나이키" 포함 상품 목록 반환, totalCount > 0 | P1 |
| SCH-002 | 키워드 검색 | 영어 키워드 검색 | 상품 "Nike Air Max" 인덱싱 완료 | 1. GET `/api/v1/search?keyword=Nike` 호출 | 200 OK, 대소문자 무관 "nike" 포함 상품 반환 | P1 |
| SCH-003 | 키워드 검색 | 복합어 검색 | 상품 "나이키 에어맥스 97 화이트" 인덱싱 완료 | 1. GET `/api/v1/search?keyword=나이키 화이트` 호출 | 200 OK, "나이키"와 "화이트" 모두 포함 상품 우선 반환 | P1 |
| SCH-004 | 필터 | 카테고리 필터 | 상의/하의/아우터 카테고리 상품 등록 | 1. GET `/api/v1/search?keyword=나이키&category=상의` 호출 | 200 OK, 카테고리 "상의"에 해당하는 나이키 상품만 반환 | P1 |
| SCH-005 | 필터 | 브랜드 필터 | Nike, Adidas 브랜드 상품 등록 | 1. GET `/api/v1/search?keyword=운동화&brand=Nike` 호출 | 200 OK, Nike 브랜드 운동화만 반환 | P1 |
| SCH-006 | 필터 | 가격 범위 필터 | 다양한 가격대 상품 등록 | 1. GET `/api/v1/search?keyword=티셔츠&minPrice=10000&maxPrice=50000` 호출 | 200 OK, 가격 10,000~50,000원 범위 상품만 반환 | P1 |
| SCH-007 | 필터 | 색상 필터 | 화이트/블랙/네이비 상품 등록 | 1. GET `/api/v1/search?keyword=맨투맨&color=WHITE` 호출 | 200 OK, 화이트 색상 맨투맨 상품만 반환 | P2 |
| SCH-008 | 필터 | 사이즈 필터 | S/M/L/XL 사이즈 상품 등록 | 1. GET `/api/v1/search?keyword=청바지&size=M` 호출 | 200 OK, M 사이즈 재고 있는 청바지만 반환 | P2 |
| SCH-009 | 필터 | 복합 필터 조합 | 다양한 상품 등록 | 1. GET `/api/v1/search?keyword=맨투맨&category=상의&brand=Nike&minPrice=30000&maxPrice=80000&color=BLACK&size=L` 호출 | 200 OK, 모든 필터 조건 동시 적용, AND 조건 결과 반환 | P1 |
| SCH-010 | 자동완성 | 2자 이상 입력 시 자동완성 | 인기 검색어 "나이키", "나이키 에어맥스" 등록 | 1. GET `/api/v1/search/autocomplete?q=나이` 호출 | 200 OK, "나이키"로 시작하는 자동완성 목록 최대 10건 반환 | P2 |
| SCH-011 | 자동완성 | 자동완성 응답 시간 | 자동완성 데이터 캐시 완료 | 1. GET `/api/v1/search/autocomplete?q=나이` 호출 후 응답 시간 측정 | 응답 시간 100ms 이내 | P2 |
| SCH-012 | 인기검색어 | Top 10 인기검색어 조회 | 검색 로그 누적 완료 | 1. GET `/api/v1/search/popular` 호출 | 200 OK, 검색 횟수 기준 상위 10개 키워드 반환, 순위 포함 | P2 |
| SCH-013 | 인기검색어 | 금칙어 필터링 | 금칙어 "욕설A" 등록, 해당 키워드 검색 다수 | 1. GET `/api/v1/search/popular` 호출 | 200 OK, 금칙어 키워드 목록에서 제외 | P3 |
| SCH-014 | 인덱싱 | 상품 생성 시 자동 인덱싱 | 없음 | 1. `event.closet.product` 상품 생성 이벤트 수신 | Elasticsearch 인덱스에 신규 문서 추가, 즉시 검색 가능 | P1 |
| SCH-015 | 인덱싱 | 상품 삭제 시 인덱스 제거 | 상품 `PRD-001` 인덱싱 완료 | 1. `event.closet.product` 상품 삭제 이벤트 수신 | Elasticsearch 인덱스에서 문서 삭제, 검색 결과에서 제외 | P1 |
