package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.enums.OrderConfirmStatus;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmRequest;

import javax.persistence.*;

@NoArgsConstructor
@Data
@Accessors(chain = true)
@Entity
@Table(name = "ORDER_CONFIRM_REQUESTS")
public class OrderConfirmRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    OtusSystem systemName;

    @OneToOne
    @JoinColumn(name = "order_id")
    Order order;

    Long orderCaseId;

    @Enumerated(EnumType.STRING)
    OrderRequestType orderRequestType;

    @Enumerated(EnumType.STRING)
    OrderConfirmStatus confirmStatus;


    public OrderConfirmRequest(TransportableOrderConfirmRequest transportableRequest) {
        this.systemName = transportableRequest.getSystemName();
        this.orderCaseId = transportableRequest.getOrderCaseId();
        this.orderRequestType = transportableRequest.getOrderRequestType();
        this.confirmStatus = OrderConfirmStatus.IN_PROGRESS;
        TransportableOrder transportableOrder = transportableRequest.getTransportableOrder();
        this.order = new Order(transportableOrder);
    }
}
