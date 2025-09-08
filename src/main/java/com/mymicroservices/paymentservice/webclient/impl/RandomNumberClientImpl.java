package com.mymicroservices.paymentservice.webclient.impl;

import com.mymicroservices.paymentservice.webclient.RandomNumberClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class RandomNumberClientImpl implements RandomNumberClient {

    private final WebClient webClient;

    public RandomNumberClientImpl(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://www.randomnumberapi.com/api/v1.0/random?min=1&max=100&count=1").build();
    }

    public int generateRandNum() {
        try {
            Integer[] arr = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("min", 1)
                            .queryParam("max", 100)
                            .queryParam("count", 1)
                            .build())
                    .retrieve() //send a request
                    .bodyToMono(Integer[].class)
                    .block(); //a terminal operation that subscribes to Mono and waits for the result synchronously.
            return (arr != null && arr.length > 0) ? arr[0] : fallbackRandom();
        } catch (Exception ex) {
            log.warn("Random API failed, use fallbackRandom()", ex);
            return fallbackRandom();
        }
    }

    private int fallbackRandom() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }
}
