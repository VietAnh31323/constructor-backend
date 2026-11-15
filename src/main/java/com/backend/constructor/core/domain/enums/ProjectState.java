package com.backend.constructor.core.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectState {
    RECEIVED("Tiếp nhận", 1, true),
    SURVEYING("Khảo sát", 2, true),
    QUOTED("Báo giá", 3, true),
    CONTRACT_SIGNED("Ký HĐ", 4, true),
    IN_CONSTRUCTION("Thi công", 5, true),
    ACCEPTANCE("Nghiệm thu", 6, true),
    HANDOVER("Bàn giao", 7, true),
    WARRANTY("Bảo hành", 8, false),
    CLOSED("Đóng", 9, false),
    PAUSED("Tạm dừng", 10, false),
    CANCELLED("Hủy", 11, false);

    private final String label;
    private final int stepOrder;
    private final boolean activeWorkload;

    public boolean isMainFlow() {
        return stepOrder >= 1 && stepOrder <= 9;
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean isCompletedOrClosed() {
        return this == HANDOVER || this == WARRANTY || this == CLOSED || this == CANCELLED;
    }

    @JsonCreator
    public static ProjectState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "NOT_STARTED" -> RECEIVED;
            case "IN_PROGRESS" -> IN_CONSTRUCTION;
            case "COMPLETED" -> CLOSED;
            default -> ProjectState.valueOf(normalized);
        };
    }
}
