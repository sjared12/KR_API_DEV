package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.dto.PlanDto;
import com.krhscougarband.paymentportal.dto.PlanUpdateDto;
import com.krhscougarband.paymentportal.dto.RefundRecordDto;
import com.krhscougarband.paymentportal.dto.RefundRequest;
import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.entities.RefundRecord;
import com.krhscougarband.paymentportal.entities.Student;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.exceptions.ResourceNotFoundException;
import com.krhscougarband.paymentportal.exceptions.UnauthorizedException;
import com.krhscougarband.paymentportal.exceptions.BadRequestException;
import com.krhscougarband.paymentportal.repositories.PlanRepository;
import com.krhscougarband.paymentportal.repositories.RefundRecordRepository;
import com.krhscougarband.paymentportal.repositories.StudentRepository;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanService {
    @Transactional
    public PlanDto updatePlan(UUID id, PlanUpdateDto dto, String email) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        User user = userRepository.findByEmail(email).orElseThrow();
        if (!plan.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this plan");
        }
        if (dto.getName() != null) plan.setName(dto.getName());
        if (dto.getAmount() != null) plan.setAmount(dto.getAmount());
        if (dto.getTotalOwed() != null) plan.setTotalOwed(dto.getTotalOwed());
        if (dto.getFrequency() != null) plan.setFrequency(dto.getFrequency());
        if (dto.getCurrency() != null) plan.setCurrency(dto.getCurrency());
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        return mapToDto(plan);
    }

    @Transactional
    public void cancelPlan(UUID id, String email) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        User user = userRepository.findByEmail(email).orElseThrow();
        if (!plan.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this plan");
        }
        plan.setStatus("CANCELLED");
        plan.setActive(false);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    @Transactional
    public Plan updateFrequency(UUID id, String frequency, String email) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        User user = userRepository.findByEmail(email).orElseThrow();
        if (!plan.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this plan");
        }
        plan.setFrequency(frequency);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        return plan;
    }

    @Transactional
    public void refundPlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.isActive() || "CANCELLED".equals(plan.getStatus())) {
            throw new BadRequestException("Cannot refund a cancelled or inactive plan");
        }
        // Example: create a refund record and mark plan as refund requested
        RefundRecord refund = new RefundRecord();
        refund.setPlan(plan);
        refund.setRequestedAt(LocalDateTime.now());
        refund.setStatus("REQUESTED");
        refundRecordRepository.save(refund);
        plan.setStatus("REFUND_REQUESTED");
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SquareBillingService squareBillingService;
    private final AuditService auditService;
    private final RefundRecordRepository refundRecordRepository;

    public PlanService(PlanRepository planRepository,
                       UserRepository userRepository,
                       StudentRepository studentRepository,
                       SquareBillingService squareBillingService,
                       AuditService auditService,
                       RefundRecordRepository refundRecordRepository) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.squareBillingService = squareBillingService;
        this.auditService = auditService;
        this.refundRecordRepository = refundRecordRepository;
    }

    public List<PlanDto> getPlansForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return planRepository.findByOwner(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PlanDto> getAllPlans() {
        return planRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlanDto createPlan(PlanDto dto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Plan plan = new Plan();
        plan.setOwner(user);
        plan.setName(dto.getName());
        plan.setAmount(dto.getAmount());
        plan.setTotalOwed(dto.getTotalOwed() != null ? dto.getTotalOwed() : dto.getAmount());
        plan.setAmountPaid(BigDecimal.ZERO);
        plan.setFrequency(dto.getFrequency());
        plan.setCurrency(dto.getCurrency());
        plan.setStatus("ACTIVE");
        plan.setActive(true);
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            Student student = studentRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            plan.setStudent(student);
        }
        planRepository.save(plan);
        return mapToDto(plan);
    }

    /**
     * Re-enable a cancelled plan (admin only)
     */
    @Transactional
    public PlanDto reactivatePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!"CANCELLED".equals(plan.getStatus())) {
            throw new BadRequestException("Only cancelled plans can be re-enabled");
        }
        plan.setActive(true);
        plan.setStatus("ACTIVE");
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        // Optionally, re-enable subscription in Square here
        return mapToDto(plan);
    }


    private PlanDto mapToDto(Plan plan) {
        if (plan == null) return null;
        PlanDto dto = new PlanDto();
        dto.setId(plan.getId() != null ? plan.getId().toString() : null);
        dto.setName(plan.getName());
        dto.setAmount(plan.getAmount());
        dto.setTotalOwed(plan.getTotalOwed());
        dto.setAmountPaid(plan.getAmountPaid());
        dto.setFrequency(plan.getFrequency());
        dto.setCurrency(plan.getCurrency());
        dto.setStatus(plan.getStatus());
        dto.setStudentId(plan.getStudent() != null ? plan.getStudent().getId().toString() : null);
        dto.setStudentName(plan.getStudent() != null ? plan.getStudent().getName() : null);
        dto.setUserEmail(plan.getOwner() != null ? plan.getOwner().getEmail() : null);
        // Add more fields as needed
        return dto;
    }
}
