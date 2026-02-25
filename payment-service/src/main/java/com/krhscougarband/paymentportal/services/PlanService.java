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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanService {
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
    public void pausePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        if (!plan.isActive() || "CANCELLED".equals(plan.getStatus())) {
            throw new BadRequestException("Cannot pause a cancelled or inactive plan");
        }
        
        plan.setPaused(true);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }
    
    @Transactional
    public void resumePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        if (!plan.isActive()) {
            throw new BadRequestException("Cannot resume an inactive plan");
        }
        
        plan.setPaused(false);
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
    public void refundPlan(UUID planId, long amountCents, String refundMethod) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.isActive() || "CANCELLED".equals(plan.getStatus())) {
            throw new BadRequestException("Cannot refund a cancelled or inactive plan");
        }
        
        BigDecimal refundAmount = BigDecimal.valueOf(amountCents);
        BigDecimal currentAmount = plan.getAmountPaid() != null ? plan.getAmountPaid() : BigDecimal.ZERO;
        
        // Validate refund amount doesn't exceed amount paid
        if (refundAmount.compareTo(currentAmount) > 0) {
            throw new BadRequestException("Refund amount cannot exceed amount already paid");
        }
        
        // Create refund record with SUBMITTED status - pending approval
        RefundRecord refund = new RefundRecord();
        refund.setPlan(plan);
        refund.setRefundAmount(refundAmount);
        refund.setRefundMethod(refundMethod);
        refund.setRequestedAt(LocalDateTime.now());
        refund.setStatus("SUBMITTED");
        refundRecordRepository.save(refund);
    }

    /**
     * Approve a submitted refund (mark as approved, do not modify amountPaid directly)
     * The mapToDto method will calculate net amount paid by subtracting approved refunds
     */
    public void approveRefund(UUID refundId) {
        RefundRecord refund = refundRecordRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund record not found"));
        
        if (!"SUBMITTED".equals(refund.getStatus())) {
            throw new BadRequestException("Only SUBMITTED refunds can be approved");
        }
        
        Plan plan = refund.getPlan();
        BigDecimal currentAmount = plan.getAmountPaid() != null ? plan.getAmountPaid() : BigDecimal.ZERO;
        
        // Validate that we have enough paid amount to refund
        if (refund.getRefundAmount().compareTo(currentAmount) > 0) {
            throw new BadRequestException("Refund amount cannot exceed amount paid");
        }
        
        // Mark refund as APPROVED and processed
        // Note: Do NOT reduce amountPaid here - mapToDto will subtract approved refunds dynamically
        refund.setStatus("APPROVED");
        refund.setApprovedAt(LocalDateTime.now());
        refund.setProcessedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        refundRecordRepository.save(refund);
    }

    public List<PlanDto> getPlansForUser(String email) {
        return userRepository.findByEmail(email)
            .map(user -> planRepository.findByOwner(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList()))
            .orElseGet(List::of);
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


    /**
     * Get all pending refunds awaiting approval by refund approver
     */
    public List<RefundRecordDto> getPendingRefunds() {
        List<RefundRecord> records = refundRecordRepository.findByStatus("SUBMITTED");
        return records.stream()
                .map(this::refundRecordToDto)
                .collect(Collectors.toList());
    }

    private RefundRecordDto refundRecordToDto(RefundRecord record) {
        if (record == null) return null;
        RefundRecordDto dto = new RefundRecordDto();
        dto.setId(record.getId() != null ? record.getId().toString() : null);
        dto.setPlanId(record.getPlan() != null ? record.getPlan().getId().toString() : null);
        dto.setRefundAmount(record.getRefundAmount());
        dto.setRefundReason(record.getRefundReason());
        dto.setStatus(record.getStatus());
        dto.setSquareRefundId(record.getSquareRefundId());
        dto.setRefundMethod(record.getRefundMethod());
        dto.setInitiatedByUserEmail(record.getInitiatedByUser() != null ? record.getInitiatedByUser().getEmail() : null);
        dto.setApprovedByUserEmail(record.getApprovedByUser() != null ? record.getApprovedByUser().getEmail() : null);
        dto.setRequestedAt(record.getRequestedAt());
        dto.setApprovedAt(record.getApprovedAt());
        dto.setProcessedAt(record.getProcessedAt());
        dto.setNotes(record.getNotes());
        return dto;
    }

    private PlanDto mapToDto(Plan plan) {
        if (plan == null) return null;
        PlanDto dto = new PlanDto();
        dto.setId(plan.getId() != null ? plan.getId().toString() : null);
        dto.setName(plan.getName());
        dto.setAmount(plan.getAmount());
        dto.setTotalOwed(plan.getTotalOwed());
        
        // Calculate net amount paid by subtracting approved refunds
        BigDecimal amountPaid = plan.getAmountPaid() != null ? plan.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        
        if (plan.getRefundRecords() != null && !plan.getRefundRecords().isEmpty()) {
            totalRefunds = plan.getRefundRecords().stream()
                    .filter(r -> "APPROVED".equals(r.getStatus()))
                    .map(RefundRecord::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        BigDecimal netAmountPaid = amountPaid.subtract(totalRefunds);
        dto.setAmountPaid(netAmountPaid);
        
        dto.setFrequency(plan.getFrequency());
        dto.setCurrency(plan.getCurrency());
        dto.setStatus(plan.getStatus());
        dto.setPaused(plan.isPaused());
        dto.setStartDate(plan.getStartDate() != null ? plan.getStartDate().toString() : null);
        
        // Calculate next charge date
        LocalDateTime nextCharge = plan.calculateNextChargeDate();
        dto.setNextChargeDate(nextCharge != null ? nextCharge.toString() : null);
        
        dto.setStudentId(plan.getStudent() != null ? plan.getStudent().getId().toString() : null);
        dto.setStudentName(plan.getStudent() != null ? plan.getStudent().getName() : null);
        dto.setUserEmail(plan.getOwner() != null ? plan.getOwner().getEmail() : null);
        // Add more fields as needed
        return dto;
    }
}
