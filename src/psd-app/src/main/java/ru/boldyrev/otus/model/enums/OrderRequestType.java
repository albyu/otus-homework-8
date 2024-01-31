package ru.boldyrev.otus.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderRequestType {
    CONFIRM("Confirm"),
    REVERSE("Reverse");

    private final String name;

    OrderRequestType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
