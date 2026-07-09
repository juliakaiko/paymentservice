package com.mymicroservice.paymentservice.scheduler;

import com.mymicroservice.paymentservice.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboxScheduler {

    private final InboxService inboxService;

    @Scheduled(fixedDelayString = "${inbox.scheduler.fixed-delay-ms}")
    public void processInbox() {
        inboxService.processPendingInboxEvents();
    }
}