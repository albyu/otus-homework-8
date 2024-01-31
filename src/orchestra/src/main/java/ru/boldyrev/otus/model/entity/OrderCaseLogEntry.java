package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@Entity
@NoArgsConstructor
@Table(name = "ORDER_CASE_LOG")
public class OrderCaseLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_case_id")
    private OrderCase orderCase;

    private String message;

    private Timestamp timestamp;

    public OrderCaseLogEntry(OrderCase orderCase, String message){
        this.orderCase = orderCase;
        this.message = message;
        this.timestamp = Timestamp.valueOf(LocalDateTime.now());
    }
}
