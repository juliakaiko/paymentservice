package com.mymicroservice.paymentservice.metrics;

import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboxMetrics {

    private final MeterRegistry meterRegistry;
    private final InboxEventRepository inboxEventRepository;

    private Counter processedCounter;
    private Counter failedCounter;
    private Counter deadLettersCounter;
    private Timer processingTimer;

    @PostConstruct
    void registerMeters() {
        processedCounter = Counter.builder("inbox.events.processed")
                .description("Total successfully processed inbox events")
                .register(meterRegistry);

        failedCounter = Counter.builder("inbox.events.failed")
                .description("Total inbox events that failed processing and will be retried")
                .register(meterRegistry);

        deadLettersCounter = Counter.builder("inbox.dead.letters")
                .description("Total inbox events moved to DEAD status")
                .register(meterRegistry);

        processingTimer = Timer.builder("inbox.processing.duration")
                .description("Time spent processing a single inbox event")
                .register(meterRegistry);

        for (InboxEventStatus status : InboxEventStatus.values()) {
            Gauge.builder("inbox.events.pending", inboxEventRepository,
                            repo -> repo.countByStatus(status))
                    .tag("status", status.name())
                    .description("Number of inbox events grouped by status")
                    .register(meterRegistry);
        }
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordProcessingDuration(Timer.Sample sample) {
        sample.stop(processingTimer);
    }

    public void recordProcessed() {
        processedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public void recordDeadLetter() {
        deadLettersCounter.increment();
    }
}
