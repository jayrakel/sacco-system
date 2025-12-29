package com.sacco.sacco_system.modules.loan.api.dto;

import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorDTO {
    private UUID id;
    private UUID memberId;
    private String memberName;

    // ✅ ADDED: Required for Dashboard Requests View
    private UUID loanId;
    private String loanNumber;
    private String applicantName;

    // Contact/Personal Info
    private String name;
    private String email;
    private String phone;
    private String relationship;
    private String idNumber;

    private BigDecimal guaranteeAmount;
    private String status;

    /**
     * ✅ STATIC MAPPER METHOD
     * Converts a Guarantor Entity to GuarantorDTO.
     */
    public static GuarantorDTO fromEntity(Guarantor g) {
        GuarantorDTO.GuarantorDTOBuilder builder = GuarantorDTO.builder()
                .id(g.getId())
                .memberId(g.getMember().getId())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .email(g.getMember().getEmail()) // Assuming Member has email
                .phone(g.getMember().getPhoneNumber()) // Assuming Member has phone
                .guaranteeAmount(g.getGuaranteeAmount())
                .status(g.getStatus().toString());

        // Handle Relationship to Loan (if loaded)
        if (g.getLoan() != null) {
            builder.loanId(g.getLoan().getId())
                    .loanNumber(g.getLoan().getLoanNumber())
                    .applicantName(g.getLoan().getMemberName());
        }

        return builder.build();
    }
}