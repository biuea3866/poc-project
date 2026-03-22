-- ================================================================
-- Closet E-Commerce 더미 데이터 (무신사 벤치마킹)
-- ================================================================

USE closet;

-- ================================================================
-- 1. 카테고리 (대/중/소 3-depth)
-- ================================================================
INSERT INTO category (id, parent_id, name, depth, sort_order, status, created_at, updated_at) VALUES
-- 대 카테고리
(1, NULL, '상의', 1, 1, 'ACTIVE', NOW(6), NOW(6)),
(2, NULL, '하의', 1, 2, 'ACTIVE', NOW(6), NOW(6)),
(3, NULL, '아우터', 1, 3, 'ACTIVE', NOW(6), NOW(6)),
(4, NULL, '신발', 1, 4, 'ACTIVE', NOW(6), NOW(6)),
(5, NULL, '가방', 1, 5, 'ACTIVE', NOW(6), NOW(6)),
(6, NULL, '액세서리', 1, 6, 'ACTIVE', NOW(6), NOW(6)),
-- 중 카테고리 (상의)
(10, 1, '반소매 티셔츠', 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(11, 1, '긴소매 티셔츠', 2, 2, 'ACTIVE', NOW(6), NOW(6)),
(12, 1, '맨투맨/스웨트', 2, 3, 'ACTIVE', NOW(6), NOW(6)),
(13, 1, '후드 티셔츠', 2, 4, 'ACTIVE', NOW(6), NOW(6)),
(14, 1, '니트/스웨터', 2, 5, 'ACTIVE', NOW(6), NOW(6)),
(15, 1, '셔츠/블라우스', 2, 6, 'ACTIVE', NOW(6), NOW(6)),
(16, 1, '피케/카라 티셔츠', 2, 7, 'ACTIVE', NOW(6), NOW(6)),
-- 중 카테고리 (하의)
(20, 2, '데님 팬츠', 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(21, 2, '코튼 팬츠', 2, 2, 'ACTIVE', NOW(6), NOW(6)),
(22, 2, '슬랙스', 2, 3, 'ACTIVE', NOW(6), NOW(6)),
(23, 2, '숏 팬츠', 2, 4, 'ACTIVE', NOW(6), NOW(6)),
(24, 2, '트레이닝/조거', 2, 5, 'ACTIVE', NOW(6), NOW(6)),
-- 중 카테고리 (아우터)
(30, 3, '후드 집업', 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(31, 3, '블루종/MA-1', 2, 2, 'ACTIVE', NOW(6), NOW(6)),
(32, 3, '코치 재킷', 2, 3, 'ACTIVE', NOW(6), NOW(6)),
(33, 3, '가디건', 2, 4, 'ACTIVE', NOW(6), NOW(6)),
(34, 3, '패딩', 2, 5, 'ACTIVE', NOW(6), NOW(6)),
(35, 3, '코트', 2, 6, 'ACTIVE', NOW(6), NOW(6)),
-- 중 카테고리 (신발)
(40, 4, '스니커즈', 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(41, 4, '구두/로퍼', 2, 2, 'ACTIVE', NOW(6), NOW(6)),
(42, 4, '샌들/슬리퍼', 2, 3, 'ACTIVE', NOW(6), NOW(6)),
(43, 4, '부츠', 2, 4, 'ACTIVE', NOW(6), NOW(6));

-- ================================================================
-- 2. 브랜드 (30개)
-- ================================================================
INSERT INTO brand (id, name, logo_url, description, seller_id, status, created_at, updated_at) VALUES
(1,  '무신사 스탠다드', '/brands/musinsa-standard.png', '무신사의 자체 브랜드. 합리적 가격의 베이직 아이템', 1, 'ACTIVE', NOW(6), NOW(6)),
(2,  '커버낫', '/brands/covernat.png', '아메리칸 캐주얼 기반 스트릿 브랜드', 2, 'ACTIVE', NOW(6), NOW(6)),
(3,  '디스이즈네버댓', '/brands/thisisneverthat.png', '한국 대표 스트릿 브랜드', 3, 'ACTIVE', NOW(6), NOW(6)),
(4,  '아디다스', '/brands/adidas.png', '글로벌 스포츠 브랜드', 4, 'ACTIVE', NOW(6), NOW(6)),
(5,  '나이키', '/brands/nike.png', '글로벌 스포츠 브랜드', 5, 'ACTIVE', NOW(6), NOW(6)),
(6,  '칼하트', '/brands/carhartt.png', '워크웨어 헤리티지 브랜드', 6, 'ACTIVE', NOW(6), NOW(6)),
(7,  '스투시', '/brands/stussy.png', '캘리포니아 스트릿 브랜드', 7, 'ACTIVE', NOW(6), NOW(6)),
(8,  '내셔널지오그래픽', '/brands/nationalgeographic.png', '아웃도어 라이프스타일', 8, 'ACTIVE', NOW(6), NOW(6)),
(9,  '폴로 랄프로렌', '/brands/polo.png', '아메리칸 클래식 프리미엄', 9, 'ACTIVE', NOW(6), NOW(6)),
(10, '리바이스', '/brands/levis.png', '데님의 오리지널', 10, 'ACTIVE', NOW(6), NOW(6)),
(11, '마르디 메크르디', '/brands/mardi.png', '프렌치 감성 브랜드', 11, 'ACTIVE', NOW(6), NOW(6)),
(12, '아크네 스튜디오', '/brands/acne.png', '스웨덴 미니멀리즘', 12, 'ACTIVE', NOW(6), NOW(6)),
(13, '스톤아일랜드', '/brands/stoneisland.png', '이탈리아 테크니컬 스포츠웨어', 13, 'ACTIVE', NOW(6), NOW(6)),
(14, '메종키츠네', '/brands/maisonkitsune.png', '파리지앵 라이프스타일', 14, 'ACTIVE', NOW(6), NOW(6)),
(15, '아미', '/brands/ami.png', '파리 컨템포러리 브랜드', 15, 'ACTIVE', NOW(6), NOW(6)),
(16, '예일', '/brands/yale.png', '아이비리그 헤리티지', 16, 'ACTIVE', NOW(6), NOW(6)),
(17, '이자벨마랑', '/brands/isabelmarant.png', '프렌치 보헤미안 럭셔리', 17, 'ACTIVE', NOW(6), NOW(6)),
(18, '오프화이트', '/brands/offwhite.png', '럭셔리 스트릿 브랜드', 18, 'ACTIVE', NOW(6), NOW(6)),
(19, '꼼데가르송', '/brands/cdg.png', '아방가르드 일본 브랜드', 19, 'ACTIVE', NOW(6), NOW(6)),
(20, '뉴발란스', '/brands/newbalance.png', '보스턴 기반 스포츠 브랜드', 20, 'ACTIVE', NOW(6), NOW(6)),
(21, '컨버스', '/brands/converse.png', '올스타 스니커즈의 원조', 21, 'ACTIVE', NOW(6), NOW(6)),
(22, '반스', '/brands/vans.png', '스케이트보드 문화의 아이콘', 22, 'ACTIVE', NOW(6), NOW(6)),
(23, '엠엘비', '/brands/mlb.png', '스포츠 캐주얼 브랜드', 23, 'ACTIVE', NOW(6), NOW(6)),
(24, '노스페이스', '/brands/northface.png', '아웃도어 라이프스타일', 24, 'ACTIVE', NOW(6), NOW(6)),
(25, '파타고니아', '/brands/patagonia.png', '지속가능한 아웃도어 브랜드', 25, 'ACTIVE', NOW(6), NOW(6)),
(26, '라코스테', '/brands/lacoste.png', '프렌치 스포츠 엘레강스', 26, 'ACTIVE', NOW(6), NOW(6)),
(27, '챔피온', '/brands/champion.png', '어센틱 아메리칸 스포츠웨어', 27, 'ACTIVE', NOW(6), NOW(6)),
(28, '리', '/brands/lee.png', '데님 헤리티지 브랜드', 28, 'ACTIVE', NOW(6), NOW(6)),
(29, '타미힐피거', '/brands/tommy.png', '클래식 아메리칸 프리미엄', 29, 'ACTIVE', NOW(6), NOW(6)),
(30, '휠라', '/brands/fila.png', '이탈리아 스포츠 헤리티지', 30, 'ACTIVE', NOW(6), NOW(6));

-- ================================================================
-- 3. 상품 (50개 — 다양한 카테고리/브랜드)
-- ================================================================
INSERT INTO product (id, name, description, brand_id, category_id, base_price, sale_price, discount_rate, status, season, fit_type, gender, created_at, updated_at) VALUES
-- 상의 - 반소매 티셔츠
(1, '무신사 스탠다드 릴렉스핏 크루넥 반팔 티셔츠', '부드러운 코튼 소재의 데일리 반팔 티셔츠. 릴렉스핏으로 편안한 착용감.', 1, 10, 19900, 12900, 35, 'ACTIVE', 'SS', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(2, '커버낫 C 로고 반팔 티셔츠', '가슴 로고 자수 포인트의 클래식 반팔 티셔츠.', 2, 10, 39000, 29900, 23, 'ACTIVE', 'SS', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(3, '디스이즈네버댓 T-Logo 반팔 티셔츠', 'INTL 시그니처 로고 프린팅 티셔츠.', 3, 10, 45000, 45000, 0, 'ACTIVE', 'SS', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(4, '나이키 스포츠웨어 에센셜 티셔츠', '드라이핏 소재의 스포츠 퍼포먼스 티셔츠.', 5, 10, 39000, 29900, 23, 'ACTIVE', 'ALL', 'REGULAR', 'MALE', NOW(6), NOW(6)),
(5, '아디다스 트레포일 반팔 티', '아디다스 오리지널스 트레포일 로고 티셔츠.', 4, 10, 45000, 35900, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 상의 - 맨투맨/스웨트
(6, '무신사 스탠다드 에센셜 스웨트셔츠', '부드러운 기모 안감의 데일리 맨투맨.', 1, 12, 34900, 24900, 29, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(7, '커버낫 아치로고 스웨트셔츠', '시그니처 아치 로고 자수 맨투맨.', 2, 12, 69000, 55200, 20, 'ACTIVE', 'FW', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(8, '칼하트 포켓 스웨트셔츠', '포켓 디테일의 워크웨어 스웨트셔츠.', 6, 12, 99000, 79200, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(9, '챔피온 리버스위브 크루넥', 'USA산 리버스위브 헤비웨이트 맨투맨.', 27, 12, 89000, 62300, 30, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 상의 - 후드
(10, '무신사 스탠다드 오버핏 후드 스웨트셔츠', '여유로운 핏의 기본 후드 티셔츠.', 1, 13, 39900, 29900, 25, 'ACTIVE', 'FW', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(11, '스투시 베이직 후디', '스투시 시그니처 로고 후드.', 7, 13, 129000, 103200, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 상의 - 니트/스웨터
(12, '아크네 스튜디오 크루넥 니트', '미니멀 디자인의 울 블렌드 니트.', 12, 14, 390000, 312000, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(13, '폴로 랄프로렌 케이블 니트', '클래식 케이블 패턴의 코튼 니트.', 9, 14, 259000, 207200, 20, 'ACTIVE', 'FW', 'REGULAR', 'MALE', NOW(6), NOW(6)),
-- 상의 - 셔츠
(14, '무신사 스탠다드 옥스포드 셔츠', 'BD 카라 옥스포드 셔츠. 비즈캐주얼 추천.', 1, 15, 34900, 24900, 29, 'ACTIVE', 'ALL', 'REGULAR', 'MALE', NOW(6), NOW(6)),
(15, '마르디 메크르디 플라워 패턴 셔츠', '마르디 시그니처 플라워 프린트 셔츠.', 11, 15, 89000, 89000, 0, 'ACTIVE', 'SS', 'REGULAR', 'FEMALE', NOW(6), NOW(6)),
-- 하의 - 데님
(16, '리바이스 501 오리지널 스트레이트', '클래식 스트레이트 핏 데님.', 10, 20, 119000, 83300, 30, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(17, '무신사 스탠다드 와이드 데님 팬츠', '트렌디한 와이드 핏 데님.', 1, 20, 49900, 34900, 30, 'ACTIVE', 'ALL', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(18, '리 레귤러핏 데님', '편안한 레귤러 핏 데님 진.', 28, 20, 89000, 62300, 30, 'ACTIVE', 'ALL', 'REGULAR', 'MALE', NOW(6), NOW(6)),
-- 하의 - 슬랙스/치노
(19, '무신사 스탠다드 와이드 치노 팬츠', '깔끔한 실루엣의 와이드 치노.', 1, 22, 39900, 29900, 25, 'ACTIVE', 'ALL', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(20, '폴로 랄프로렌 클래식핏 치노', '프리미엄 코튼 치노 팬츠.', 9, 22, 179000, 143200, 20, 'ACTIVE', 'ALL', 'REGULAR', 'MALE', NOW(6), NOW(6)),
-- 하의 - 숏팬츠
(21, '나이키 스포츠웨어 클럽 쇼츠', '드라이핏 저지 소재 5인치 쇼츠.', 5, 23, 45000, 35900, 20, 'ACTIVE', 'SS', 'REGULAR', 'MALE', NOW(6), NOW(6)),
(22, '무신사 스탠다드 나일론 숏 팬츠', '가벼운 나일론 소재 하프 팬츠.', 1, 23, 29900, 19900, 33, 'ACTIVE', 'SS', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 아우터 - 후드집업
(23, '나이키 클럽 풀짚 후디', '데일리 후드 집업.', 5, 30, 79000, 63200, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(24, '노스페이스 윈드 재킷', '방풍 경량 재킷.', 24, 30, 129000, 103200, 20, 'ACTIVE', 'SS', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 아우터 - 블루종/코치
(25, '스투시 코치 재킷', '나일론 소재 코치 재킷.', 7, 32, 149000, 119200, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(26, '칼하트 미시간 코트', '칼하트 시그니처 워크 재킷.', 6, 32, 219000, 175200, 20, 'ACTIVE', 'FW', 'REGULAR', 'MALE', NOW(6), NOW(6)),
-- 아우터 - 패딩
(27, '노스페이스 1996 레트로 눕시 패딩', '700 필파워 구스다운 패딩.', 24, 34, 359000, 287200, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(28, '파타고니아 다운 스웨터 재킷', '경량 다운 재킷. 지속가능한 리사이클 소재.', 25, 34, 329000, 296100, 10, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 아우터 - 코트
(29, '아크네 스튜디오 울 블렌드 코트', '미니멀 실루엣의 울 블렌드 싱글 코트.', 12, 35, 890000, 712000, 20, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 신발 - 스니커즈
(30, '나이키 에어포스 1 07', '클래식 화이트 스니커즈.', 5, 40, 139000, 119000, 14, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(31, '아디다스 삼바 OG', '가죽 소재의 레트로 스니커즈.', 4, 40, 139000, 139000, 0, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(32, '뉴발란스 530', '메시 소재의 러닝 헤리티지 스니커즈.', 20, 40, 129000, 109000, 16, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(33, '컨버스 척테일러 올스타 70', '빈티지 감성의 하이탑 스니커즈.', 21, 40, 95000, 76000, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(34, '반스 올드스쿨', '사이드 스트라이프의 아이코닉 스케이트 슈즈.', 22, 40, 75000, 60000, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 신발 - 구두/로퍼
(35, '닥터마틴 1461', '3홀 옥스포드 슈즈.', 29, 41, 219000, 175200, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 가방
(36, '무신사 스탠다드 캔버스 토트백', '데일리 캔버스 토트백.', 1, 5, 19900, 14900, 25, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(37, '내셔널지오그래픽 백팩', '네오프렌 소재 데일리 백팩.', 8, 5, 89000, 71200, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 액세서리
(38, '무신사 스탠다드 볼캡', '6패널 코튼 볼캡.', 1, 6, 19900, 14900, 25, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(39, '예일 아치로고 볼캡', '예일 시그니처 아치 로고 캡.', 16, 6, 35000, 28000, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(40, '엠엘비 뉴욕양키스 볼캡', 'NY 로고 클래식 볼캡.', 23, 6, 39900, 31900, 20, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
-- 추가 인기 상품
(41, '디스이즈네버댓 아치로고 맨투맨', '시그니처 아치 로고 스웨트셔츠.', 3, 12, 79000, 63200, 20, 'ACTIVE', 'FW', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6)),
(42, '메종키츠네 폭스 헤드 패치 티셔츠', '폭스 헤드 와펜 장식 반팔 티.', 14, 10, 145000, 116000, 20, 'ACTIVE', 'SS', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(43, '아미 하트 로고 반팔 티셔츠', 'AMI 하트 자수 라운드넥 티셔츠.', 15, 10, 195000, 156000, 20, 'ACTIVE', 'SS', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(44, '라코스테 L.12.12 피케 폴로', '클래식 피케 소재 폴로 셔츠.', 26, 16, 149000, 119200, 20, 'ACTIVE', 'SS', 'REGULAR', 'MALE', NOW(6), NOW(6)),
(45, '타미힐피거 플래그 로고 맨투맨', '타미 플래그 자수 크루넥.', 29, 12, 159000, 111300, 30, 'ACTIVE', 'FW', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(46, '휠라 디스럽터 2', '어글리 슈즈 대표 모델.', 30, 40, 99000, 69300, 30, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(47, '꼼데가르송 플레이 하트 반팔 티', 'CDG PLAY 하트 와펜 티셔츠.', 19, 10, 135000, 135000, 0, 'ACTIVE', 'ALL', 'REGULAR', 'UNISEX', NOW(6), NOW(6)),
(48, '스톤아일랜드 와펜 맨투맨', '나침반 와펜 패치 스웨트셔츠.', 13, 12, 450000, 360000, 20, 'ACTIVE', 'FW', 'REGULAR', 'MALE', NOW(6), NOW(6)),
(49, '이자벨마랑 로고 맨투맨', '이자벨마랑 시그니처 로고 스웨트셔츠.', 17, 12, 390000, 312000, 20, 'ACTIVE', 'FW', 'REGULAR', 'FEMALE', NOW(6), NOW(6)),
(50, '오프화이트 애로우 후드', '오프화이트 시그니처 애로우 후드.', 18, 13, 590000, 472000, 20, 'ACTIVE', 'FW', 'OVERSIZED', 'UNISEX', NOW(6), NOW(6));

-- ================================================================
-- 4. 상품 옵션 (사이즈 × 색상) — 상품당 3~6개
-- ================================================================
-- 상품 1: 무신사 스탠다드 반팔 티 (5색 × 5사이즈 = 25 옵션)
INSERT INTO product_option (product_id, size, color_name, color_hex, sku_code, additional_price, created_at, updated_at) VALUES
(1, 'S', '화이트', '#FFFFFF', 'MSS-TEE-WHT-S', 0, NOW(6), NOW(6)),
(1, 'M', '화이트', '#FFFFFF', 'MSS-TEE-WHT-M', 0, NOW(6), NOW(6)),
(1, 'L', '화이트', '#FFFFFF', 'MSS-TEE-WHT-L', 0, NOW(6), NOW(6)),
(1, 'XL', '화이트', '#FFFFFF', 'MSS-TEE-WHT-XL', 0, NOW(6), NOW(6)),
(1, 'S', '블랙', '#000000', 'MSS-TEE-BLK-S', 0, NOW(6), NOW(6)),
(1, 'M', '블랙', '#000000', 'MSS-TEE-BLK-M', 0, NOW(6), NOW(6)),
(1, 'L', '블랙', '#000000', 'MSS-TEE-BLK-L', 0, NOW(6), NOW(6)),
(1, 'XL', '블랙', '#000000', 'MSS-TEE-BLK-XL', 0, NOW(6), NOW(6)),
(1, 'S', '네이비', '#1B2838', 'MSS-TEE-NVY-S', 0, NOW(6), NOW(6)),
(1, 'M', '네이비', '#1B2838', 'MSS-TEE-NVY-M', 0, NOW(6), NOW(6)),
(1, 'L', '네이비', '#1B2838', 'MSS-TEE-NVY-L', 0, NOW(6), NOW(6)),
(1, 'XL', '네이비', '#1B2838', 'MSS-TEE-NVY-XL', 0, NOW(6), NOW(6)),
-- 상품 2~5: 각 3옵션씩
(2, 'M', '화이트', '#FFFFFF', 'CVN-LOGO-WHT-M', 0, NOW(6), NOW(6)),
(2, 'L', '화이트', '#FFFFFF', 'CVN-LOGO-WHT-L', 0, NOW(6), NOW(6)),
(2, 'L', '블랙', '#000000', 'CVN-LOGO-BLK-L', 0, NOW(6), NOW(6)),
(3, 'M', '블랙', '#000000', 'TINT-TLOGO-BLK-M', 0, NOW(6), NOW(6)),
(3, 'L', '블랙', '#000000', 'TINT-TLOGO-BLK-L', 0, NOW(6), NOW(6)),
(3, 'XL', '화이트', '#FFFFFF', 'TINT-TLOGO-WHT-XL', 0, NOW(6), NOW(6)),
(4, 'M', '블랙', '#000000', 'NK-ESS-BLK-M', 0, NOW(6), NOW(6)),
(4, 'L', '블랙', '#000000', 'NK-ESS-BLK-L', 0, NOW(6), NOW(6)),
(4, 'L', '화이트', '#FFFFFF', 'NK-ESS-WHT-L', 0, NOW(6), NOW(6)),
(5, 'M', '블랙', '#000000', 'AD-TREF-BLK-M', 0, NOW(6), NOW(6)),
(5, 'L', '블랙', '#000000', 'AD-TREF-BLK-L', 0, NOW(6), NOW(6)),
(5, 'L', '화이트', '#FFFFFF', 'AD-TREF-WHT-L', 0, NOW(6), NOW(6)),
-- 신발 (사이즈만 다름)
(30, '260', '화이트', '#FFFFFF', 'NK-AF1-WHT-260', 0, NOW(6), NOW(6)),
(30, '270', '화이트', '#FFFFFF', 'NK-AF1-WHT-270', 0, NOW(6), NOW(6)),
(30, '280', '화이트', '#FFFFFF', 'NK-AF1-WHT-280', 0, NOW(6), NOW(6)),
(31, '260', '화이트', '#FFFFFF', 'AD-SAMBA-WHT-260', 0, NOW(6), NOW(6)),
(31, '270', '화이트', '#FFFFFF', 'AD-SAMBA-WHT-270', 0, NOW(6), NOW(6)),
(31, '280', '화이트', '#FFFFFF', 'AD-SAMBA-WHT-280', 0, NOW(6), NOW(6)),
(32, '260', '실버', '#C0C0C0', 'NB-530-SLV-260', 0, NOW(6), NOW(6)),
(32, '270', '실버', '#C0C0C0', 'NB-530-SLV-270', 0, NOW(6), NOW(6)),
(32, '280', '실버', '#C0C0C0', 'NB-530-SLV-280', 0, NOW(6), NOW(6));

-- ================================================================
-- 5. 상품 이미지 (상품당 1~2장)
-- ================================================================
INSERT INTO product_image (product_id, image_url, type, sort_order, created_at) VALUES
(1, '/products/mss-tee-white-main.jpg', 'MAIN', 1, NOW(6)),
(1, '/products/mss-tee-white-detail1.jpg', 'DETAIL', 2, NOW(6)),
(2, '/products/cvn-logo-main.jpg', 'MAIN', 1, NOW(6)),
(3, '/products/tint-tlogo-main.jpg', 'MAIN', 1, NOW(6)),
(4, '/products/nk-ess-main.jpg', 'MAIN', 1, NOW(6)),
(5, '/products/ad-trefoil-main.jpg', 'MAIN', 1, NOW(6)),
(6, '/products/mss-sweat-main.jpg', 'MAIN', 1, NOW(6)),
(7, '/products/cvn-arch-main.jpg', 'MAIN', 1, NOW(6)),
(10, '/products/mss-hood-main.jpg', 'MAIN', 1, NOW(6)),
(16, '/products/levis-501-main.jpg', 'MAIN', 1, NOW(6)),
(17, '/products/mss-wide-denim-main.jpg', 'MAIN', 1, NOW(6)),
(27, '/products/tnf-nuptse-main.jpg', 'MAIN', 1, NOW(6)),
(30, '/products/nk-af1-main.jpg', 'MAIN', 1, NOW(6)),
(31, '/products/ad-samba-main.jpg', 'MAIN', 1, NOW(6)),
(32, '/products/nb-530-main.jpg', 'MAIN', 1, NOW(6)),
(42, '/products/mk-fox-main.jpg', 'MAIN', 1, NOW(6)),
(47, '/products/cdg-play-main.jpg', 'MAIN', 1, NOW(6)),
(48, '/products/si-wappen-main.jpg', 'MAIN', 1, NOW(6));

-- ================================================================
-- 6. 테스트 회원 (5명)
-- ================================================================
INSERT INTO member (id, email, password_hash, name, phone, grade, point_balance, status, created_at, updated_at) VALUES
(1, 'user1@closet.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '김민수', '01012345678', 'GOLD', 15000, 'ACTIVE', NOW(6), NOW(6)),
(2, 'user2@closet.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '이지은', '01023456789', 'SILVER', 8000, 'ACTIVE', NOW(6), NOW(6)),
(3, 'user3@closet.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '박서준', '01034567890', 'PLATINUM', 50000, 'ACTIVE', NOW(6), NOW(6)),
(4, 'user4@closet.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '최수진', '01045678901', 'NORMAL', 1000, 'ACTIVE', NOW(6), NOW(6)),
(5, 'seller1@closet.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '셀러관리자', '01056789012', 'NORMAL', 0, 'ACTIVE', NOW(6), NOW(6));
-- 비밀번호: 모두 "password123"

-- ================================================================
-- 7. 배송지
-- ================================================================
INSERT INTO shipping_address (member_id, name, phone, zip_code, address, detail_address, is_default, created_at, updated_at) VALUES
(1, '김민수', '01012345678', '06035', '서울특별시 강남구 가로수길 50', '3층 302호', 1, NOW(6), NOW(6)),
(1, '김민수', '01012345678', '04527', '서울특별시 중구 명동길 14', '명동 빌딩 5층', 0, NOW(6), NOW(6)),
(2, '이지은', '01023456789', '03925', '서울특별시 마포구 양화로 45', '홍대 오피스텔 801호', 1, NOW(6), NOW(6)),
(3, '박서준', '01034567890', '06164', '서울특별시 강남구 테헤란로 152', '역삼동 타워 1201호', 1, NOW(6), NOW(6));
