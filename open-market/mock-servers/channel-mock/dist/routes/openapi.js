"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const router = (0, express_1.Router)();
router.get('/openapi.json', (_req, res) => {
    res.json({
        openapi: '3.0.0',
        info: {
            title: 'Channel Mock Server API',
            version: '1.0.0',
        },
        paths: {
            '/api/st11/auth/token': { post: { summary: 'ST11 인증 토큰 발급' } },
            '/api/st11/products': { post: { summary: 'ST11 상품 등록' } },
            '/api/st11/products/{productNo}': { get: { summary: 'ST11 상품 조회' } },
            '/api/st11/orders': { get: { summary: 'ST11 주문 목록' } },
            '/api/st11/orders/{orderNo}/ship': { post: { summary: 'ST11 발송 처리' } },
            '/api/st11/webhooks/register': { post: { summary: 'ST11 웹훅 등록' } },
            '/api/st11/webhooks/events': { get: { summary: 'ST11 웹훅 이벤트 목록' } },
            '/api/st11/webhooks/trigger': { post: { summary: 'ST11 웹훅 트리거' } },
            '/api/naver/oauth/token': { post: { summary: '네이버 인증 토큰 발급' } },
            '/api/naver/products': { post: { summary: '네이버 상품 등록' } },
            '/api/naver/products/{productNo}': { get: { summary: '네이버 상품 조회' } },
            '/api/naver/orders/search': { post: { summary: '네이버 주문 검색' } },
            '/api/naver/orders/{productOrderId}/ship': { post: { summary: '네이버 발송 처리' } },
            '/api/naver/webhooks/register': { post: { summary: '네이버 웹훅 등록' } },
            '/api/naver/webhooks/events': { get: { summary: '네이버 웹훅 이벤트 목록' } },
            '/api/naver/webhooks/trigger': { post: { summary: '네이버 웹훅 트리거' } },
            '/api/kakao/oauth/token': { post: { summary: '카카오 인증 토큰 발급' } },
            '/api/kakao/products': { post: { summary: '카카오 상품 등록' } },
            '/api/kakao/orders': { get: { summary: '카카오 주문 조회' } },
            '/api/kakao/webhooks/register': { post: { summary: '카카오 웹훅 등록' } },
            '/api/kakao/webhooks/events': { get: { summary: '카카오 웹훅 이벤트 목록' } },
            '/api/kakao/webhooks/trigger': { post: { summary: '카카오 웹훅 트리거' } },
            '/api/toss/v1/auth/token': { post: { summary: '토스 인증 토큰 발급' } },
            '/api/toss/v1/products': { post: { summary: '토스 상품 등록' } },
            '/api/toss/v1/orders': { get: { summary: '토스 주문 조회' } },
            '/api/toss/webhooks/register': { post: { summary: '토스 웹훅 등록' } },
            '/api/toss/webhooks/events': { get: { summary: '토스 웹훅 이벤트 목록' } },
            '/api/toss/webhooks/trigger': { post: { summary: '토스 웹훅 트리거' } },
            '/api/coupang/v2/products': { post: { summary: '쿠팡 상품 등록' } },
            '/api/coupang/v2/orders': { get: { summary: '쿠팡 주문 조회' } },
            '/api/coupang/v2/orders/{shipmentBoxId}/invoice': { put: { summary: '쿠팡 송장 등록' } },
            '/api/coupang/webhooks/register': { post: { summary: '쿠팡 웹훅 등록' } },
            '/api/coupang/webhooks/events': { get: { summary: '쿠팡 웹훅 이벤트 목록' } },
            '/api/coupang/webhooks/trigger': { post: { summary: '쿠팡 웹훅 트리거' } },
            '/webhooks/receiver': { post: { summary: '테스트용 웹훅 수신' } },
            '/webhooks/received': { get: { summary: '수신된 웹훅 조회' } },
        },
    });
});
exports.default = router;
//# sourceMappingURL=openapi.js.map