package com.event.model;

import lombok.Data;

@Data
public class ExternalApiResponse {
    private String eventId;
    private String currentScore;
}