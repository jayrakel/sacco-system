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
    private UUID loanId;
    private String loanNumber;
    private String applicantName;
    private BigDecimal guaranteeAmount;
    private String status;

    public static GuarantorDTO fromEntity(Guarantor g) {
        GuarantorDTOBuilder builder = GuarantorDTO.builder()
                .id(g.getId())
                .memberId(g.getMember().getId())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .guaranteeAmount(g.getGuaranteeAmount())
                .status(g.getStatus().toString());

        if (g.getLoan() != null) {
            builder.loanId(g.getLoan().getId())
                    .loanNumber(g.getLoan().getLoanNumber())
                    .applicantName(g.getLoan().getMemberName());
        }
        return builder.build();
    }
}