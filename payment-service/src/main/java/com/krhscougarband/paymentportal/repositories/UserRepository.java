package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "students")
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithStudents(String email);

    @EntityGraph(attributePaths = "students")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithStudents(UUID id);

    @EntityGraph(attributePaths = "students")
    @Query("SELECT u FROM User u")
    List<User> findAllWithStudents();
}
