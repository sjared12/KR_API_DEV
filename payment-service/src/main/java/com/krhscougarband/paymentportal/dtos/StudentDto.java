package com.krhscougarband.paymentportal.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO for Student to avoid serialization issues with bidirectional relationships
 */
@Data
@AllArgsConstructor
public class StudentDto {
    private String studentId;
    private String firstName;
    private String lastName;

    public StudentDto(com.krhscougarband.paymentportal.entities.Student student) {
        this.studentId = student.getStudentId();
        this.firstName = student.getFirstName();
        this.lastName = student.getLastName();
    }
}
