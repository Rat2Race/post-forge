package com.postforge.board.posts.domain.repository;

import com.postforge.board.posts.domain.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
	Optional<User> findByAccount(String account);

	@Query("SELECT u FROM User u WHERE u.role LIKE %:role%")
	List<User> findAllByType(@Param("role") String type);

	Optional<User> findByUsername(String username);
}