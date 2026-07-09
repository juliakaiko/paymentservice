package com.mymicroservice.paymentservice.unit.util;

import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.util.PaymentDataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentDataLoaderTest {

    @InjectMocks
    private PaymentDataLoader paymentDataLoader;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void run_ShouldSaveInitialPayments_WhenApplicationStarts() {
        paymentDataLoader.run();

        verify(paymentRepository, times(3)).save(any());
    }
}
