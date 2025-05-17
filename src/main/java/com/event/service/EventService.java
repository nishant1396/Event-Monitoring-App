package com.event.service;

import com.event.model.Event;

import java.util.Map;

public interface EventService {
    void updateEventStatus(String eventId, boolean isLive);

    Map<String, Event> getAllEvents();
}
