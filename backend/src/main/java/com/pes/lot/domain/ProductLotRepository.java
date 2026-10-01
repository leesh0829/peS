package com.pes.lot.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductLotRepository extends JpaRepository<ProductLot, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ProductLot l WHERE l.id = :id")
    Optional<ProductLot> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"productionResult", "productionResult.workOrder",
        "productionResult.workOrder.productionPlan", "productionResult.workOrder.productionPlan.product",
        "productionResult.recordedBy"})
    @Query("""
        SELECT l FROM ProductLot l
        WHERE l.productionResult.workOrder.status = :status
        AND NOT EXISTS (SELECT i.id FROM InspectionResult i WHERE i.productLot = l)
        AND (:search = '' OR lower(l.lotNumber) LIKE lower(concat('%', :search, '%'))
            OR lower(l.productionResult.workOrder.workOrderNumber) LIKE lower(concat('%', :search, '%')))
        """)
    Page<ProductLot> searchInspectionCandidates(@Param("search") String search,
        @Param("status") com.pes.workorder.domain.WorkOrderStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"productionResult", "productionResult.workOrder",
        "productionResult.workOrder.productionPlan", "productionResult.workOrder.productionPlan.product",
        "productionResult.workOrder.assignedWorker", "productionResult.recordedBy"})
    @Query("""
        SELECT l FROM ProductLot l WHERE
        (:workerId IS NULL OR l.productionResult.workOrder.assignedWorker.id = :workerId)
        AND (:search = '' OR lower(l.lotNumber) LIKE lower(concat('%', :search, '%'))
        OR lower(l.productionResult.workOrder.workOrderNumber) LIKE lower(concat('%', :search, '%'))
        OR lower(l.productionResult.workOrder.productionPlan.product.code) LIKE lower(concat('%', :search, '%')))
        """)
    Page<ProductLot> search(@Param("workerId") UUID workerId, @Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"productionResult", "productionResult.workOrder",
        "productionResult.workOrder.productionPlan", "productionResult.workOrder.productionPlan.product",
        "productionResult.workOrder.assignedWorker", "productionResult.recordedBy"})
    @Query("SELECT l FROM ProductLot l WHERE l.id = :id")
    Optional<ProductLot> findDetailById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"productionResult", "productionResult.workOrder",
        "productionResult.workOrder.productionPlan", "productionResult.workOrder.productionPlan.product",
        "productionResult.recordedBy"})
    List<ProductLot> findByProductionResultWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
