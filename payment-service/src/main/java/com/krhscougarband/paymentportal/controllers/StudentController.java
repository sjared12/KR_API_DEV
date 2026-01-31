package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.dtos.StudentDto;
import com.krhscougarband.paymentportal.entities.Student;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.repositories.StudentRepository;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    // Add student to current user's account
    @PostMapping
    public ResponseEntity<?> addStudent(Authentication auth, @RequestBody Map<String, String> body) {
        // ...existing code...
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        String studentId = body.get("studentId");
        String firstName = body.get("firstName");
        String lastName = body.get("lastName");

        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student ID is required"));
        }

        User user = userRepository.findByEmailWithStudents(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find or create student
        Student student = studentRepository.findById(studentId)
                .orElse(new Student());
        
        student.setStudentId(studentId);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        // Save student first (without cascading through user)
        student = studentRepository.save(student);
        
        // Check if student is already in user's collection
        boolean alreadyAdded = user.getStudents().stream()
                .anyMatch(s -> s.getStudentId().equals(studentId));
        
        // Add student to user's students only if not already present
        if (!alreadyAdded) {
            user.getStudents().add(student);
        }
        
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Student added successfully"));
    }

    // Get all students for current user
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyStudents(Authentication auth) {
        // ...existing code...
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User user = userRepository.findByEmailWithStudents(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        // Convert to DTOs to avoid serialization issues
        List<StudentDto> studentDtos = user.getStudents() != null 
                ? user.getStudents().stream()
                    .map(StudentDto::new)
                    .collect(Collectors.toList())
                : List.of();
        return ResponseEntity.ok(studentDtos);
    }

    // Remove student from current user's account
    @DeleteMapping("/{studentId}")
    public ResponseEntity<?> removeStudent(Authentication auth, @PathVariable String studentId) {
        User user = userRepository.findByEmailWithStudents(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        user.getStudents().remove(student);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Student removed from account"));
    }

    // Admin: Get all students for a specific user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> getUserStudents(@PathVariable UUID userId) {
        User user = userRepository.findByIdWithStudents(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(user.getStudents());
    }

    // Admin: Add student to a specific user
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> addStudentToUser(@PathVariable UUID userId, @RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        String firstName = body.get("firstName");
        String lastName = body.get("lastName");

        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student ID is required"));
        }

        User user = userRepository.findByIdWithStudents(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find or create student
        Student student = studentRepository.findById(studentId)
                .orElse(new Student());
        
        student.setStudentId(studentId);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        user.getStudents().add(student);
        
        studentRepository.save(student);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Student added successfully"));
    }

    // Admin: Remove student from a specific user
    @DeleteMapping("/user/{userId}/{studentId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> removeStudentFromUser(@PathVariable UUID userId, @PathVariable String studentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        user.getStudents().remove(student);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Student removed from account"));
    }
}
