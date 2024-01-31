package ru.boldyrev.otus.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.boldyrev.otus.model.entity.OrderCase;
import ru.boldyrev.otus.model.enums.*;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmResponse;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmRequest;
import ru.boldyrev.otus.service.OrderCaseService;

@Slf4j
@Component
public class ConfirmListener {

    private static final int MAX_TRY_COUNT = 3;

    @Autowired
    @Lazy
    public ConfirmListener(RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper,
                           OrderCaseService orderCaseService,
                           OrderListener orderListener) {

        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderCaseService = orderCaseService;
        this.orderListener = orderListener;
    }

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final OrderCaseService orderCaseService;


    private final OrderListener orderListener;

    @Value("${application.rabbitmq.confirmreq.exchangeName}")
    private String reqConfirmExchange;
    public static final String PAYMENT_REQ_ROUTEKEY = "payment";
    public static final String STORE_REQ_ROUTEKEY = "store";
    public static final String DELIVERY_REQ_ROUTEKEY = "delivery";


    public void sendOrderCaseToConfirm(TransportableOrderConfirmRequest orderCase, String routeKey) throws JsonProcessingException {
        String caseOrderAsString = null;
        caseOrderAsString = objectMapper.writeValueAsString(orderCase);
        rabbitTemplate.convertAndSend(reqConfirmExchange, routeKey, caseOrderAsString);
    }

    @RabbitListener(queues = "${application.rabbitmq.confirmres.queueName}")
    public void getOrderToProcess(String confirmAsString) {
        TransportableOrderConfirmResponse confirmResponse;
        try {
            confirmResponse = objectMapper.readValue(confirmAsString, TransportableOrderConfirmResponse.class);
            /*Найдем orderCase*/
            if (confirmResponse == null) {
                log.error("Error in confirm processing");
                return;
            }

            OrderCase orderCase = orderCaseService.getOrderCaseById(confirmResponse.getOrderCaseId()).orElseThrow();
            //Order order = orderCase.getOrder();

            log.info("Received confirm response for orderId = {} from {}, result {}", orderCase.getOrder().getId(), confirmResponse.getSystemName().getName(), confirmResponse.getOrderConfirmStatus().getName());

            /*разбираем от какой системы пришло*/
            /*От payments*/
            if (confirmResponse.getSystemName() == OtusSystem.PAYMENT) {
                if (confirmResponse.getRequestType() == OrderRequestType.CONFIRM) {
                    /*Разбираем результат выполнения в payment*/
                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        /* Платеж подтвержден, сохраняем статус и отдаем запрос на подтверждение на склад */

                        orderCase.setStatus(OrderCaseStatus.PAYMENT_CONFIRMED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order confirmed by payment, ref_id = " + confirmResponse.getReferenceId()  + ", sent confirm request to store");

                        /* отправить запрос конферма в store */
                        TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                orderCase.getId(),
                                OrderRequestType.CONFIRM,
                                OtusSystem.STORE);
                        sendOrderCaseToConfirm(request, STORE_REQ_ROUTEKEY);

                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        /* Платеж не прошел, сохраняем статус и отдаем ответ FAILED системе заказов */

                        orderCase.getOrder().setStatus(OrderStatus.FAILED);
                        orderCase.setStatus(OrderCaseStatus.FAILED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order rejected by payment, ref_id = " + confirmResponse.getReferenceId() + ", marked as FAILED, sent back to order");

                        /* Отправить результат в orders */
                        orderListener.sendOrderRes(new TransportableOrder(orderCase.getOrder()));


                    }
                } else if (confirmResponse.getRequestType() == OrderRequestType.REVERSE) {
                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        /* Отмена платежа прошла удачно, теперь можно сохранить статус и отдать ответ FAILED системе заказов */

                        orderCase.getOrder().setStatus(OrderStatus.FAILED);
                        orderCase.setStatus(OrderCaseStatus.FAILED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order reversed by payment, ref_id = " + confirmResponse.getReferenceId() + ", marked as FAILED, sent back to order");

                        /* Отправить результат в orders */
                        orderListener.sendOrderRes(new TransportableOrder(orderCase.getOrder()));


                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        /* Отмена платежа отвергнута, еще раз попробовать отменить платеж в payments, инкрементировать try_count */
                        /* оставляем статус OrderCaseStatus.STORE_FAILED */
                        int retryCount = orderCase.getRetryCount();
                        if (retryCount < MAX_TRY_COUNT) {
                            orderCase.setStatus(OrderCaseStatus.STORE_FAILED).setRetryCount(retryCount + 1);
                            orderCaseService.saveOrderCase(orderCase, "Order reversal rejected by payment (retryCount = "+ String.valueOf(retryCount) +"), ref_id = " + confirmResponse.getReferenceId() + ", retried");
                            TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                    orderCase.getId(),
                                    OrderRequestType.REVERSE,
                                    OtusSystem.PAYMENT);
                            sendOrderCaseToConfirm(request, PAYMENT_REQ_ROUTEKEY);

                        } else {
                            orderCase.setStatus(OrderCaseStatus.STORE_FAILED).setRetryCount(retryCount + 1);
                            orderCaseService.saveOrderCase(orderCase, "Order reversal rejected by payment (retryCount = "+ String.valueOf(retryCount) +"), ref_id = " + confirmResponse.getReferenceId() + ", retry limit reached");
                        }
                    }
                }

            } /*Разбираем результат выполнения в store*/ else if (confirmResponse.getSystemName() == OtusSystem.STORE) {
                if (confirmResponse.getRequestType() == OrderRequestType.CONFIRM) {
                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        /*store подтвердил заказ, сохраняем статус и продвигаем его дальше в delivery*/

                        orderCase.setStatus(OrderCaseStatus.STORE_CONFIRMED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order confirmed by store, ref_id = " + confirmResponse.getReferenceId() + ", sent confirm request to delivery");

                        /* Отправить запрос конферма в delivery */
                        TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                orderCase.getId(),
                                OrderRequestType.CONFIRM,
                                OtusSystem.DELIVERY);
                        sendOrderCaseToConfirm(request, DELIVERY_REQ_ROUTEKEY);

                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        /*store отверг запрос, сохраняем статус и шлем отмену в payments*/

                        orderCase.setStatus(OrderCaseStatus.STORE_FAILED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order rejected by store, ref_id = " + confirmResponse.getReferenceId() + ", sent reversal to payment");
                        /* Отправить запрос на отмену платежа в payments */
                        TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                orderCase.getId(),
                                OrderRequestType.REVERSE,
                                OtusSystem.PAYMENT);
                        sendOrderCaseToConfirm(request, PAYMENT_REQ_ROUTEKEY);

                    }
                } else if (confirmResponse.getRequestType() == OrderRequestType.REVERSE) {
                    /* Удачно выполнена отмена в store - сохраняем статус и отменяем в payment*/

                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        orderCase.setStatus(OrderCaseStatus.STORE_FAILED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order reversed by store, ref_id = " + confirmResponse.getReferenceId() + ", sent reversal to payment");
                        /* Отправить запрос на отмену платежа в payments */
                        TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                orderCase.getId(),
                                OrderRequestType.REVERSE,
                                OtusSystem.PAYMENT);
                        sendOrderCaseToConfirm(request, PAYMENT_REQ_ROUTEKEY);


                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        //orderCase.setStatus(OrderCaseStatus.DELIVERY_FAILED);
                        /*Отмена резервирования store неудачна - еще раз попробовать отменить резервирование в store,
                         * инкрементировать try_count */
                        int retryCount = orderCase.getRetryCount();
                        if (retryCount < MAX_TRY_COUNT) {
                            orderCase.setStatus(OrderCaseStatus.DELIVERY_FAILED).setRetryCount(retryCount + 1);
                            orderCaseService.saveOrderCase(orderCase, "Order reversal rejected by store (retryCount = "+ String.valueOf(retryCount) +"), ref_id = " + confirmResponse.getReferenceId() + ", retried");
                            TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                    orderCase.getId(),
                                    OrderRequestType.REVERSE,
                                    OtusSystem.STORE);
                            sendOrderCaseToConfirm(request, STORE_REQ_ROUTEKEY);
                        }
                        else{
                            orderCase.setStatus(OrderCaseStatus.DELIVERY_FAILED).setRetryCount(retryCount + 1);
                            orderCaseService.saveOrderCase(orderCase, "Order reversal rejected by store (retryCount = "+ String.valueOf(retryCount) +"), ref_id = " + confirmResponse.getReferenceId() + ", retry limit reached");
                        }
                    }
                }
            } /*Разбираем результат выполнения в delivery*/ else if (confirmResponse.getSystemName() == OtusSystem.DELIVERY) {
                if (confirmResponse.getRequestType() == OrderRequestType.CONFIRM) {
                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        // Доставка подтверждена - заказ оформлен. Изменяем статус заявки и заказа, отправляем его в
                        // orders
                        orderCase.getOrder().setStatus(OrderStatus.COMPLETED);
                        orderCase.setStatus(OrderCaseStatus.COMPLETED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order confirmed by delivery, ref_id = " + confirmResponse.getReferenceId() + ", marked as COMPLETED, sent back to order");

                        /* Сохранить и отправить результат в orders */
                        orderListener.sendOrderRes(new TransportableOrder(orderCase.getOrder()));

                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        //Подтверждение заявки отвергнуто - сохраняем статус, отменяем резервирование на складе

                        orderCase.setStatus(OrderCaseStatus.DELIVERY_FAILED).setRetryCount(0);
                        orderCaseService.saveOrderCase(orderCase, "Order rejected by delivery, ref_id = " + confirmResponse.getReferenceId() + ", sent reversal to store");
                        /* Отправить отмену резервирования в store */
                        TransportableOrderConfirmRequest request = new TransportableOrderConfirmRequest(orderCase.getOrder(),
                                orderCase.getId(),
                                OrderRequestType.REVERSE,
                                OtusSystem.STORE);
                        sendOrderCaseToConfirm(request, STORE_REQ_ROUTEKEY);
                    }
                } else if (confirmResponse.getRequestType() == OrderRequestType.REVERSE) {
                    if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.CONFIRMED) {
                        //Отмена доставки подтверждена - сохраняем статус, отменяем store
                        //(невозможный кейс, будет актуален, если за доставкой появится еще одна система

                    } else if (confirmResponse.getOrderConfirmStatus() == OrderConfirmStatus.REJECTED) {
                        /*Отмена резервирования delivery неудачна - еще раз попробовать отменить резервирование в delivery,
                         * инкрементировать try_count */
                        //(невозможный кейс, будет актуален, если за доставкой появится еще одна система)
                    }
                }
            }


        } catch (Exception e) {
            log.error("Error in confirm processing", e);
        }

    }


}
