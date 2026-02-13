package io.basquiat.nats.responder.handler

import io.basquiat.domain.orders.code.OrderStatus
import io.basquiat.domain.orders.entity.Order
import io.basquiat.domain.orders.repository.OrderRepository
import io.basquiat.domain.reservation.code.ReservationStatus
import io.basquiat.domain.reservation.entity.Reservation
import io.basquiat.domain.reservation.repository.ReservationRepository
import io.basquiat.global.extensions.findByIdOrThrow
import io.basquiat.global.utils.unableToJoin
import io.basquiat.nats.model.ReservationAction
import io.basquiat.nats.model.ReservationActionResponse
import io.basquiat.nats.model.ReservationActionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationActionHandler(
    private val reservationRepository: ReservationRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun execute(
        request: ReservationAction,
        action: ReservationActionType,
    ): ReservationActionResponse {
        println("request: $request, action: $action")
        val reservationId = request.reservationId
        val reservation =
            reservationRepository.findByIdOrThrow(reservationId, "해적단 합류 예약 목록을 찾을 수 없습니다. 해적단 합류 예약 아이디: $reservationId")

        validateReservationStatus(reservation.status)

        val message =
            when (action) {
                ReservationActionType.CONFIRM -> {
                    val orderId = confirmed(reservation)
                    "해적단 합류 확정되었습니다. 내 동료가 되어라!!! 해적단 합류 아이디: $orderId"
                }

                ReservationActionType.CANCEL -> {
                    cancel(reservation)
                    "실망인데? 내 동료가 될 생각이 없는거냐!!!"
                }
            }
        return ReservationActionResponse(message = message)
    }

    private fun confirmed(reservation: Reservation): Long {
        val orderId = createOrder(reservation)
        reservation.status = ReservationStatus.CONFIRMED
        return orderId
    }

    private fun validateReservationStatus(status: ReservationStatus) {
        if (status == ReservationStatus.CANCELLED) unableToJoin("겁쟁이!!!! 해적단 합류를 이미 취소했잖아!!")
        if (status == ReservationStatus.CONFIRMED) unableToJoin("멋진 녀석이었잖아! 이미 해적단에 합류했어!! 가자고~")
    }

    private fun createOrder(reservation: Reservation): Long {
        val product = reservation.product

        val entity =
            Order(
                product = product,
                quantity = 1,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderRepository.save(entity)
        return completeOrder.id!!
    }

    private fun cancel(reservation: Reservation) {
        val product = reservation.product
        product.restore(1)
        reservation.status = ReservationStatus.CANCELLED
    }
}