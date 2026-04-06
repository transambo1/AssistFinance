package com.financeai.finance_management.entity;

import com.financeai.finance_management.enums.FrequencyType;
import com.financeai.finance_management.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
@Entity
@Table(name = "salary_configs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class SalaryConfig extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catetgory_id", nullable = false)
    private Category category;

    @Builder.Default
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "pay_day")
    private Integer payDay; // CHECK (pay_day BETWEEN 1 AND 31)

    @Column(name = "last_processed")
    private Long lastProcessed;

    @Column(name = "description")
    private String description; // Nội dung note cho Transaction

    @Column(name = "frequency")
    @Enumerated(EnumType.STRING)
    private FrequencyType frequency; // WEEKLY, MONTHLY

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;
}
