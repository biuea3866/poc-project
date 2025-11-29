package com.biuea.concurrency.cinema.domain

class Cinema(
    val movieId: Long,
    val seats: MutableList<Seat> = mutableListOf()
) {
    private val reservations: MutableList<Reservation> = mutableListOf()
    private var reservationIdCounter: Long = 0

    fun getSeat(seatId: Long): Seat? {
        return seats.find { it.id == seatId }
    }

    fun getAvailableSeats(): List<Seat> {
        return seats.filter { it.isAvailable() }
    }

    fun getAvailableSeatsCount(): Int {
        return getAvailableSeats().size
    }

    fun getTotalReservations(): Int {
        return reservations.size
    }

    fun reserveSeat(userId: String, seatId: Long): Reservation {
        val seat = getSeat(seatId)
            ?: throw IllegalArgumentException("좌석을 찾을 수 없습니다: $seatId")

        if (!seat.isAvailable()) {
            throw SeatAlreadyReservedException("좌석 ${seat.row}${seat.number}는 이미 예약되었습니다.")
        }

        seat.reserve(userId)

        val reservation = Reservation(
            id = ++reservationIdCounter,
            userId = userId,
            movieId = movieId,
            seatId = seatId
        )

        reservations.add(reservation)
        return reservation
    }

    fun cancelReservation(reservationId: Long) {
        val reservation = reservations.find { it.id == reservationId }
            ?: throw IllegalArgumentException("예약을 찾을 수 없습니다: $reservationId")

        val seat = getSeat(reservation.seatId)
            ?: throw IllegalArgumentException("좌석을 찾을 수 없습니다: ${reservation.seatId}")

        seat.cancel()
        reservations.remove(reservation)
    }
}
