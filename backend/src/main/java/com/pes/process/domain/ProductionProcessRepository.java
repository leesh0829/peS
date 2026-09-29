package com.pes.process.domain;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionProcessRepository extends JpaRepository<ProductionProcess, UUID> {

	boolean existsByCode(String code);

	@Query("""
			SELECT p FROM ProductionProcess p
			WHERE (:search IS NULL
			    OR lower(p.code) LIKE lower(concat('%', :search, '%'))
			    OR lower(p.name) LIKE lower(concat('%', :search, '%')))
			  AND (:active IS NULL OR p.active = :active)
			""")
	Page<ProductionProcess> search(
			@Param("search") String search,
			@Param("active") Boolean active,
			Pageable pageable);
}
