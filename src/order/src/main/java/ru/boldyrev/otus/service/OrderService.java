package ru.boldyrev.otus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.listener.OrderListener;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.enums.OrderStatus;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.repo.OrderRepo;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepo orderRepo;
    private final OrderListener orderListener;

    @Autowired
    @Lazy
    public OrderService(OrderRepo orderRepo, OrderListener orderListener) {
        this.orderRepo = orderRepo;
        this.orderListener = orderListener;
    }

    public Order get(String orderId) throws NotFoundException {
        return orderRepo.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    /* Размещение нового заказа */
    @Transactional
    public Order place(Order order) throws ConflictException {

        /* Проверим наличие orderId */
        if (order.getId() == null) {
            throw new ConflictException("Null orderId");
        }

        /* Поищем существующий заказ */
        Optional<Order> existingOrder = orderRepo.findById(order.getId());

        /* Добавляем новый заказ */
        if (existingOrder.isEmpty()) {
            //Размещаем новый заказ со статусом NEW и версией 0
            order.setVersion(0L);
            order.setStatus(OrderStatus.NEW);
            order = orderRepo.saveAndFlush(order);
            return order;
        }

        /* Заказ уже существует, вернем его состояние as is */
        else {
            return existingOrder.get();
        }
    }

    /* Изменение существующего заказа */
    @Transactional
    public Order adjust(Order order) throws ConflictException, NotFoundException {

        /* Проверим наличие orderId */
        if (order.getId() == null) {
            throw new ConflictException("Null orderId");
        }

        /* Поищем существующий заказ, если нет, то выбросим исключение */
        Order existingOrder = orderRepo.findById(order.getId()).orElseThrow(() -> new NotFoundException("Order not found"));

        /* Проверяем статус заказа */
        if (existingOrder.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Order already in progress");
        } else if (existingOrder.getStatus() == OrderStatus.COMPLETED || existingOrder.getStatus() == OrderStatus.FAILED) {
            throw new ConflictException("Order already completed");
        } else if (existingOrder.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Order already canceled");
        } else { /* Order in status NEW */
            /* Корректируем существующий заказ */
            order = orderRepo.saveAndFlush(order);
            return order;
        }
    }

    /* Передача заказа в обработку */
    @Transactional
    public Order startProcessing(Order order) throws ConflictException, NotFoundException {

        /* Проверим наличие orderId */
        if (order.getId() == null) {
            throw new ConflictException("Null orderId");
        }

        /* Поищем существующий заказ, если нет, то выбросим исключение */
        Order existingOrder = orderRepo.findById(order.getId()).orElseThrow(() -> new NotFoundException("Order not found"));

        /* Проверяем статус заказа */
        if (existingOrder.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Already canceled");
        } else if (existingOrder.getStatus() == OrderStatus.NEW) {
            /* Меняем статус и сохраняем */
            order.setStatus(OrderStatus.IN_PROGRESS);
            order = orderRepo.saveAndFlush(order);
            /* Поставим заказ во внутреннюю очередь */
            try {
                TransportableOrder transportableOrder = new TransportableOrder(order);
                orderListener.sendMessageReq(transportableOrder);
            } catch (JsonProcessingException e) {
                order.setStatus(OrderStatus.FAILED);
            }
            /* и вернем его состояние*/
            return order;

        } else /*IN_PROGRESS, COMPLETED*/ {
            /* Только поставим в очередь, в БД ничего не меняем */
            //progressEmulator.addToList(order);

            /* вернем состояние as is */
            return existingOrder;
        }
    }

    /* Отмена заказа*/
    @Transactional
    public Order cancel(Order canceledOrder) throws ConflictException, NotFoundException {

        /* Проверим наличие orderId */
        if (canceledOrder.getId() == null) {
            throw new ConflictException("Null orderId");
        }

        /* Поищем существующий заказ, если нет, то выбросим исключение */
        Order existingOrder = orderRepo.findById(canceledOrder.getId()).orElseThrow(() -> new NotFoundException("Order not found"));

        /* Проверяем статус заказа */
        if (existingOrder.getStatus() == OrderStatus.COMPLETED || existingOrder.getStatus() == OrderStatus.FAILED) {
            throw new ConflictException("Already completed");
        } else if (existingOrder.getStatus() == OrderStatus.NEW || existingOrder.getStatus() == OrderStatus.IN_PROGRESS) {
            /* Меняем статус и сохраняем */
            canceledOrder.setStatus(OrderStatus.CANCELED);
            return orderRepo.saveAndFlush(canceledOrder);
        } else /*OrderStatus.CANCELED*/ {
            /* вернем состояние as is */
            return existingOrder;
        }
    }

    @Transactional
    public void saveOrder(Order order) {
        orderRepo.saveAndFlush(order);
    }




}
