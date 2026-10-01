package com.pes.quality.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InspectionResultRepository extends JpaRepository<InspectionResult, UUID> {
    boolean existsByProductLotId(UUID productLotId);

    @EntityGraph(attributePaths = {"productLot", "productLot.productionResult", "productLot.productionResult.workOrder",
        "productLot.productionResult.workOrder.productionPlan", "productLot.productionResult.workOrder.productionPlan.product",
        "productLot.productionResult.recordedBy", "inspectedBy"})
    @Query("""
        SELECT i FROM InspectionResult i
        WHERE (:workerId IS NULL OR i.productLot.productionResult.workOrder.assignedWorker.id = :workerId)
        AND (:judgement IS NULL OR i.judgement = :judgement)
        AND (:search = '' OR lower(i.productLot.lotNumber) LIKE lower(concat('%', :search, '%'))
            OR lower(i.productLot.productionResult.workOrder.workOrderNumber) LIKE lower(concat('%', :search, '%')))
        """)
    Page<InspectionResult> search(@Param("workerId") UUID workerId, @Param("search") String search,
        @Param("judgement") InspectionJudgement judgement, Pageable pageable);

    @EntityGraph(attributePaths = {"productLot", "productLot.productionResult", "productLot.productionResult.workOrder",
        "productLot.productionResult.workOrder.productionPlan", "productLot.productionResult.workOrder.productionPlan.product",
        "productLot.productionResult.recordedBy", "inspectedBy", "defects", "defects.defectCode"})
    Optional<InspectionResult> findByProductLotId(UUID productLotId);
}
