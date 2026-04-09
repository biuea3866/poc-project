-- ============================================================
-- V5: Point 도메인이 closet-member로 이관됨에 따라 promotion에서 Point 테이블 삭제
-- Point(적립금)는 회원의 금융 자산이므로 member BC에서 관리
-- ============================================================

DROP TABLE IF EXISTS point_history;
DROP TABLE IF EXISTS point_balance;
DROP TABLE IF EXISTS point_policy;
