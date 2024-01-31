package ru.boldyrev.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.entity.OrderCase;
import ru.boldyrev.otus.model.entity.OrderCaseLogEntry;
import ru.boldyrev.otus.model.enums.OrderCaseStatus;
import ru.boldyrev.otus.model.transfer.TransportableLogEntry;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.repo.OrderCaseLogEntryRepo;
import ru.boldyrev.otus.repo.OrderCaseRepo;
import ru.boldyrev.otus.repo.OrderRepo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderCaseService {
    private final OrderCaseRepo orderCaseRepo;
    private final OrderCaseLogEntryRepo logEntryRepo;
    private final OrderRepo orderRepo;

    public Order getOrderById(String orderId) throws NotFoundException {
        return orderRepo.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    public List<OrderCase> getOrderCasesByOrderId(String orderId) throws NotFoundException {
        List<OrderCase> orderCases = orderCaseRepo.findByOrderId(orderId);
        if (orderCases != null)
            if (!orderCases.isEmpty())
              return orderCases;

        throw new NotFoundException("Order case not found");
    }

    public Optional<OrderCase> getOrderCaseById(Long orderCaseId)
    {
        return orderCaseRepo.findById(orderCaseId);
    }


    @Transactional
    public void saveOrderCase(OrderCase orderCase, String message){
        OrderCaseLogEntry entry = new OrderCaseLogEntry(orderCase, message);
        logEntryRepo.saveAndFlush(entry);
        orderRepo.saveAndFlush(orderCase.getOrder());
        orderCaseRepo.saveAndFlush(orderCase);
    }

    @Transactional/* Обработка новой заявки на обработку*/
    public OrderCase registerOrder(TransportableOrder transportableOrder) {
        /*Сперва проверим, есть ли уже такой заказ*/
        List<OrderCase> orderCases = orderCaseRepo.findByOrderId(transportableOrder.getId());

        if(orderCases != null)
            if(!orderCases.isEmpty()){
                // Создаем компаратор для сравнения OrderCase по убыванию id
                Comparator<OrderCase> idComparator = Comparator.comparingLong(OrderCase::getId).reversed();
                // Сортируем список orderCases с использованием компаратора
                orderCases.sort(idComparator);
                return orderCases.get(0); //Возвращаем найденный OrderCase
            }

        /* Заказ еще не обрабатывался, создадим кейс здесь*/
        Order order = new Order(transportableOrder);
        orderRepo.saveAndFlush(order);

        /*Теперь создаем кейс*/
        OrderCase orderCase = new OrderCase().setOrder(order).setStatus(OrderCaseStatus.NEW);

        orderCaseRepo.saveAndFlush(orderCase);

        return orderCase;

    }


    public List<TransportableLogEntry> getOrderLogsByOrderId(String orderId) throws NotFoundException {
        List<OrderCase> orderCases = orderCaseRepo.findByOrderId(orderId);
        if(orderCases != null)
            if(!orderCases.isEmpty()){
                // Создаем компаратор для сравнения OrderCase по убыванию id
                Comparator<OrderCase> orderCaseComparator = Comparator.comparingLong(OrderCase::getId).reversed();
                // Сортируем список orderCases с использованием компаратора
                orderCases.sort(orderCaseComparator);
                OrderCase orderCase = orderCases.get(0); //Возвращаем найденный OrderCase

                List<OrderCaseLogEntry> entries = logEntryRepo.findByOrderCaseId(orderCase.getId());
                List<TransportableLogEntry> transportableEntries = logEntryRepo.findByOrderCaseId(orderCase.getId()).stream()
                        .sorted(Comparator.comparing(OrderCaseLogEntry::getId))
                        .map(TransportableLogEntry::new)
                        .toList();

                return transportableEntries;
            }

        throw new NotFoundException("Order not found");

    }
}
