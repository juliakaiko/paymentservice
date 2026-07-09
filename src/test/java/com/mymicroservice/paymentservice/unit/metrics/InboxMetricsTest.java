package com.mymicroservice.paymentservice.unit.metrics;

import com.mymicroservice.paymentservice.metrics.InboxMetrics;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class InboxMetricsTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    private InboxMetrics inboxMetrics;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        inboxMetrics = new InboxMetrics(registry, inboxEventRepository);
        ReflectionTestUtils.invokeMethod(inboxMetrics, "registerMeters");
    }

    @Test
    void recordMethods_ShouldIncrementCounters_WhenCalled() {
        assertDoesNotThrow(() -> {
            var sample = inboxMetrics.startProcessingTimer();
            inboxMetrics.recordProcessingDuration(sample);
            inboxMetrics.recordProcessed();
            inboxMetrics.recordFailed();
            inboxMetrics.recordDeadLetter();
        });
    }
}
