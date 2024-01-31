package ru.boldyrev.otus.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.boldyrev.otus.model.entity.OrderCase;
import ru.boldyrev.otus.model.enums.OrderCaseStatus;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmRequest;
import ru.boldyrev.otus.service.OrderCaseService;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@Component
public class OrderListener {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OrderCaseService orderCaseService;
    private final ConfirmListener confirmListener;



    @Value("${application.rabbitmq.orderres.exchangeName}")
    private String resOrderExchangeName;

    /* Отправка обработанного заказа в order-app */
    public void sendOrderRes(TransportableOrder transportableOrder) {
        try {
            String orderAsString = null;
            orderAsString = objectMapper.writeValueAsString(transportableOrder);
            rabbitTemplate.convertAndSend(resOrderExchangeName, "", orderAsString);
            log.info("Sent response {} for order {}", transportableOrder.getStatus().getName(), transportableOrder.getId());
        } catch (Exception e) {
            log.error("Cannot send response for order " + transportableOrder.getId(), e);
        }
    }
    /* Обработка входящего сообщения от order-app */
    @RabbitListener(queues = "${application.rabbitmq.orderreq.queueName}")
    public void getOrderToProcess(String orderAsString) {
        TransportableOrder transportableOrder;

        try {
            transportableOrder = objectMapper.readValue(orderAsString, TransportableOrder.class);
            log.info("Received order {} for processing", transportableOrder.getId());

            //Получаем или заводим новый кейс
            OrderCase orderCase = orderCaseService.registerOrder(transportableOrder);
            orderCaseService.saveOrderCase(orderCase, "OrderCase created");

            /*Проверим статус кейса*/
            if (orderCase.getStatus() == OrderCaseStatus.NEW) {
                orderCase.setStatus(OrderCaseStatus.STARTED);
                orderCaseService.saveOrderCase(orderCase, "Payment confirmation requested");
                /* Создаем запрос для кейса - прикрепляем заказ, id кейса, тип запроса (CONFIRM) */
                TransportableOrderConfirmRequest orderConfirmRequest = new TransportableOrderConfirmRequest(orderCase.getOrder(), orderCase.getId(), OrderRequestType.CONFIRM, OtusSystem.PAYMENT);

                /*Посылаем запрос в Payment*/
                confirmListener.sendOrderCaseToConfirm(orderConfirmRequest, ConfirmListener.PAYMENT_REQ_ROUTEKEY);
            }
            /*Если кейс уже готов*/
            else if (orderCase.getStatus() == OrderCaseStatus.COMPLETED || orderCase.getStatus() == OrderCaseStatus.FAILED) {
                TransportableOrder orderToSend = new TransportableOrder(orderCase.getOrder());
                sendOrderRes(orderToSend);
            }



        } catch (Exception e) {
            log.error("Error in order processing", e);
        }

    }


}
