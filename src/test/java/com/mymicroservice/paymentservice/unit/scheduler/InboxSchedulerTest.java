package com.mymicroservice.paymentservice.unit.scheduler;

import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.service.InboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InboxSchedulerTest {

    @InjectMocks
    private InboxScheduler inboxScheduler;

    @Mock
    private InboxService inboxService;

    @Test
    void processInbox_ShouldDelegateToInboxService_WhenTriggered() {
        inboxScheduler.processInbox();

        verify(inboxService).processPendingInboxEvents();
    }
}
