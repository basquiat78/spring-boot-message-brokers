package io.basquiat.global.broker.kafka.processor

import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import io.basquiat.global.broker.kafka.schedule.DailyProductDBExporterProcessor
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.state.WindowStore
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DailyProductFetchStreamProcessor {
    @Bean
    fun productFetchCountStream(builder: StreamsBuilder): KStream<String, Long> {
        val stream =
            builder.stream(
                KafkaStreamTopic.PRODUCT_FETCH.topic,
                Consumed.with(Serdes.String(), Serdes.String()),
            )

        val dailyCountStream =
            stream
                .groupByKey()
                .windowedBy(
                    TimeWindows
                        .ofSizeWithNoGrace(Duration.ofDays(1))
                        .advanceBy(Duration.ofDays(1)),
                ).count(
                    Materialized
                        .`as`<String, Long, WindowStore<Bytes, ByteArray>>(
                            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        ).withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()),
                ).toStream()

        dailyCountStream.process(
            ProcessorSupplier { DailyProductDBExporterProcessor() },
            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
        )

        return dailyCountStream.map { windowedKey, value ->
            KeyValue(windowedKey.key(), value)
        }
    }
}