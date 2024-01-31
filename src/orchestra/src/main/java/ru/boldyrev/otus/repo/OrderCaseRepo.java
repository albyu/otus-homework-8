package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.OrderCase;

import java.util.List;


public interface OrderCaseRepo extends JpaRepository<OrderCase, Long> {

    List<OrderCase> findByOrderId(String orderId);
}

