package com.pes.plan.domain;

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

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, UUID> {

	@EntityGraph(attributePaths = "product")
	@Query("""
			SELECT p FROM ProductionPlan p
			WHERE (:search = ''
			    OR lower(p.planNumber) LIKE lower(concat('%', :search, '%'))
			    OR lower(p.product.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(p.product.name) LIKE lower(concat('%', :search, '%')))
			  AND (:status IS NULL OR p.status = :status)
			""")
	Page<ProductionPlan> search(
			@Param("search") String search,
			@Param("status") ProductionPlanStatus status,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM ProductionPlan p JOIN FETCH p.product WHERE p.id = :id")
	Optional<ProductionPlan> findByIdForUpdate(@Param("id") UUID id);
}
