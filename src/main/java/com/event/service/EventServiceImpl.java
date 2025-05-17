package com.event.service;

import com.event.external.ExternalApiClient;
import com.event.model.Event;
import com.event.publisher.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    @Value("${event.polling.interval}")
    private long pollingIntervalSeconds;

    @Value("${event.initial.delay:1}")
    private long initialDelaySeconds;

    private final TaskScheduler taskScheduler;
    private final ExternalApiClient externalApiClient;
    private final MessagePublisher messagePublisher;

    private final Map<String, Event> events = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Create or update event status
     * @param eventId eventId
     * @param live status
     */
    @Override
    public void updateEventStatus(String eventId, boolean live) {
        Objects.requireNonNull(eventId, "Event ID cannot be null");
        log.info("Updating event status: eventId={}, live={}", eventId, live);

        if (live) {
            startEventTracking(eventId);
        } else {
            stopEventTracking(eventId);
        }
    }

    private void startEventTracking(String eventId) {
        log.info("Starting tracking for event: {}", eventId);

        // Create or Update event status
        events.compute(eventId, (key, existingEvent) -> {
            if (existingEvent == null) {
                return Event.builder().eventId(key).live(true).build();
            } else {
                existingEvent.setLive(true);
                return existingEvent;
            }
        });

        // stop any existing scheduled task
        stopScheduledTask(eventId);

        // create task scheduler and add current event to it
        try {
            log.debug("Scheduling periodic task for event: {}", eventId);

            ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(() -> fetchAndPublishEventUpdate(eventId), Instant.now()
                    .plusSeconds(initialDelaySeconds), Duration.ofSeconds(pollingIntervalSeconds));

            log.info("Successfully scheduled task for event: {}", eventId);
            scheduledTasks.put(eventId, scheduledTask);
        } catch (Exception e) {
            log.error("Error scheduling task for event {}: {}", eventId, e.getMessage(), e);
            throw new EventSchedulingException("Failed to schedule event tracking", e);
        }
    }

    private void stopEventTracking(String eventId) {
        log.info("Stopping tracking for event: {}", eventId);

        events.computeIfPresent(eventId, (key, event) -> {
            event.setLive(false);
            return event;
        });

        stopScheduledTask(eventId);
    }

    /**
     * Cancels and removes a scheduled task for an event.
     */
    private void stopScheduledTask(String eventId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(eventId);
        if (scheduledTask != null) {
            log.info("Cancelling scheduled task for event: {}", eventId);
            boolean cancelled = scheduledTask.cancel(false);
            log.info("Task cancellation result: {}", cancelled);
        }
    }

    /**
     * Call external API, get the latest score and publish kafka message
     * @param eventId current event
     */
    private void fetchAndPublishEventUpdate(String eventId) {
        Event currentEvent = events.get(eventId);
        if (currentEvent == null || !currentEvent.isLive()) {
            log.warn("Attempted to update non-live event: {}", eventId);
            stopScheduledTask(eventId);
            return;
        }

        try {
            log.debug("Fetching update for event: {}", eventId);
            Event eventUpdate = externalApiClient.fetchEventData(eventId);

            if (eventUpdate == null) {
                log.warn("Received null event data from external API for event: {}", eventId);
                return;
            }

            events.computeIfPresent(eventId, (key, existingEvent) -> {
                existingEvent.setCurrentScore(eventUpdate.getCurrentScore());
                existingEvent.setLastUpdated(Instant.now());
                log.info("Updated score for event {}: {}", eventId, eventUpdate.getCurrentScore());
                return existingEvent;
            });

            messagePublisher.publishEventUpdate(eventUpdate);
            log.debug("Published update for event {}", eventId);

        } catch (Exception e) {
            log.error("Error processing update for event {}: {}", eventId, e.getMessage(), e);
            // Don't rethrow to prevent the scheduler from cancelling future executions
        }
    }

    public static class EventSchedulingException extends RuntimeException {
        public EventSchedulingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Created all event API to fetch status of all current events
     * @return Map of event string and event details
     */
    @Override
    public Map<String, Event> getAllEvents() {
        return Collections.unmodifiableMap(events);
    }
}