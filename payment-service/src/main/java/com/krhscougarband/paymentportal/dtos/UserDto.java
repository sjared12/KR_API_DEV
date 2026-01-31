package com.krhscougarband.paymentportal.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for User to avoid serialization issues with bidirectional relationships
 */
@Data
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private List<StudentDto> students;

    public UserDto(com.krhscougarband.paymentportal.entities.User user) {
        this.id = user.getId().toString();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.students = user.getStudents().stream()
                .map(StudentDto::new)
                .collect(Collectors.toList());
    }
}
