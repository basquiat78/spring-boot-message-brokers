package io.basquiat.domain.orders.entity

import io.basquiat.domain.orders.code.OrderStatus
import io.basquiat.domain.product.entity.Product
import io.basquiat.global.model.entity.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "orders")
class Order(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,
    val quantity: Int,
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.COMPLETED,
) : BaseTimeEntity()