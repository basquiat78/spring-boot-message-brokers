package io.basquiat.domain.product.repository

import io.basquiat.domain.product.entity.Product
import io.basquiat.domain.product.repository.querydsl.CustomProductRepository
import io.basquiat.global.repository.BaseRepository

interface ProductRepository :
    BaseRepository<Product, Long>,
    CustomProductRepository