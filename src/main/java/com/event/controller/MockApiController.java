package com.event.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/mock-api")
@Slf4j
public class MockApiController {

    private final Map<String, AtomicInteger> eventScores = new ConcurrentHashMap<>();

    /**
     * Mock external end point to get the latest score.
     * @param eventId current event
     * @return updated event data
     */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<Map<String, String>> getEventData(@PathVariable String eventId) {
        log.info("Mock API received request for event: {}", eventId);

        AtomicInteger score = eventScores.computeIfAbsent(eventId, k -> new AtomicInteger(0));

        Map<String, String> response = Map.of(
                "eventId", eventId,
                "currentScore", String.valueOf(score.getAndIncrement())
        );

        log.info("Mock API returning data for event {}: {}", eventId, response);
        return ResponseEntity.ok(response);
    }
}