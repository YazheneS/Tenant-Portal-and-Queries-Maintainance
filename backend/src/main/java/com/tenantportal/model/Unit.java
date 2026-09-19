package com.tenantportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String unitNumber;

    private Integer floor;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false)
    private Boolean isOccupied = false;
}