package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.dtos.StudentDto;
import com.krhscougarband.paymentportal.entities.Student;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.entities.UserProfile;
import com.krhscougarband.paymentportal.repositories.StudentRepository;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import com.krhscougarband.paymentportal.services.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository, StudentRepository studentRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String userEmail = authentication.getName();
        
        // Get user info with students
        Optional<User> userOpt = userRepository.findByEmailWithStudents(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        UserProfile profile = profileService.getByEmail(userEmail);
        
        // Debug logging
        System.out.println("DEBUG ProfileController - User email: " + user.getEmail());
        System.out.println("DEBUG ProfileController - Students count: " + (user.getStudents() != null ? user.getStudents().size() : 0));
        if (user.getStudents() != null) {
            user.getStudents().forEach(s -> 
                System.out.println("DEBUG ProfileController - Student: " + s.getStudentId() + " - " + s.getFirstName() + " " + s.getLastName())
            );
        }
        
        // If profile doesn't exist, create a new one
        if (profile == null) {
            profile = profileService.upsertProfile(userEmail, userEmail, "");
        }
        
        // Convert students to DTOs
        List<StudentDto> studentDtos = user.getStudents() != null 
                ? user.getStudents().stream()
                    .map(StudentDto::new)
                    .collect(Collectors.toList())
                : List.of();
        
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        response.put("students", studentDtos);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String userEmail = authentication.getName();
        
        // Update user info
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        if (body.containsKey("firstName")) {
            user.setFirstName(body.get("firstName"));
        }
        if (body.containsKey("lastName")) {
            user.setLastName(body.get("lastName"));
        }
        
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        response.put("message", "Profile updated successfully");
        
        return ResponseEntity.ok(response);
    }

    // Add student to profile
    @PostMapping("/students")
    @Transactional
    public ResponseEntity<?> addStudent(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String studentId = body.get("studentId");
        String firstName = body.get("firstName");
        String lastName = body.get("lastName");
        String instrument = body.get("instrument");

        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student ID is required"));
        }

        User user = userRepository.findByEmailWithStudents(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find or create student
        Student student = studentRepository.findById(studentId)
                .orElse(new Student());
        
        student.setStudentId(studentId);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setInstrument(instrument);

        // Save student first
        student = studentRepository.save(student);
        
        // Check if student is already in user's collection
        boolean alreadyAdded = user.getStudents().stream()
                .anyMatch(s -> s.getStudentId().equals(studentId));
        
        // Add student to user's students only if not already present
        if (!alreadyAdded) {
            user.getStudents().add(student);
            userRepository.save(user);
        }

        // Return updated student list
        List<StudentDto> studentDtos = user.getStudents().stream()
                .map(StudentDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "message", "Student added successfully",
            "students", studentDtos
        ));
    }

    // Remove student from profile
    @DeleteMapping("/students/{studentId}")
    @Transactional
    public ResponseEntity<?> removeStudent(Authentication authentication, @PathVariable String studentId) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmailWithStudents(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        user.getStudents().remove(student);
        userRepository.save(user);

        // Return updated student list
        List<StudentDto> studentDtos = user.getStudents().stream()
                .map(StudentDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "message", "Student removed from account",
            "students", studentDtos
        ));
    }
}

