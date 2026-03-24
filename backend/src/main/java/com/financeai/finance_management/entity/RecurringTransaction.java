package com.financeai.finance_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
@Entity
@Table(name = "recurring_transactions")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class RecurringTransaction extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // INCOME, EXPENSE

    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    @Builder.Default
    @Column(name = "every")
    private Integer every = 1;

    @Column(name = "next_trigger_date", nullable = false)
    private Long nextTriggerDate;

    @Column(name = "last_trigger_date")
    private Long lastTriggerDate;

    @Column(name = "end_date")
    private Long endDate;

    @Builder.Default
    @Column(name = "auto_apply")
    private boolean autoApply = true;
}