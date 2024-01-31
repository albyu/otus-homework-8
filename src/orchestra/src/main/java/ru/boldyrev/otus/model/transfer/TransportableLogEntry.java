package ru.boldyrev.otus.model.transfer;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.OrderCase;
import ru.boldyrev.otus.model.entity.OrderCaseLogEntry;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;




@Data
@Accessors(chain = true)
@NoArgsConstructor
public class TransportableLogEntry {

    private Long id;

    private Long orderCaseId;

    private String message;

    private Timestamp timestamp;

    public TransportableLogEntry(OrderCaseLogEntry orderCaseLogEntry){
        this.id = orderCaseLogEntry.getId();
        this.orderCaseId = orderCaseLogEntry.getOrderCase().getId();
        this.message = orderCaseLogEntry.getMessage();
        this.timestamp = orderCaseLogEntry.getTimestamp();
    }
}