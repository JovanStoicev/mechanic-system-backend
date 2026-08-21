package com.garage.system.part;

import java.time.OffsetDateTime;

public record PartRequestResponse(Long id, Long mechanicId, String mechanicName, Long partId,
                                  String partName, int quantity, String note, String status,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static PartRequestResponse from(PartRequest request) {
        return new PartRequestResponse(request.getId(), request.getMechanic().getId(),
                request.getMechanic().getUser().getName(), request.getPart().getId(),
                request.getPart().getName(), request.getQuantity(), request.getNote(),
                request.getStatus().name(), request.getCreatedAt(), request.getUpdatedAt());
    }
}
