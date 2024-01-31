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
import ru.boldyrev.otus.service.OrderService;

import javax.persistence.EntityExistsException;
import java.util.Map;

@RestController
@Slf4j
@Api(tags = "Обработка заказа")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    /*Создать заказ*/
    @ApiOperation(value = "Создание заказа", notes = "Идемпотентный метод, если заказ уже существует, то возвращается в ответе as is")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)")
    })
    @PostMapping(value = "/order/place", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Order> orderPlace(@RequestBody Order order) throws ConflictException {
        Order placedOrder = orderService.place(order);
        return ResponseEntity.status(HttpStatus.OK).body(placedOrder);
    }

    /*Изменить заказ*/
    @ApiOperation(value = "Изменение заказа", notes = "Применимо для заказов в статусе NEW, использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)")
    })
    @PutMapping(value = "/order/adjust", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Order> orderAdjust(@RequestBody Order order) throws ConflictException, NotFoundException {
        Order placedOrder = orderService.adjust(order);
        return ResponseEntity.status(HttpStatus.OK).body(placedOrder);
    }

    /*Начать обработку заказа*/
    @ApiOperation(value = "Передать заказ в обработку", notes = "Применимо для заказов в статусе NEW, для заказов в статусе IN_PROGRESS, COMPLETED просто возвращает состояние заказа as is. Для заказа в статусе CANCELED возвращает ошибку. Использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)")
    })
    @PutMapping(value = "/order/process", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Order> orderProcess(@RequestBody Order order) throws ConflictException, NotFoundException {
        Order changedOrder = orderService.startProcessing(order);
        return ResponseEntity.status(HttpStatus.OK).body(changedOrder);
    }

    /*Отменить заказ*/
    @ApiOperation(value = "Отменить заказ", notes = "Применимо для заказов в статусе NEW, IN_PROGRESS. Для заказа в статусе CANCELED просто возвращает состояние. Для заказа в статусе CANCELED возвращает ошибку. Использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)")
    })
    @PutMapping(value = "/order/cancel", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Order> orderCancel(@RequestBody Order order) throws ConflictException, NotFoundException {
        Order canceledOrder = orderService.cancel(order);
        return ResponseEntity.status(HttpStatus.OK).body(canceledOrder);
    }


    /*Получить текущий статус заказа*/
    @ApiOperation(value = "Получить текущее состояние заказа")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 404, message = "Order not found")
    })
    @GetMapping(value = "/order/get", produces = "application/json")
    public ResponseEntity<Order> orderGet(@RequestParam(name = "orderId") String orderId) throws NotFoundException {
        Order order = orderService.get(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(order);
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
