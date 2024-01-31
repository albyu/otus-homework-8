package ru.boldyrev.otus.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.model.entity.Product;
import ru.boldyrev.otus.service.ProductService;

import java.util.Map;

@RestController
@Slf4j
@Api(tags = "Редактирование справочника продуктов")
public class ProductController {
    ProductService productService;

    @Autowired
    ProductController(ProductService productService){
        this.productService = productService;
    }

    /*Получить продукт*/
    @GetMapping(value = "/product/get", produces = "application/json")
    public ResponseEntity<Product> productGet(@RequestParam(name = "productId") Long productId) throws ConflictException {
        Product product = productService.get(productId);
        return ResponseEntity.status(HttpStatus.OK).body(product);
    }

    /*Добавить продукт*/
    @PostMapping(value = "/product/post", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Product> productPost(@RequestBody Product product)  {
        product = productService.addOrUpdate(product);
        return ResponseEntity.status(HttpStatus.OK).body(product);
    }


    /*Удалить продукт*/
    @DeleteMapping(value = "/product/delete",consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, String>> productDelete(@RequestParam(name = "productId") Long productId)
    {

        if (productService.delete(productId)) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", "1 rows deleted"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("result", "0 rows deleted"));
        }
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> orderExceptionProcessing(ConflictException conflictException){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", conflictException.getMessage()));
    }


}
