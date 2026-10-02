package com.pes.label;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelEventRepository extends JpaRepository<LabelEvent, UUID> {
    Page<LabelEvent> findByProductLotId(UUID id, Pageable pageable);
    Optional<LabelEvent> findFirstByProductLotIdOrderBySequenceNumberDesc(UUID id);
}
