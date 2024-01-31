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
import ru.boldyrev.otus.model.entity.OrderConfirmRequest;
import ru.boldyrev.otus.model.enums.OrderConfirmStatus;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmRequest;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmResponse;
import ru.boldyrev.otus.service.ConfirmService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ConfirmListener {


    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final ConfirmService confirmService;

    @Value("${application.role}")
    private OtusSystem otusSystem;

    @Value("${application.rabbitmq.confirmres.exchangeName}")
    private String resConfirmExchangeName;


    public void sendConfirmRes(TransportableOrderConfirmResponse response) throws JsonProcessingException {
        String responseAsString = null;
        responseAsString = objectMapper.writeValueAsString(response);
        rabbitTemplate.convertAndSend(resConfirmExchangeName, "", responseAsString);
    }


    /*Получение запроса на подтверждение заказа*/
    @RabbitListener(queues = "${application.rabbitmq.confirmreq.queueName}")
    public void receiveMessage(String requestAsString) {
        TransportableOrderConfirmRequest transportableRequest = new TransportableOrderConfirmRequest();

        try {
            transportableRequest = objectMapper.readValue(requestAsString, TransportableOrderConfirmRequest.class);
            log.info("confirmation request for order {} received", transportableRequest.getTransportableOrder().getId());

            //Поищем существующий запрос по этому Order
            OrderConfirmRequest request = confirmService.registerOrderConfirmRequest(transportableRequest);

            /*Если кейс новый*/
            if (request.getConfirmStatus() == OrderConfirmStatus.IN_PROGRESS) {

                //Определяем ответ
                OrderConfirmStatus decision = makeDecision(request.getOrder().getId(), request.getOrderRequestType());

                request.setConfirmStatus(decision);
                confirmService.saveConfirmRequest(request);
            }

            //Формируем ответное сообщение
            TransportableOrderConfirmResponse response = new TransportableOrderConfirmResponse(request, request.getConfirmStatus(), request.getConfirmStatus() == OrderConfirmStatus.REJECTED ? "Rejected randomly" : "");

            //Отправляем ответное сообщение
            sendConfirmRes(response);
            log.info("confirmation response for order {} sent, result {}", request.getOrder().getId(), response.getOrderConfirmStatus().getName());



        } catch (Exception e) {
            log.error("Cannot process " + requestAsString, e);
        }
    }

    private OrderConfirmStatus makeDecision(String orderId, OrderRequestType requestType) {
        if (orderId != null) {
            if (orderId.length() >= 2) {
                if (otusSystem == OtusSystem.PAYMENT) {
                    if (requestType == OrderRequestType.CONFIRM) {
                        if (orderId.charAt(0) == 'p') {
                            return OrderConfirmStatus.REJECTED;
                        }
                    } else if (requestType == OrderRequestType.REVERSE) {
                        if (orderId.charAt(1) == 'p') {
                            return OrderConfirmStatus.REJECTED;
                        }
                    }

                } else if (otusSystem == OtusSystem.STORE) {
                    if (requestType == OrderRequestType.CONFIRM) {
                        if (orderId.charAt(0) == 's') {
                            return OrderConfirmStatus.REJECTED;
                        }

                    } else if (requestType == OrderRequestType.REVERSE) {
                        if (orderId.charAt(1) == 's') {
                            return OrderConfirmStatus.REJECTED;
                        }
                    }

                } else if (otusSystem == OtusSystem.DELIVERY) {
                    if (requestType == OrderRequestType.CONFIRM) {
                        if (orderId.charAt(0) == 'd') {
                            return OrderConfirmStatus.REJECTED;
                        }

                    } else if (requestType == OrderRequestType.REVERSE) {
                        if (orderId.charAt(1) == 'd') {
                            return OrderConfirmStatus.REJECTED;
                        }
                    }
                }
            }
        }
        return OrderConfirmStatus.CONFIRMED;
    }

}
