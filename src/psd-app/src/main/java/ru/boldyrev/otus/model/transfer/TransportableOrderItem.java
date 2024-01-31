package ru.boldyrev.otus.model.transfer;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.OrderItem;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class TransportableOrderItem {

    @JsonProperty("externalId")
    private Long id;

    private int quantity;

    private Long productId;

    private String productName;

    private double productPrice;

    public TransportableOrderItem(OrderItem orderItem){
        this.id = orderItem.getId();
        this.quantity = orderItem.getQuantity();
        this.productId = orderItem.getId();
        this.productName = orderItem.getProductName();
        this.productPrice = orderItem.getProductPrice();
    }
}
