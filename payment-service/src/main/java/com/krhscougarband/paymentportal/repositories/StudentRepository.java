package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
}
