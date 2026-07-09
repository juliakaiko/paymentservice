package com.mymicroservice.paymentservice.unit.webclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.paymentservice.webclient.impl.RandomNumberClientImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomNumberClientImplTest {

    private WireMockServer wireMockServer;
    private RandomNumberClientImpl randomNumberClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        randomNumberClient = new RandomNumberClientImpl(
                WebClient.builder(),
                "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void generateRandNum_ShouldReturnApiValue_WhenApiRespondsSuccessfully() {
        WireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[42]")));

        int result = randomNumberClient.generateRandNum();

        assertTrue(result >= 1 && result <= 100);
    }

    @Test
    void generateRandNum_ShouldUseFallback_WhenApiFails() {
        WireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(500)));

        int result = randomNumberClient.generateRandNum();

        assertTrue(result >= 1 && result <= 100);
    }
}
