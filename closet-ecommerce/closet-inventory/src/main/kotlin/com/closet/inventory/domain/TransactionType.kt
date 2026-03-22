package com.closet.inventory.domain

enum class TransactionType(val description: String) {
    INBOUND("입고"),
    OUTBOUND("출고 (결제 확정)"),
    RESERVE("예약 (주문 시 재고 선점)"),
    RELEASE("예약 해제 (취소/반품)"),
    ADJUST("수동 조정"),
}
