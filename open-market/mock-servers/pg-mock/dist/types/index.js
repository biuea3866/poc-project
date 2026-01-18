"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PgProvider = exports.PaymentStatus = void 0;
// ==================== Common Types ====================
var PaymentStatus;
(function (PaymentStatus) {
    PaymentStatus["READY"] = "READY";
    PaymentStatus["DONE"] = "DONE";
    PaymentStatus["CANCELED"] = "CANCELED";
    PaymentStatus["PARTIAL_CANCELED"] = "PARTIAL_CANCELED";
    PaymentStatus["FAILED"] = "FAILED";
})(PaymentStatus || (exports.PaymentStatus = PaymentStatus = {}));
var PgProvider;
(function (PgProvider) {
    PgProvider["TOSS_PAYMENTS"] = "TOSS_PAYMENTS";
    PgProvider["KAKAO_PAY"] = "KAKAO_PAY";
    PgProvider["NAVER_PAY"] = "NAVER_PAY";
    PgProvider["DANAL"] = "DANAL";
})(PgProvider || (exports.PgProvider = PgProvider = {}));
//# sourceMappingURL=index.js.map