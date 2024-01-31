package ru.boldyrev.otus.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.enums.OrderStatus;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.service.OrderService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class OrderListener {


    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final OrderService orderService;

    @Value("${application.rabbitmq.orderreq.exchangeName}")
    private String reqOrderExchangeName;


    public void sendMessageReq(TransportableOrder order) throws JsonProcessingException {
        String orderAsString = null;
        orderAsString = objectMapper.writeValueAsString(order);
        rabbitTemplate.convertAndSend(reqOrderExchangeName, "", orderAsString);
    }


    /*Получение итога обработки заказа*/
    @RabbitListener(queues = "${application.rabbitmq.orderres.queueName}")
    public void receiveMessage(String orderAsString) {
        TransportableOrder transportableOrder = new TransportableOrder();

        try {
            transportableOrder = objectMapper.readValue(orderAsString, TransportableOrder.class);
            log.info("Received processed order: {}, result {}", transportableOrder.getId(), transportableOrder.getStatus());

            /* Ищем заказ по ID */
            Order order = orderService.get(transportableOrder.getId());
            if (order.getStatus() == OrderStatus.IN_PROGRESS) {
                order.setStatus(transportableOrder.getStatus());
                orderService.saveOrder(order);
            }

        } catch (Exception e) {
            log.error("Cannot process received processed order {}", orderAsString, e);
            System.out.println("Cannot process received processed order: " + orderAsString);

        }
    }

}
