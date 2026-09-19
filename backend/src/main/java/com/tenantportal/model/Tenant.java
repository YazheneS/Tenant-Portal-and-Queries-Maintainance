package com.tenantportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true)
    private String clerkUserId;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private Unit unit;

    private LocalDate moveInDate;
    private LocalDate moveOutDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    private String emergencyContact;
}