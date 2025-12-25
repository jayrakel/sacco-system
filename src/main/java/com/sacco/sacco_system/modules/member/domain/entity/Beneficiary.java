package com.sacco.sacco_system.modules.member.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String relationship; // e.g., SPOUSE, CHILD, PARENT

    private String idNumber;
    
    private String phoneNumber;

    @Column(name = "allocation_percentage")
    private Double allocation; // e.g., 50.0 for 50%
}