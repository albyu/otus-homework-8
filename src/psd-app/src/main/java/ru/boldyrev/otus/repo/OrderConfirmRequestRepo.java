package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.boldyrev.otus.model.entity.OrderConfirmRequest;
import ru.boldyrev.otus.model.enums.OrderRequestType;
import ru.boldyrev.otus.model.enums.OtusSystem;

import java.util.List;

public interface OrderConfirmRequestRepo  extends JpaRepository<OrderConfirmRequest, Long> {

    @Query("SELECT o FROM OrderConfirmRequest o WHERE o.orderCaseId = :orderCaseId and o.orderRequestType = :orderRequestType " +
            "and o.systemName = :systemName")
    List<OrderConfirmRequest> findByComposedKey(@Param("orderCaseId")Long orderCaseId,
                                                @Param("orderRequestType")OrderRequestType orderRequestType,
                                                @Param("systemName")OtusSystem systemName);

    List<OrderConfirmRequest> findByOrderId (String orderId);
}
