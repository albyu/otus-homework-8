package ru.boldyrev.otus.model.enums;

import lombok.Getter;

public enum OrderConfirmStatus {
    CONFIRMED("Confirmed"),
    REJECTED("Rejected"),
    IN_PROGRESS("In progress");

    @Getter
    private final String name;

    OrderConfirmStatus(String name) {
        this.name = name;
    }
}
