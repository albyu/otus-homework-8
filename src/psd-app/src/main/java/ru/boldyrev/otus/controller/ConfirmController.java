package ru.boldyrev.otus.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.entity.OrderConfirmRequest;
import ru.boldyrev.otus.service.ConfirmService;

import javax.persistence.EntityExistsException;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Api(tags = "Обработка заказа")
public class ConfirmController {

    private final ConfirmService orderConfirmRequest;

    @Autowired
    public ConfirmController(ConfirmService orderConfirmRequest) {
        this.orderConfirmRequest = orderConfirmRequest;
    }


    /*Получить текущий статус заказа*/
    @ApiOperation(value = "Получить текущее состояние заказа")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found")
    })
    @GetMapping(value = "/order/get", produces = "application/json")
    public ResponseEntity<Order> orderGet(@RequestParam(name = "orderId") String orderId) throws NotFoundException {
        Order order = orderConfirmRequest.getOrder(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(order);
    }

    @ApiOperation(value = "Получить текущее состояние заказа")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found")
    })
    @GetMapping(value = "/confirm/get", produces = "application/json")
    public ResponseEntity<List<OrderConfirmRequest>> requestsGet(@RequestParam(name = "orderId") String orderId) throws NotFoundException {
        List<OrderConfirmRequest> requests = orderConfirmRequest.getOrderRequests(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(requests);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> orderExceptionProcessing(ConflictException conflictException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", conflictException.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> orderExceptionProcessing(NotFoundException conflictException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorReason", conflictException.getMessage()));
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<Map<String, String>> orderExceptionProcessing(EntityExistsException orderException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", "Cannot change object, try to refresh with /order/get"));
    }

    /*Перехватываем тут, потому что при перехвате внутри @Transactional прокси-класс кидает свое исключение и все равно перехватывать тут еще раз*/
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> orderExceptionProcessing(OptimisticLockingFailureException orderException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", "Object was changed by another transaction, try to refresh with /order/get"));
    }


}
