package com.pes.lot.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface MaterialLotRepository extends JpaRepository<MaterialLot, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MaterialLot m WHERE m.id = :id")
    Optional<MaterialLot> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT m FROM MaterialLot m WHERE :search = ''
        OR lower(m.lotNumber) LIKE lower(concat('%', :search, '%'))
        OR lower(m.materialCode) LIKE lower(concat('%', :search, '%'))
        OR lower(m.materialName) LIKE lower(concat('%', :search, '%'))
        """)
    Page<MaterialLot> search(@Param("search") String search, Pageable pageable);
}
