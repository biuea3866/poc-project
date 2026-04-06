rootProject.name = "closet-ecommerce"

include(
    // Shared Kernel
    "closet-common",

    // Infrastructure
    "closet-gateway",

    // Core Domain Services (Phase 1-2)
    "closet-member",
    "closet-product",
    "closet-order",
    "closet-payment",
    "closet-inventory",
    "closet-search",
    "closet-review",

    // Phase 2 → Phase 3 확장 (shipping + CS 병합)
    "closet-fulfillment",     // 배송 + 반품/교환 접수 + CS(문의/FAQ)

    // Phase 3 Domain Services
    "closet-promotion",       // 쿠폰 + 할인 + 타임세일 + 적립금
    "closet-display",         // 배너 + 기획전 + 랭킹 + 매거진 + OOTD
    "closet-notification",    // SMS/푸시/이메일 알림 인프라

    // Orchestration
    "closet-bff",

    // External Mock
    "closet-external-api",
)
