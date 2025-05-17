package com.event.controller;

import com.event.model.Event;
import com.event.model.EventStatusRequest;
import com.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping("/status")
    public ResponseEntity<Void> updateEventStatus(@Valid @RequestBody EventStatusRequest request) {
        log.info("Received status update for event {}: {}", request.getEventId(), request.isStatus());
        eventService.updateEventStatus(request.getEventId(), request.isStatus());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Event>> getAllEvents() {
        log.info("Getting all events");
        return ResponseEntity.ok(eventService.getAllEvents());
    }
}