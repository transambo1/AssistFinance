package com.financeai.finance_management.entity;

import jakarta.persistence.*;
import com.financeai.finance_management.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Category extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    @Builder.Default
    private CategoryType type = CategoryType.EXPENSE;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 10)
    private String color;

    @Builder.Default
    @Column(name = "is_archived")
    private boolean isArchived = false;

    @Builder.Default
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Transaction> transactions = new HashSet<>();
}
