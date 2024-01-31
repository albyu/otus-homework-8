package ru.boldyrev.otus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.model.entity.Product;
import ru.boldyrev.otus.repo.ProductRepo;

@Service
public class ProductService {
    private final ProductRepo productRepo;

    @Autowired
    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }


    public Product get(Long productId) throws ConflictException {
        return productRepo.findById(productId).orElseThrow(()-> new ConflictException("Product not found"));
    }

    public Product addOrUpdate(Product product) {
        return productRepo.save(product);
    }

    public boolean delete(Long productId) {
        if (productRepo.findById(productId).isPresent()){
            productRepo.deleteById(productId);
            return true;
        } else {
            return false;
        }
    }
}
