package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.boldyrev.otus.model.entity.OrderCaseLogEntry;

import java.util.List;

public interface OrderCaseLogEntryRepo extends JpaRepository<OrderCaseLogEntry, Long> {
    @Query("SELECT o FROM OrderCaseLogEntry o WHERE o.orderCase.id = :orderCaseId")
    List<OrderCaseLogEntry> findByOrderCaseId(@Param("orderCaseId")Long orderCaseId);
}
