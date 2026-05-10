package com.mymicroservice.paymentservice.service;

import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import org.mymicroservices.common.events.OrderEventDto;

import java.util.UUID;

public interface InboxService {

    void process(InboxEvent inboxEvent, OrderEventDto event, String idempotenceId);

    void updateStatus(InboxEventStatus status, String idempotenceId);

}
