package com.pes.result.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionResultRepository extends JpaRepository<ProductionResult, UUID> {

	interface QuantitySummary {
		long getProducedQuantity();

		long getGoodQuantity();

		long getDefectQuantity();
	}

	interface WorkOrderQuantitySummary extends QuantitySummary {
		UUID getWorkOrderId();
	}

	@EntityGraph(attributePaths = {
			"workOrder", "workOrder.productionPlan", "workOrder.productionPlan.product",
			"workOrder.productionProcess", "workOrder.assignedWorker", "recordedBy"
	})
	@Query("""
			SELECT r FROM ProductionResult r
			WHERE (:search = ''
			    OR lower(r.workOrder.workOrderNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.planNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.product.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.product.name) LIKE lower(concat('%', :search, '%')))
			""")
	Page<ProductionResult> searchAll(@Param("search") String search, Pageable pageable);

	@EntityGraph(attributePaths = {
			"workOrder", "workOrder.productionPlan", "workOrder.productionPlan.product",
			"workOrder.productionProcess", "workOrder.assignedWorker", "recordedBy"
	})
	@Query("""
			SELECT r FROM ProductionResult r
			WHERE r.workOrder.assignedWorker.id = :workerId
			  AND (:search = ''
			    OR lower(r.workOrder.workOrderNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.planNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.product.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(r.workOrder.productionPlan.product.name) LIKE lower(concat('%', :search, '%')))
			""")
	Page<ProductionResult> searchAssigned(
			@Param("workerId") UUID workerId,
			@Param("search") String search,
			Pageable pageable);

	@Query("""
			SELECT r.workOrder.id AS workOrderId,
			       SUM(r.producedQuantity) AS producedQuantity,
			       SUM(r.goodQuantity) AS goodQuantity,
			       SUM(r.defectQuantity) AS defectQuantity
			FROM ProductionResult r
			WHERE r.workOrder.id IN :workOrderIds
			GROUP BY r.workOrder.id
			""")
	List<WorkOrderQuantitySummary> summarizeByWorkOrderIdIn(
			@Param("workOrderIds") Collection<UUID> workOrderIds);

	@Query("""
			SELECT r.workOrder.id AS workOrderId,
			       SUM(r.producedQuantity) AS producedQuantity,
			       SUM(r.goodQuantity) AS goodQuantity,
			       SUM(r.defectQuantity) AS defectQuantity
			FROM ProductionResult r
			WHERE r.workOrder.id = :workOrderId
			GROUP BY r.workOrder.id
			""")
	Optional<WorkOrderQuantitySummary> summarizeByWorkOrderId(@Param("workOrderId") UUID workOrderId);

	@Query("SELECT COALESCE(SUM(r.producedQuantity), 0) FROM ProductionResult r WHERE r.workOrder.id = :workOrderId")
	long sumProducedQuantityByWorkOrderId(@Param("workOrderId") UUID workOrderId);

	@Query("""
			SELECT COALESCE(SUM(r.producedQuantity), 0) AS producedQuantity,
			       COALESCE(SUM(r.goodQuantity), 0) AS goodQuantity,
			       COALESCE(SUM(r.defectQuantity), 0) AS defectQuantity
			FROM ProductionResult r
			""")
	QuantitySummary summarizeAll();
}
