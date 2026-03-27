package com.financeai.finance_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@JsonIgnoreProperties(
    value = {"created_at", "updated_at", "deleted_at"},
    allowGetters = true)
public abstract class BaseEntity {
  @Builder.Default
  @Column(name = "is_active", nullable = false)
  protected boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  protected Long createdAt;

  @Column(name = "updated_at", nullable = false)
  protected Long updatedAt;

  @Column(name = "deleted_at")
  protected Long deletedAt;

//  @Version
//  @Column(name = "version", nullable = false)
//  protected int version;

  @PrePersist
  protected void prePersist() {
    Instant now = Instant.now();
    if (this.createdAt == null) this.createdAt = now.toEpochMilli();
    if (this.updatedAt == null) this.updatedAt = now.toEpochMilli();
  }

  @PreUpdate
  protected void preUpdate() {
    this.updatedAt = Instant.now().toEpochMilli();
  }

  @PreRemove
  protected void preRemove() {
    this.deletedAt = Instant.now().toEpochMilli();
  }

  public boolean isActive() {
    return this.isActive;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void activate() {
    this.isActive = true;
  }
}
