-- 상품 테이블
CREATE TABLE "basquiat".product (
     id bigserial NOT NULL,
     name VARCHAR(255) NOT NULL,
     price INT4 NOT NULL DEFAULT 0,
     quantity INT4 NOT NULL DEFAULT 0,
     created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
     updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
     CONSTRAINT product_pkey PRIMARY KEY (id)
);

-- 주문 테이블
CREATE TABLE "basquiat".orders (
    id bigserial NOT NULL,
    product_id INT8 NOT NULL,
    quantity INT4 NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT orders_product_pkey PRIMARY KEY (id),
    CONSTRAINT orders_product_id_fkey FOREIGN KEY (product_id) REFERENCES "basquiat".product(id)
);

-- 예약 테이블
CREATE TABLE "basquiat".reservation (
     id bigserial NOT NULL,
     product_id INT8 NOT NULL,
     reserved_at TIMESTAMPTZ(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
     updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
     CONSTRAINT reservation_pkey PRIMARY KEY (id),
     CONSTRAINT reservation_product_id_fkey FOREIGN KEY (product_id) REFERENCES "basquiat".product(id)
);

CREATE INDEX idx_orders_product_id ON "basquiat".orders(product_id);
CREATE INDEX idx_reservation_product_id ON "basquiat".reservation(product_id);


INSERT INTO "basquiat".product (name, price, quantity) VALUES
('[품절주의] 몽키 D. 루피 고무고무 열매', 5656000, 1),
('롤로노아 조로 명검 슈스이', 3330000, 3),
('나미 제우스 기상봉', 730000, 73),
('갓 우솝 저격왕 새총', 80000, 100 ),
('상디 올블루 특제 도시락', 32000, 50 ),
('토니토니 쵸파 럼블볼 팩', 1000, 1000 ),
('니코 로빈 포네그리프 사본', 870000, 20 ),
('프랑키 콜라 충전식 엔진', 44000, 44 ),
('브룩 소울 킹 콘서트 티켓', 90000, 35 ),
('징베 어인공수도 도복', 460000, 10 );