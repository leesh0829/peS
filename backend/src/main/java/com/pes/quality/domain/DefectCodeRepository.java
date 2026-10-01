package com.pes.quality.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DefectCodeRepository extends JpaRepository<DefectCode, UUID> {
    @Query("""
        SELECT d FROM DefectCode d WHERE :search = ''
        OR lower(d.code) LIKE lower(concat('%', :search, '%'))
        OR lower(d.name) LIKE lower(concat('%', :search, '%'))
        """)
    Page<DefectCode> search(@Param("search") String search, Pageable pageable);
}
