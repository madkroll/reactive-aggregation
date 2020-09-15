package com.madkroll.aggregation.web;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@RunWith(SpringRunner.class)
@Import(AggregationWithBackendIT.AggregationTestConfiguration.class)
@SpringBootTest(
        properties = "spring.main.allow-bean-definition-overriding=true",
        classes = AggregationWithBackendIT.AggregationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class AggregationWithBackendIT {

    @ClassRule
    public static GenericContainer backendContainer =
            new GenericContainer("xyzassessment/backend-services:latest")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/pricing?q=1"));

    @Autowired
    private AggregationController controller;

    @Test
    public void shouldSucceed() {
        WebTestClient
                .bindToController(controller)
                .build()
                .mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build()
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/aggregation")
                                .queryParam("pricing", "NL, FR, US")
                                .queryParam("track", "1, 2, 3")
                                .queryParam("shipments", "1, 2, 3")
                                .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.pricing").isNotEmpty()
                .jsonPath("$.track").isNotEmpty()
                .jsonPath("$.shipments").isNotEmpty();
    }

    @Profile("test")
    @TestConfiguration
    public static class AggregationTestConfiguration {

        @Bean
        public WebClient pricingWebClient() {
            return WebClient.builder()
                    .baseUrl(String.format("http://%s:%d/pricing",backendContainer.getHost(), backendContainer.getMappedPort(8080))).build();
        }

        @Bean
        public WebClient trackWebClient() {
            return WebClient.builder().baseUrl(String.format("http://%s:%d/track",backendContainer.getHost(), backendContainer.getMappedPort(8080))).build();
        }

        @Bean
        public WebClient shipmentsWebClient() {
            return WebClient.builder().baseUrl(String.format("http://%s:%d/shipments",backendContainer.getHost(), backendContainer.getMappedPort(8080))).build();
        }
    }
}