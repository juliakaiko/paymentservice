package com.mymicroservice.paymentservice.webclient.impl;

import com.mymicroservice.paymentservice.webclient.RandomNumberClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class RandomNumberClientImpl implements RandomNumberClient {

    private final WebClient webClient;

    public RandomNumberClientImpl(WebClient.Builder builder,
                                  @Value("${random.number.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build(); // используем параметр, а не поле
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
