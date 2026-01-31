package com.krhscougarband.paymentportal.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Student {
    @Id
    @EqualsAndHashCode.Include
    private String studentId; // Student ID number as primary key
    
    private String firstName;
    private String lastName;
    
    @JsonIgnore
    @ManyToMany(mappedBy = "students")
    private Set<User> users = new HashSet<>();
    public String getId() {
        return studentId;
    }

    public String getName() {
        return firstName + (lastName != null ? (" " + lastName) : "");
    }
}
