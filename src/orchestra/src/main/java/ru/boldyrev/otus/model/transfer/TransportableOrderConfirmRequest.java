package ru.boldyrev.otus.model.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;
import ru.boldyrev.otus.model.transfer.TransportableOrder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TransportableOrderConfirmRequest {
    TransportableOrder transportableOrder;

    Long orderCaseId;

    OrderRequestType orderRequestType;

    OtusSystem systemName;

    public TransportableOrderConfirmRequest(Order order, Long id, OrderRequestType orderRequestType, OtusSystem otusSystem) {
        this.transportableOrder = new TransportableOrder(order);
        this.orderCaseId = id;
        this.orderRequestType = orderRequestType;
        this.systemName = otusSystem;
    }
}
