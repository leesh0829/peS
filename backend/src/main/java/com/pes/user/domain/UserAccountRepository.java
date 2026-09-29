package com.pes.user.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByUsername(String username);

	boolean existsByUsername(String username);

	long countByRoleAndActiveTrue(UserRole role);

	@Query("""
			SELECT u FROM UserAccount u
			WHERE (:search IS NULL
			    OR lower(u.username) LIKE lower(concat('%', :search, '%'))
			    OR lower(u.displayName) LIKE lower(concat('%', :search, '%')))
			  AND (:active IS NULL OR u.active = :active)
			""")
	Page<UserAccount> search(
			@Param("search") String search,
			@Param("active") Boolean active,
			Pageable pageable);
}
