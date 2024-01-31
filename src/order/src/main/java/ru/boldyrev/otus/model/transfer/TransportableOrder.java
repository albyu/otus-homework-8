package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.entity.OrderItem;
import ru.boldyrev.otus.model.enums.OrderStatus;

import java.util.HashSet;
import java.util.Set;


@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TransportableOrder {

    private String id;

    private OrderStatus status;

    private Set<TransportableOrderItem> orderItems;

    public TransportableOrder(Order order) {
        this.id = order.getId();
        this.status = order.getStatus();
        this.orderItems = new HashSet<>();
        for (OrderItem item : order.getOrderItems()) {
            TransportableOrderItem transportableOrderItem = new TransportableOrderItem(item);
            this.orderItems.add(transportableOrderItem);
        }
    }
}
