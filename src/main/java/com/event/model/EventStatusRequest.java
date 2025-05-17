package com.event.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class EventStatusRequest {
    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "Status is required")
    private boolean status;
}
