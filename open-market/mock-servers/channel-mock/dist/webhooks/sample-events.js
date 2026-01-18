"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getSamplePayload = exports.getEventTypes = exports.channelEventSamples = void 0;
exports.channelEventSamples = {
    ST11: {
        'ORDER.CREATED': {
            orderNo: 'ST11-ORD-00001',
            orderDate: new Date().toISOString(),
            buyerName: '홍길동',
            totalAmount: 10000,
        },
        'PAYMENT.COMPLETED': {
            orderNo: 'ST11-ORD-00001',
            paidAt: new Date().toISOString(),
            paymentMethod: 'CARD',
            amount: 10000,
        },
        'CLAIM.REQUESTED': {
            orderNo: 'ST11-ORD-00001',
            claimType: 'CANCEL',
            reason: '고객 요청',
        },
        'SHIPMENT.UPDATED': {
            orderNo: 'ST11-ORD-00001',
            status: 'SHIPPING',
            trackingNumber: '1234567890',
        },
    },
    NAVER_STORE: {
        'ORDER.CREATED': {
            orderId: '2025010412345',
            orderStatusType: 'PAY_COMPLETE',
            totalPaymentAmount: 10000,
        },
        'CLAIM.CREATED': {
            claimId: 'NAVER-CLM-00001',
            claimType: 'RETURN',
            reason: '단순 변심',
        },
        'SHIPMENT.UPDATED': {
            productOrderId: '20250104123451',
            deliveryStatus: 'DELIVERING',
            trackingNumber: '1234567890',
        },
    },
    KAKAO_STORE: {
        'ORDER.CREATED': {
            orderId: 'KAKAO-ORD-00001',
            status: 'PAID',
            totalAmount: 10000,
        },
        'PAYMENT.COMPLETED': {
            orderId: 'KAKAO-ORD-00001',
            paidAt: new Date().toISOString(),
            amount: 10000,
        },
        'SHIPMENT.UPDATED': {
            orderId: 'KAKAO-ORD-00001',
            deliveryStatus: 'DELIVERING',
        },
    },
    TOSS_STORE: {
        'ORDER.CREATED': {
            orderId: 'TOSS-ORD-00001',
            status: 'PAYMENT_COMPLETED',
            totalAmount: 10000,
        },
        'PAYMENT.COMPLETED': {
            orderId: 'TOSS-ORD-00001',
            approvedAt: new Date().toISOString(),
            amount: 10000,
        },
        'SETTLEMENT.COMPLETED': {
            settlementId: 'TOSS-SET-00001',
            settledAt: new Date().toISOString(),
            amount: 10000,
        },
    },
    COUPANG: {
        'ORDER.CREATED': {
            orderId: 1234567890,
            statusName: 'ACCEPT',
            orderedAt: new Date().toISOString(),
        },
        'CLAIM.REQUESTED': {
            claimId: 'CP-CLM-00001',
            claimType: 'CANCEL',
            reason: '고객 요청',
        },
        'SHIPMENT.UPDATED': {
            shipmentBoxId: 9876543210,
            deliveryStatus: 'DELIVERING',
            trackingNumber: '1234567890',
        },
    },
};
const getEventTypes = (channel) => Object.keys(exports.channelEventSamples[channel]);
exports.getEventTypes = getEventTypes;
const getSamplePayload = (channel, eventType) => exports.channelEventSamples[channel][eventType];
exports.getSamplePayload = getSamplePayload;
//# sourceMappingURL=sample-events.js.map