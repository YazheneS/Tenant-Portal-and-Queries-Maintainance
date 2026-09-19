package com.tenantportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.INITIATED;

    private LocalDateTime initiatedAt;
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        initiatedAt = LocalDateTime.now();
    }

    public enum Status {
        INITIATED, SUCCESS, FAILED, REFUNDED
    }
}