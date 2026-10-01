package com.pes.lot.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, UUID> {
    @EntityGraph(attributePaths = {"materialLot", "workOrder", "recordedBy"})
    List<WorkOrderMaterial> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);

    @EntityGraph(attributePaths = {"materialLot", "workOrder", "workOrder.assignedWorker", "recordedBy"})
    List<WorkOrderMaterial> findByMaterialLotIdOrderByCreatedAtAsc(UUID materialLotId);

    boolean existsByWorkOrderIdAndMaterialLotId(UUID workOrderId, UUID materialLotId);

    @Query("SELECT COALESCE(SUM(m.inputQuantity), 0) FROM WorkOrderMaterial m WHERE m.workOrder.id = :id")
    long sumByWorkOrderId(@Param("id") UUID id);

    @Query("SELECT COALESCE(SUM(m.inputQuantity), 0) FROM WorkOrderMaterial m WHERE m.materialLot.id = :id")
    long sumByMaterialLotId(@Param("id") UUID id);
}
