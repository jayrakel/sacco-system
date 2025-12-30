package com.sacco.sacco_system.modules.member.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.member.api.dto.BeneficiaryDTO;
import com.sacco.sacco_system.modules.member.api.dto.EmploymentDetailsDTO;
import com.sacco.sacco_system.modules.member.api.dto.MemberDTO;
import com.sacco.sacco_system.modules.member.domain.entity.Beneficiary;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService; // ✅ Added
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingService systemSettingService;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;
    private final ReferenceCodeService referenceCodeService; // ✅ Inject Reference Service

    @Value("${app.upload.dir:uploads/profiles/}")
    private String uploadDir;

    public MemberDTO createMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String userExternalRef, String bankAccountCode, User user) throws IOException {

        if (memberRepository.findByEmail(memberDTO.getEmail()).isPresent()) throw new RuntimeException("Email already registered");
        if (memberRepository.findByPhoneNumber(memberDTO.getPhoneNumber()).isPresent()) throw new RuntimeException("Phone already registered");
        if (memberRepository.findByIdNumber(memberDTO.getIdNumber()).isPresent()) throw new RuntimeException("ID number already registered");

        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            imagePath = "profiles/" + filename;
        }

        String memberNumber = generateMemberNumber();

        Member member = Member.builder()
                .user(user)
                .memberNumber(memberNumber)
                .firstName(memberDTO.getFirstName())
                .lastName(memberDTO.getLastName())
                .email(memberDTO.getEmail())
                .phoneNumber(memberDTO.getPhoneNumber())
                .idNumber(memberDTO.getIdNumber())
                .kraPin(memberDTO.getKraPin())
                .address(memberDTO.getAddress())
                .dateOfBirth(memberDTO.getDateOfBirth())
                .profileImageUrl(imagePath)
                .status(Member.MemberStatus.ACTIVE)
                .totalShares(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .beneficiaries(new ArrayList<>())
                .build();

        if (memberDTO.getBeneficiaries() != null) {
            for (BeneficiaryDTO bDto : memberDTO.getBeneficiaries()) {
                Beneficiary beneficiary = Beneficiary.builder()
                        .fullName(bDto.getFullName())
                        .relationship(bDto.getRelationship())
                        .idNumber(bDto.getIdNumber())
                        .phoneNumber(bDto.getPhoneNumber())
                        .allocation(bDto.getAllocation())
                        .build();
                member.addBeneficiary(beneficiary);
            }
        }

        if (memberDTO.getEmploymentDetails() != null) {
            EmploymentDetailsDTO eDto = memberDTO.getEmploymentDetails();
            EmploymentDetails.EmploymentTerms terms = EmploymentDetails.EmploymentTerms.PERMANENT;
            try {
                if(eDto.getTerms() != null) terms = EmploymentDetails.EmploymentTerms.valueOf(eDto.getTerms());
            } catch (Exception e) {}

            EmploymentDetails details = EmploymentDetails.builder()
                    .terms(terms)
                    .employerName(eDto.getEmployerName())
                    .staffNumber(eDto.getStaffNumber())
                    .stationOrDepartment(eDto.getStationOrDepartment())
                    .dateEmployed(eDto.getDateEmployed())
                    .contractExpiryDate(eDto.getContractExpiryDate())
                    .grossMonthlyIncome(eDto.getGrossMonthlyIncome())
                    .netMonthlyIncome(eDto.getNetMonthlyIncome())
                    .bankName(eDto.getBankName())
                    .bankBranch(eDto.getBankBranch())
                    .bankAccountNumber(eDto.getBankAccountNumber())
                    .build();
            member.setEmploymentDetails(details);
        }

        Member savedMember = memberRepository.save(member);

        // ✅ Process fee using strict System Reference logic
        processRegistrationFee(savedMember, paymentMethod, userExternalRef, bankAccountCode);

        createDefaultSavingsAccount(savedMember);

        return convertToDTO(savedMember);
    }

    public MemberDTO updateProfile(UUID memberId, MemberDTO updateDTO, MultipartFile file) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member profile not found"));

        member.setPhoneNumber(updateDTO.getPhoneNumber());
        member.setAddress(updateDTO.getAddress());
        member.setKraPin(updateDTO.getKraPin());

        if (updateDTO.getBeneficiaries() != null) {
            member.getBeneficiaries().clear();
            for (BeneficiaryDTO bDto : updateDTO.getBeneficiaries()) {
                Beneficiary b = Beneficiary.builder()
                        .fullName(bDto.getFullName())
                        .relationship(bDto.getRelationship())
                        .idNumber(bDto.getIdNumber())
                        .phoneNumber(bDto.getPhoneNumber())
                        .allocation(bDto.getAllocation())
                        .build();
                member.addBeneficiary(b);
            }
        }

        if (updateDTO.getEmploymentDetails() != null) {
            EmploymentDetailsDTO eDto = updateDTO.getEmploymentDetails();
            EmploymentDetails details = member.getEmploymentDetails();
            if (details == null) {
                details = new EmploymentDetails();
                member.setEmploymentDetails(details);
            }

            if (eDto.getTerms() != null && !eDto.getTerms().isEmpty()) {
                try {
                    details.setTerms(EmploymentDetails.EmploymentTerms.valueOf(eDto.getTerms()));
                } catch (Exception e) {
                    log.warn("Invalid employment term: {}", eDto.getTerms());
                }
            }

            details.setEmployerName(eDto.getEmployerName());
            details.setStaffNumber(eDto.getStaffNumber());
            details.setStationOrDepartment(eDto.getStationOrDepartment());
            details.setGrossMonthlyIncome(eDto.getGrossMonthlyIncome());
            details.setNetMonthlyIncome(eDto.getNetMonthlyIncome());
            details.setBankName(eDto.getBankName());
            details.setBankAccountNumber(eDto.getBankAccountNumber());
        }

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "PROFILE_" + member.getMemberNumber() + "_" + UUID.randomUUID() + ".jpg";
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            member.setProfileImageUrl("profiles/" + filename);
        }

        Member saved = memberRepository.save(member);
        return convertToDTO(saved);
    }

    private void processRegistrationFee(Member member, String paymentMethodStr, String userExternalRef, String bankAccountCode) {
        double feeAmount = systemSettingService.getDouble("REGISTRATION_FEE");

        if (feeAmount > 0) {
            BigDecimal amount = BigDecimal.valueOf(feeAmount);

            // 1. Generate System Reference (The "PCM..." Code)
            String systemRef = referenceCodeService.generateReferenceCode();

            // 2. Determine Source Account
            Transaction.PaymentMethod payMethod = Transaction.PaymentMethod.MPESA;
            String sourceAccount = "1002"; // Default Paybill (MPESA)

            if ("BANK".equalsIgnoreCase(paymentMethodStr) || "BANK_TRANSFER".equalsIgnoreCase(paymentMethodStr)) {
                payMethod = Transaction.PaymentMethod.BANK;
                sourceAccount = (bankAccountCode != null && !bankAccountCode.isEmpty()) ? bankAccountCode : "1010";
            } else if ("CASH".equalsIgnoreCase(paymentMethodStr)) {
                payMethod = Transaction.PaymentMethod.CASH;
                sourceAccount = "1001"; // Cash on Hand
            }

            // 3. Post to General Ledger using SYSTEM REFERENCE
            // Debit: Source (Asset), Credit: 4001 (Registration Fee Income)
            accountingService.postEvent(
                    "REGISTRATION_FEE",
                    "Registration Fee - " + member.getMemberNumber(),
                    systemRef, // ✅ Uses System Ref for Accounting
                    amount,
                    sourceAccount, // Override Debit
                    "4001"         // Override Credit
            );

            // 4. Create Transaction Record
            // Reference Code = System Ref (PCM...)
            // External Reference = User's Code (M-Pesa)
            Transaction registrationTx = Transaction.builder()
                    .member(member)
                    .type(Transaction.TransactionType.REGISTRATION_FEE)
                    .amount(amount)
                    .paymentMethod(payMethod)
                    .referenceCode(systemRef)        // ✅ Primary Ref = System Code
                    .externalReference(userExternalRef) // ✅ Secondary Ref = M-Pesa Code
                    .description("Registration Fee")
                    .balanceAfter(BigDecimal.ZERO) // Registration fee does not affect savings balance
                    .build();

            transactionRepository.save(registrationTx);
        }
    }

    private void createDefaultSavingsAccount(Member member) {
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .member(member)
                .accountNumber(generateSavingsAccountNumber())
                .balance(BigDecimal.ZERO)
                .status(SavingsAccount.AccountStatus.ACTIVE)
                .build();
        savingsAccountRepository.save(savingsAccount);
    }

    public MemberDTO getMemberById(UUID id) {
        return memberRepository.findById(id).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public MemberDTO getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MemberDTO> getActiveMembers() {
        return memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public long getActiveMembersCount() { return memberRepository.countActiveMembers(); }

    public MemberDTO updateMember(UUID id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        member.setFirstName(memberDTO.getFirstName());
        member.setLastName(memberDTO.getLastName());
        member.setEmail(memberDTO.getEmail());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setAddress(memberDTO.getAddress());
        member.setDateOfBirth(memberDTO.getDateOfBirth());
        member.setKraPin(memberDTO.getKraPin());
        Member updatedMember = memberRepository.save(member);
        return convertToDTO(updatedMember);
    }

    public void deleteMember(UUID id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        member.setStatus(Member.MemberStatus.INACTIVE);
        memberRepository.save(member);
    }

    private String generateMemberNumber() {
        long count = memberRepository.count();
        return "MEM" + String.format("%06d", count + 1);
    }

    private String generateSavingsAccountNumber() {
        long count = savingsAccountRepository.count();
        return "SAV" + String.format("%05d", count + 1);
    }

    public MemberDTO convertToDTO(Member member) {
        MemberDTO dto = MemberDTO.builder()
                .id(member.getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .idNumber(member.getIdNumber())
                .kraPin(member.getKraPin())
                .address(member.getAddress())
                .dateOfBirth(member.getDateOfBirth())
                .profileImageUrl(member.getProfileImageUrl())
                .status(member.getStatus().toString())
                .totalShares(member.getTotalShares())
                .totalSavings(member.getTotalSavings())
                .registrationDate(member.getRegistrationDate())
                .build();

        if (member.getBeneficiaries() != null) {
            dto.setBeneficiaries(member.getBeneficiaries().stream().map(b -> BeneficiaryDTO.builder()
                    .fullName(b.getFullName())
                    .relationship(b.getRelationship())
                    .idNumber(b.getIdNumber())
                    .phoneNumber(b.getPhoneNumber())
                    .allocation(b.getAllocation())
                    .build()).collect(Collectors.toList()));
        }

        if (member.getEmploymentDetails() != null) {
            EmploymentDetails ed = member.getEmploymentDetails();
            dto.setEmploymentDetails(EmploymentDetailsDTO.builder()
                    .terms(ed.getTerms() != null ? ed.getTerms().toString() : "PERMANENT")
                    .employerName(ed.getEmployerName())
                    .staffNumber(ed.getStaffNumber())
                    .grossMonthlyIncome(ed.getGrossMonthlyIncome())
                    .netMonthlyIncome(ed.getNetMonthlyIncome())
                    .bankName(ed.getBankName())
                    .bankAccountNumber(ed.getBankAccountNumber())
                    .build());
        }

        return dto;
    }
}