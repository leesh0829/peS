package com.pes.workorder.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

	interface PlanAllocation {
		UUID getPlanId();

		long getAllocatedQuantity();

		long getWorkOrderCount();
	}

	@EntityGraph(attributePaths = { "productionPlan", "productionPlan.product", "productionProcess", "assignedWorker" })
	@Query("""
			SELECT w FROM WorkOrder w
			WHERE (:search = ''
			    OR lower(w.workOrderNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.planNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.product.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.product.name) LIKE lower(concat('%', :search, '%')))
			  AND (:status IS NULL OR w.status = :status)
			""")
	Page<WorkOrder> searchAll(
			@Param("search") String search,
			@Param("status") WorkOrderStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = { "productionPlan", "productionPlan.product", "productionProcess", "assignedWorker" })
	@Query("""
			SELECT w FROM WorkOrder w
			WHERE w.assignedWorker.id = :workerId
			  AND (:search = ''
			    OR lower(w.workOrderNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.planNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.product.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(w.productionPlan.product.name) LIKE lower(concat('%', :search, '%')))
			  AND (:status IS NULL OR w.status = :status)
			""")
	Page<WorkOrder> searchAssigned(
			@Param("workerId") UUID workerId,
			@Param("search") String search,
			@Param("status") WorkOrderStatus status,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT w FROM WorkOrder w
			JOIN FETCH w.productionPlan p
			JOIN FETCH p.product
			JOIN FETCH w.productionProcess
			JOIN FETCH w.assignedWorker
			WHERE w.id = :id
			""")
	Optional<WorkOrder> findByIdForUpdate(@Param("id") UUID id);

	@Query("SELECT COALESCE(SUM(w.targetQuantity), 0) FROM WorkOrder w WHERE w.productionPlan.id = :planId")
	long sumTargetQuantityByProductionPlanId(@Param("planId") UUID planId);

	@Query("""
			SELECT w.productionPlan.id AS planId,
			       SUM(w.targetQuantity) AS allocatedQuantity,
			       COUNT(w) AS workOrderCount
			FROM WorkOrder w
			WHERE w.productionPlan.id IN :planIds
			GROUP BY w.productionPlan.id
			""")
	List<PlanAllocation> summarizeByProductionPlanIdIn(@Param("planIds") Collection<UUID> planIds);

	@Query("""
			SELECT w.productionPlan.id AS planId,
			       SUM(w.targetQuantity) AS allocatedQuantity,
			       COUNT(w) AS workOrderCount
			FROM WorkOrder w
			WHERE w.productionPlan.id = :planId
			GROUP BY w.productionPlan.id
			""")
	Optional<PlanAllocation> summarizeByProductionPlanId(@Param("planId") UUID planId);

	long countByStatus(WorkOrderStatus status);

	@Query("SELECT COALESCE(SUM(w.targetQuantity), 0) FROM WorkOrder w")
	long sumTargetQuantity();
}
