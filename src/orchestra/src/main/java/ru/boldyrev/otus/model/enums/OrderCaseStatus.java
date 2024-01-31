package ru.boldyrev.otus.model.enums;


import lombok.Getter;

@Getter
public enum OrderCaseStatus {
    NEW("New"),
    STARTED("Started"),
    PAYMENT_CONFIRMED("Payment confirmed"),
    STORE_CONFIRMED("Store confirmed"),
    COMPLETED("Completed"),
    DELIVERY_FAILED("Delivery failed"),
    STORE_FAILED("Store failed"),
    FAILED("Failed");

    private final String name;

    OrderCaseStatus(String name) {
        this.name = name;
    }

}
