package ru.boldyrev.otus.model.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.transfer.TransportableOrderItem;

import javax.persistence.*;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "ORDER_ITEMS")
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalId;

    private int quantity;

    private Long productId;

    private String productName;

    private double productPrice;

    public OrderItem(TransportableOrderItem transportableOrderItem) {
        this.externalId = transportableOrderItem.getId();
        this.quantity = transportableOrderItem.getQuantity();
        this.productId = transportableOrderItem.getProductId();
        this.productName = transportableOrderItem.getProductName();
        this.productPrice = transportableOrderItem.getProductPrice();
    }
}
