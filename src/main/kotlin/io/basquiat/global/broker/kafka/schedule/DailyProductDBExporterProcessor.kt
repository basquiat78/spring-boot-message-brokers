package io.basquiat.global.broker.kafka.schedule

import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import io.basquiat.global.utils.logger
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ContextualProcessor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.WindowStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyProductDBExporterProcessor : ContextualProcessor<Windowed<String>, Long, Void, Void>() {
    private val log = logger<DailyProductDBExporterProcessor>()

    // 정산 여부를 위한 변수 설정
    private var lastExportedDate: LocalDate? = null

    override fun init(context: ProcessorContext<Void, Void>) {
        super.init(context)

//        context().schedule(Duration.ofDays(1), PunctuationType.WALL_CLOCK_TIME) {
        context().schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME) { timestamp ->
            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            // 만일 자정이거나 오전 1시가 아니라면 실행하지 않는다.
            if (now.hour !in 0..1) return@schedule
            // 마지막 정산 날짜가 오늘이다? 그러면 중복 실행 방지를 위해 실행하지 않는다.
            if (lastExportedDate == today) return@schedule

            val store: WindowStore<String, Long> = context().getStateStore(KafkaStreamTopic.PRODUCT_FETCH.aggregationKey)

            val startOfYesterday =
                LocalDate
                    .now()
                    .minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()

            val endOfYesterday =
                LocalDate
                    .now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .minusNanos(1)
                    .toInstant()

            store.fetchAll(startOfYesterday, endOfYesterday).use { iterator ->
                iterator.forEach { entry ->
                    // window의 entry를 통해서 저장된 상품 아이디 정보를 가져온다.
                    val productId = entry.key.key()
                    val viewCount = entry.value
                    log.info("period: $startOfYesterday - $endOfYesterday")
                    log.info("productId: $productId / viewCount: $viewCount")
                    // TODO 집계 테이블에 넣는 로직을 실행하자.
                }
            }
            // 실행이 완료되면 마지막 정산 날짜를 오늘로 세팅한다.
            lastExportedDate = today
            log.info("[Scheduler] 어제 저장된 Window Store 데이터 처리 완료")
        }
    }

    /**
     * 아무것도 안할 것이기 때문에 그냥 패스
     */
    override fun process(record: Record<Windowed<String>, Long>?) {
        return
    }
}