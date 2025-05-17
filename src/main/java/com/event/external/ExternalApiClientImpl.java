package com.event.external;

import com.event.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiClientImpl implements ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${event.api.mock-endpoint}")
    private String apiMockUrl;

    /**
     * method to call external service
     * @param eventId current event
     * @return event object with current score
     */
    @Override
    public Event fetchEventData(String eventId) {
        log.debug("Calling mock API for event: {}", eventId);

        String url = UriComponentsBuilder.fromUriString(apiMockUrl).buildAndExpand(eventId).toUriString();

        try {
            // calling external api using restTemplate, web flux and circuit breaker can be used here.
            ResponseEntity<Map<String, String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            Map<String, String> response = Optional.ofNullable(responseEntity.getBody())
                    .orElseThrow(() -> new ExternalApiException("Null response from mock API for event: " + eventId));

            String currentScore = Optional.ofNullable(response.get("currentScore"))
                    .orElseThrow(() -> new ExternalApiException("Missing 'currentScore' in response for event: " + eventId));

            return Event.builder().eventId(eventId).live(true).currentScore(currentScore).build();

        } catch (RestClientException e) {
            log.error("Error calling mock API for event {}: {}", eventId, e.getMessage());
            throw new ExternalApiException("Failed to fetch data from mock API: " + e.getMessage(), e);
        }
    }

    public static class ExternalApiException extends RuntimeException {
        public ExternalApiException(String message) {
            super(message);
        }

        public ExternalApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}