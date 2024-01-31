package ru.boldyrev.otus.model.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.OrderConfirmRequest;
import ru.boldyrev.otus.model.enums.OrderConfirmStatus;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class TransportableOrderConfirmResponse {

    public TransportableOrderConfirmResponse(OrderConfirmRequest request, OrderConfirmStatus status, String errorMessage){
        this.orderCaseId = request.getOrderCaseId();
        this.orderConfirmStatus = status;
        this.requestType = request.getOrderRequestType();
        this.errorMessage = errorMessage;
        this.systemName = request.getSystemName();
        this.referenceId = String.valueOf(request.getId());
    }

    Long orderCaseId;

    /* Статус подтверждения */
    OrderConfirmStatus orderConfirmStatus;

    /* Тип запроса */
    OrderRequestType requestType;

    /* Текст ошибки */
    String errorMessage;

    /*Имя системы, подвердившей или отвергшей платеж*/
    OtusSystem systemName;

    /* Ссылка на Id во внутренней системе, подтвердившей заказ */
    String referenceId;
}
