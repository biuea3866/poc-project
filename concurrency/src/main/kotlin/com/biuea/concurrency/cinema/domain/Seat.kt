package com.biuea.concurrency.cinema.domain

data class Seat(
    val id: Long,
    val row: String,
    val number: Int,
    var status: SeatStatus = SeatStatus.AVAILABLE,
    var reservedBy: String? = null
) {
    fun isAvailable(): Boolean = status == SeatStatus.AVAILABLE

    fun reserve(userId: String) {
        if (!isAvailable()) {
            throw SeatAlreadyReservedException("좌석 ${row}${number}는 이미 예약되었습니다.")
        }
        this.status = SeatStatus.RESERVED
        this.reservedBy = userId
    }

    fun cancel() {
        this.status = SeatStatus.AVAILABLE
        this.reservedBy = null
    }
}

enum class SeatStatus {
    AVAILABLE,
    RESERVED
}

class SeatAlreadyReservedException(message: String) : RuntimeException(message)
