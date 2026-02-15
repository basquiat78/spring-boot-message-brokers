package io.basquiat.global.broker.kafka.channel

enum class KafkaStreamTopic(
    val topic: String,
    val aggregationKey: String,
) {
    PRODUCT_FETCH("basquiat.product.fetch", "search-join-store"),

//    ORDER_CREATE("basquiat.order.create", "accumulation-join-store"),
//
//    RESERVATION_CREATE("basquiat.reservation.create", "pending-reservation-join-store"),
//    RESERVATION_CONFIRM("basquiat.reservation.confirm", "confirmed-join-store"),
//    RESERVATION_CANCEL("basquiat.reservation.cancel", "cancel-join-store"),
    ;

    companion object {
        val allTopic: List<String>
            get() = entries.map { it.topic }

        fun findByName(name: String) = entries.find { it.topic == name }
    }
}