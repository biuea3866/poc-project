-- Phase 2 Feature Flag 초기값 INSERT (모두 OFF)
-- Sprint 8 Canary 배포 시 점진적으로 ON 전환 (5% -> 25% -> 50% -> 100%)

INSERT INTO simple_runtime_config (config_key, config_value, description, created_at, updated_at) VALUES
-- Sprint 5
('OUTBOX_POLLING_ENABLED',      'false', 'Outbox Poller를 통한 Kafka 이벤트 발행 활성화',                       NOW(6), NOW(6)),
('ROLE_AUTHORIZATION_ENABLED',  'false', 'X-Member-Role 헤더 기반 역할 인가 활성화',                            NOW(6), NOW(6)),
('INVENTORY_KAFKA_ENABLED',     'false', '재고 서비스 Kafka Consumer (order.created/cancelled) 활성화',          NOW(6), NOW(6)),
('SEARCH_INDEXING_ENABLED',     'false', 'product.* Kafka Consumer -> ES 인덱싱 파이프라인 활성화',             NOW(6), NOW(6)),

-- Sprint 6
('SHIPPING_SERVICE_ENABLED',    'false', '배송 서비스 (택배사 연동, 배송 추적) 활성화',                         NOW(6), NOW(6)),
('AUTO_CONFIRM_BATCH_ENABLED',  'false', '배송 완료 후 7일 자동 구매확정 배치 활성화',                          NOW(6), NOW(6)),
('RETURN_REQUEST_ENABLED',      'false', '반품 요청 접수 및 처리 기능 활성화',                                  NOW(6), NOW(6)),
('SEARCH_FILTER_ENABLED',       'false', '카테고리/브랜드/가격/사이즈/색상 검색 필터 활성화',                   NOW(6), NOW(6)),

-- Sprint 7
('REVIEW_SERVICE_ENABLED',      'false', '리뷰 작성/조회/삭제 기능 활성화',                                    NOW(6), NOW(6)),
('REVIEW_POINT_ENABLED',        'false', '리뷰 작성 시 포인트 적립 (텍스트 200P, 포토 500P) 활성화',           NOW(6), NOW(6)),
('EXCHANGE_REQUEST_ENABLED',    'false', '교환 요청 접수 및 처리 기능 활성화',                                  NOW(6), NOW(6)),
('AUTOCOMPLETE_ENABLED',        'false', 'edge_ngram 기반 자동완성 검색 활성화',                                NOW(6), NOW(6)),
('POPULAR_KEYWORDS_ENABLED',    'false', 'Redis Sorted Set 기반 인기 검색어 기능 활성화',                       NOW(6), NOW(6));
