package ru.boldyrev.otus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.listener.ConfirmListener;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.entity.OrderConfirmRequest;
import ru.boldyrev.otus.model.transfer.TransportableOrderConfirmRequest;
import ru.boldyrev.otus.repo.OrderConfirmRequestRepo;
import ru.boldyrev.otus.repo.OrderRepo;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
public class ConfirmService {
    private final OrderRepo orderRepo;
    private final ConfirmListener confirmListener;
    private final OrderConfirmRequestRepo requestRepo;

    @Autowired
    @Lazy
    public ConfirmService(OrderRepo orderRepo, ConfirmListener confirmListener, OrderConfirmRequestRepo requestRepo) {
        this.orderRepo = orderRepo;
        this.confirmListener = confirmListener;
        this.requestRepo = requestRepo;
    }

    public Order getOrder(String orderId) throws NotFoundException {
        return orderRepo.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }


    @Transactional
    public void saveConfirmRequest(OrderConfirmRequest request)
    {
        orderRepo.saveAndFlush(request.getOrder());
        requestRepo.saveAndFlush(request);
    }

    @Transactional
    public OrderConfirmRequest registerOrderConfirmRequest(TransportableOrderConfirmRequest transportableRequest){

        /*Сперва проверим, есть ли уже такой запрос*/
        List<OrderConfirmRequest> requests = requestRepo.findByComposedKey(transportableRequest.getOrderCaseId(),
                                                                           transportableRequest.getOrderRequestType(),
                                                                           transportableRequest.getSystemName());
        if (requests != null)
            if (!requests.isEmpty()){
                // Создаем компаратор для сравнения OrderCase по убыванию id
                Comparator<OrderConfirmRequest> idComparator = Comparator.comparingLong(OrderConfirmRequest::getId).reversed();
                // Сортируем список orderCases с использованием компаратора
                requests.sort(idComparator);
                return requests.get(0); //Возвращаем найденный OrderCase
            }
        /*Еще не обрабатывался*/
        OrderConfirmRequest orderConfirmRequest = new OrderConfirmRequest(transportableRequest);
        saveConfirmRequest(orderConfirmRequest);
        return orderConfirmRequest;
    }

    public List<OrderConfirmRequest> getOrderRequests(String orderId)
    {
        return requestRepo.findByOrderId(orderId);
    }






}
