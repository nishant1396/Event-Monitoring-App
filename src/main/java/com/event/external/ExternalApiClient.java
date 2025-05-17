package com.event.external;

import com.event.model.Event;

public interface ExternalApiClient {
    Event fetchEventData(String eventId);
}
