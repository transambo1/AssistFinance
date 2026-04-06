package com.financeai.finance_management.entity;

import com.financeai.finance_management.enums.JobStatus; // Giả định bạn tạo enum này
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_job_logs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class SalaryJobLog extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private SalaryConfig salaryConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status; // SUCCESS, FAILED, SKIPPED

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount; // Số tiền thực hiện tại thời điểm đó

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Chi tiết lỗi (vd: "Số dư không đủ...")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction; // Nếu SUCCESS, lưu ID của Transaction vừa tạo để đối soát
}