package com.tenantportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rent_agreements")
public class RentAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal escalationPercent;

    /** Day-of-year (1-366) the annual escalation applies on. */
    @Column(nullable = false)
    private Integer escalationDayOfYear;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDate nextEscalationDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal nextRentAmount;
}
