package io.basquiat.global.broker.kafka.processor

import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore
import org.springframework.context.annotation.Bean

// @Component
class ProductFetchStreamProcessor {
    @Bean
    fun productFetchCountStream(builder: StreamsBuilder): KStream<String, Long> {
        val stream =
            builder.stream(
                KafkaStreamTopic.PRODUCT_FETCH.topic,
                Consumed.with(Serdes.String(), Serdes.String()),
            )

        val countBuilder =
            stream
                .groupByKey()
                .count(
                    Materialized
                        .`as`<String, Long, KeyValueStore<Bytes, ByteArray>>(
                            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        ).withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()),
                )

        return countBuilder.toStream()
    }

//    @Bean
//    fun productFetchCountStreamBySink(builder: StreamsBuilder): KStream<String, Long> {
//        val stream = builder.stream(
//            KafkaStreamTopic.PRODUCT_FETCH.topic,
//            Consumed.with(Serdes.String(), Serdes.String()),
//        )
//
//        val countTable = stream
//            .groupByKey()
//            .count(
//                Materialized.`as`<String, Long, KeyValueStore<Bytes, ByteArray>>(
//                    KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
//                ).withKeySerde(Serdes.String())
//                    .withValueSerde(Serdes.Long()),
//            )
//
//        val resultStream = countTable.toStream()
//        resultStream.to(
//            "basquiat.product.view.count",
//            Produced.with(Serdes.String(), Serdes.Long())
//        )
//        return resultStream
//    }
}