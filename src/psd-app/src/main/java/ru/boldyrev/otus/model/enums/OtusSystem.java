package ru.boldyrev.otus.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum OtusSystem {
    DELIVERY("Delivery"),
    PAYMENT("Payment"),
    STORE("Store");


    private final String name;

    @JsonValue
    public final String getName(){
        return this.name;
    }

    OtusSystem(String name) {
        this.name = name;
    }
}
