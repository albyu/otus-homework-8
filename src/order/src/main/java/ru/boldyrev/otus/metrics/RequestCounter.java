package ru.boldyrev.otus.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestCounter {
    private final Counter requestCounter;

    @Autowired
    public RequestCounter(MeterRegistry registry) {
        this.requestCounter = Counter.builder("requests_counter")
                .description("Total number of successful requests")
                .register(registry);
    }

    public void increment() {
        requestCounter.increment();
    }
}