package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.Order;


public interface OrderRepo extends JpaRepository<Order, String> {}
