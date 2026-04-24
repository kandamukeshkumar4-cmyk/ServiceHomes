package com.servicehomes.api.wishlists.application.dto;

import java.util.List;
import java.util.UUID;

public record UpdateCollaboratorsRequest(List<UUID> collaboratorIds) {}
