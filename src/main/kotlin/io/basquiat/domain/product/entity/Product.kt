package io.basquiat.domain.product.entity

import io.basquiat.global.model.entity.BaseTimeEntity
import io.basquiat.global.utils.unableToJoin
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "product")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var name: String,
    var price: Long,
    var quantity: Int,
) : BaseTimeEntity() {
    /**
     * 예약을 하면 수량만큼 선점
     */
    fun reserveProduct(orderQuantity: Int) {
        if (this.quantity < orderQuantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        this.quantity -= orderQuantity
    }

    /**
     * 예약 취소를 하면 선점한 재고 복구
     */
    fun restore(restockCount: Int) {
        this.quantity += restockCount
    }
}