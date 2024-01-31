package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.Product;

public interface ProductRepo  extends JpaRepository<Product, Long> {
}
