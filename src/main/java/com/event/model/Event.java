package com.event.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String eventId;
    private boolean live;
    private String currentScore;
    @Builder.Default
    private Instant lastUpdated = Instant.now();
}