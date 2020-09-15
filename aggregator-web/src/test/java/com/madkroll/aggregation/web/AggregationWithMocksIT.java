package com.madkroll.aggregation.web;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

@RunWith(SpringRunner.class)
@Import(AggregationWithMocksIT.WebMockConfiguration.class)
@SpringBootTest(
        properties = "spring.main.allow-bean-definition-overriding=true",
        classes = AggregationWithMocksIT.WebMockConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class AggregationWithMocksIT {

    private final static String BACKEND_HOST = "localhost";
    private final static int PRICING_PORT = 33333;
    private final static int TRACK_PORT = 33334;
    private final static int SHIPMENTS_PORT = 33335;

    private MockWebServer pricingMock;
    private MockWebServer trackMock;
    private MockWebServer shipmentsMock;

    @Autowired
    private AggregationController controller;

    @Before
    public void setup() throws IOException {
        pricingMock = new MockWebServer();
        trackMock = new MockWebServer();
        shipmentsMock = new MockWebServer();

        pricingMock.start(PRICING_PORT);
        trackMock.start(TRACK_PORT);
        shipmentsMock.start(SHIPMENTS_PORT);
    }

    @After
    public void tearDown() throws Exception {
        pricingMock.shutdown();
        trackMock.shutdown();
        shipmentsMock.shutdown();
    }

    @Test
    public void shouldFetchFromAllProviders() {
        mockResponse(pricingMock, "{\"NL\":14.242090605778}");
        mockResponse(trackMock, "{\"109347263\": \"NEW\"}");
        mockResponse(shipmentsMock, "{\"109347263\": [\"box\", \"box\", \"pallet\"]}");

        sendRequest("NL", "109347263", "109347263")
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.pricing.NL").isEqualTo("14.242090605778")
                .jsonPath("$.track.109347263").isEqualTo("NEW")
                .jsonPath("$.shipments.109347263").isArray()
                .jsonPath("$.shipments.109347263[0]").isEqualTo("box")
                .jsonPath("$.shipments.109347263[1]").isEqualTo("box")
                .jsonPath("$.shipments.109347263[2]").isEqualTo("pallet");
    }

    @Test
    public void shouldReturnDefaultJsonIfNothingRequested() {
        mockResponse(pricingMock, "");
        mockResponse(trackMock, "");
        mockResponse(shipmentsMock, "");

        sendRequest("", "", "")
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .json("{\"pricing\":{},\"track\":{},\"shipments\":{}}");
    }

    @Test
    public void shouldReturnResponseIfOneProviderFailed() {
        mockResponse(pricingMock, "{\"NL\":14.242090605778}");
        mockResponse(trackMock, "{\"109347263\": \"NEW\"}");
        mockResponseExceptionally(shipmentsMock);

        sendRequest("NL", "109347263", "109347263")
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.pricing.NL").isEqualTo(new BigDecimal("14.242090605778"))
                .jsonPath("$.track.109347263").isEqualTo("NEW")
                .jsonPath("$.shipments.109347263").isEmpty();
    }

    @Test
    public void shouldReturnDefaultJsonIfNothingReceived() {
        mockResponse(pricingMock, "");
        mockResponse(trackMock, "");
        mockResponse(shipmentsMock, "");

        sendRequest("NL", "109347263", "109347263")
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .json("{\"pricing\":{},\"track\":{},\"shipments\":{}}");
    }

    @Test
    public void shouldReturnDefaultJsonIfTimeoutOnBackend() {
        sendRequest("NL", "109347263", "109347263")
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .json("{\"pricing\":{},\"track\":{},\"shipments\":{}}");
    }

    private void mockResponse(final MockWebServer provider, final String content) {
        provider.enqueue(
                new MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(content)
        );
    }

    private void mockResponseExceptionally(final MockWebServer provider) {
        provider.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        // and for retries
        provider.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private WebTestClient.ResponseSpec sendRequest(
            final String pricing, final String track, final String shipments
    ) {
        return WebTestClient
                .bindToController(controller)
                .build()
                .mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/aggregation")
                                .queryParam("pricing", pricing)
                                .queryParam("track", track)
                                .queryParam("shipments", shipments)
                                .build())
                .exchange();
    }

    @Profile("test")
    @TestConfiguration
    public static class WebMockConfiguration {

        @Bean
        public WebClient pricingWebClient() {
            return WebClient.builder()
                    .baseUrl(String.format("http://%s:%d/pricing", BACKEND_HOST, PRICING_PORT)).build();
        }

        @Bean
        public WebClient trackWebClient() {
            return WebClient.builder().baseUrl(String.format("http://%s:%d/track", BACKEND_HOST, TRACK_PORT)).build();
        }

        @Bean
        public WebClient shipmentsWebClient() {
            return WebClient.builder().baseUrl(String.format("http://%s:%d/shipments", BACKEND_HOST, SHIPMENTS_PORT)).build();
        }
    }
}
