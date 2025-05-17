package com.event.publisher;

import com.event.model.Event;

public interface MessagePublisher {

    void publishEventUpdate(Event event);
}