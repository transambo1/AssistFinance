package com.financeai.finance_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
@Entity
@Table(name = "debts_loans")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class DebtLoan extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "type", nullable = false, length = 10)
    private String type; // DEBT, LOAN

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", insertable = false, updatable = false)
    private BigDecimal remainingAmount; // Cột này do DB tự tính (Generated Always)

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "ONGOING";

    @Column(name = "start_date")
    private Long startDate;

    @Column(name = "due_date")
    private Long dueDate;

    @Builder.Default
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
