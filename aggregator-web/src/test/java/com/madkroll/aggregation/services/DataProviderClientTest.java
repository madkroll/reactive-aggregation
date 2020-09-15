package com.madkroll.aggregation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DataProviderClientTest {

    private static final int BACKEND_TIMEOUT = 2;
    private static final int MAX_RETRIES = 1;

    private static final Map<String, String> MAP_WITH_VALUES =
            Map.of(
                    "first_entry_key", "first_entry_value",
                    "second_entry_key", "second_entry_value"
            );

    private static final Set<String> QUERIES = MAP_WITH_VALUES.keySet();

    private MockWebServer mockWebServer;

    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        mockWebServer = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void shouldReturnResponseIfRetrySucceeded() throws Exception {
        // given
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(MAP_WITH_VALUES);
        // once returns 503
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.SERVICE_UNAVAILABLE.value()));
        // and then succeeds to request data, 200
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(toJson(MAP_WITH_VALUES))
        );

        // when
        final Map<String, String> response = newClient().retrieveByHttp(QUERIES).block();

        // then
        assertThat(response).containsExactlyInAnyOrderEntriesOf(MAP_WITH_VALUES);
        verify(objectMapper).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    public void shouldFailIfRetriesExceeded() {
        // given
        // twice returns 503
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.SERVICE_UNAVAILABLE.value()));
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.SERVICE_UNAVAILABLE.value()));

        // when, then
        assertThatThrownBy(() -> newClient().retrieveByHttp(QUERIES).block())
                .hasMessage("Retries exhausted: 1/1");
        verifyNoInteractions(objectMapper);
    }

    @Test
    public void shouldFailIfMalformedResponse() throws JsonProcessingException {
        // given
        given(objectMapper.readValue(eq("nonsense-content"), any(TypeReference.class)))
                .willThrow(JsonProcessingException.class);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody("nonsense-content")
        );

        // when, then
        assertThatThrownBy(() -> newClient().retrieveByHttp(QUERIES).block())
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to parse response. Response is malformed.")
                .hasCauseInstanceOf(JsonProcessingException.class);
        verify(objectMapper).readValue(eq("nonsense-content"), any(TypeReference.class));
    }

    private DataProviderClient<String> newClient() {
        return new DataProviderClient<>(
                BACKEND_TIMEOUT,
                MAX_RETRIES,
                WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build(),
                objectMapper
        );
    }

    private static String toJson(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON");
        }
    }
}