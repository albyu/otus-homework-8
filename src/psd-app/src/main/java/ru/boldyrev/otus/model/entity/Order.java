package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.enums.OrderStatus;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.model.transfer.TransportableOrderItem;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Data
@Accessors(chain = true)
@Entity
@Table(name = "ORDERS")
@NoArgsConstructor
public class Order {
    @Id
    private String id;

    // Добавляем поле status типа OrderStatus
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<OrderItem> orderItems;

    public Order(TransportableOrder transportableOrder){
        this.id = transportableOrder.getId();
        this.status = transportableOrder.getStatus();
        this.orderItems = new HashSet<>();
        for (TransportableOrderItem transportableOrderItem : transportableOrder.getOrderItems()) {
            OrderItem orderItem = new OrderItem(transportableOrderItem);
            this.orderItems.add(orderItem);
        }
    }
}
