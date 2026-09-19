package com.tenantportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_disputes")
public class BillDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne
    @JoinColumn(name = "line_item_id", nullable = false)
    private BillLineItem lineItem;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    private String evidencePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RAISED;

    private String ownerResponse;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Status {
        RAISED, UNDER_REVIEW, ACCEPTED, REJECTED
    }
}