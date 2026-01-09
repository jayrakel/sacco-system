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
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;

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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingService systemSettingService;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final AccountingService accountingService;
    private final ReferenceCodeService referenceCodeService;

    @Value("${app.upload.dir:uploads/profiles/}")
    private String uploadDir;

    public MemberDTO createMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String userExternalRef, String bankAccountCode, User user) throws IOException {

        if (memberRepository.findByEmail(memberDTO.getEmail()).isPresent()) throw new RuntimeException("Email already registered");
        if (memberRepository.findByPhoneNumber(memberDTO.getPhoneNumber()).isPresent()) throw new RuntimeException("Phone already registered");
        if (memberRepository.findByNationalId(memberDTO.getNationalId()).isPresent()) throw new RuntimeException("National ID already registered");

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
                .nationalId(memberDTO.getNationalId())
                .kraPin(memberDTO.getKraPin())
                .address(memberDTO.getAddress())
                .dateOfBirth(memberDTO.getDateOfBirth())
                .profileImageUrl(imagePath)
                .memberStatus(Member.MemberStatus.ACTIVE)
                .totalShares(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .beneficiaries(new ArrayList<>())
                .build();

        if (memberDTO.getBeneficiaries() != null) {
            for (BeneficiaryDTO bDto : memberDTO.getBeneficiaries()) {
                Beneficiary beneficiary = Beneficiary.builder()
                        .firstName(bDto.getFirstName())
                        .lastName(bDto.getLastName())
                        .relationship(bDto.getRelationship())
                        .identityNumber(bDto.getIdentityNumber())
                        .phoneNumber(bDto.getPhoneNumber())
                        .allocationPercentage(bDto.getAllocationPercentage())
                        .build();
                member.addBeneficiary(beneficiary);
            }
        }

        if (memberDTO.getEmploymentDetails() != null) {
            EmploymentDetailsDTO eDto = memberDTO.getEmploymentDetails();
            EmploymentDetails.EmploymentTerms employmentTerms = EmploymentDetails.EmploymentTerms.PERMANENT;
            try {
                if(eDto.getEmploymentTerms() != null) employmentTerms = EmploymentDetails.EmploymentTerms.valueOf(eDto.getEmploymentTerms());
            } catch (Exception e) {
                // ✅ FIX: Logged the exception instead of silent failure
                log.warn("Invalid employment term provided: {}. Defaulting to PERMANENT.", eDto.getEmploymentTerms());
            }

            EmploymentDetails details = EmploymentDetails.builder()
                    .employmentTerms(employmentTerms)
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

        processRegistrationFee(savedMember, paymentMethod, userExternalRef, bankAccountCode);

        // ✅ Updated to Lazy Load default product
        createDefaultSavingsAccount(savedMember);

        return convertToDTO(savedMember);
    }

    private void createDefaultSavingsAccount(Member member) {
        // 1. Try to find the standard default product
        SavingsProduct defaultProduct = savingsProductRepository.findByProductCode("SAV001").orElse(null);

        // 2. If it doesn't exist, and NO products exist, create it now using SYSTEM SETTINGS
        if (defaultProduct == null && savingsProductRepository.count() == 0) {
            defaultProduct = createSystemDefaultProduct();
        }
        // 3. Fallback: If "SAV001" missing but others exist, use the first available one
        else if (defaultProduct == null) {
            List<SavingsProduct> all = savingsProductRepository.findAll();
            // ✅ FIX: Used getFirst()
            if (!all.isEmpty()) defaultProduct = all.getFirst();
        }

        if (defaultProduct == null) {
            log.warn("❌ CRITICAL: No savings product found. Cannot create savings account for member {}", member.getMemberNumber());
            return;
        }

        SavingsAccount savingsAccount = SavingsAccount.builder()
                .member(member)
                .product(defaultProduct)
                .accountNumber(generateSavingsAccountNumber())
                .balanceAmount(BigDecimal.ZERO)
                .totalDeposits(BigDecimal.ZERO)
                .totalWithdrawals(BigDecimal.ZERO)
                .accountStatus(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        savingsAccountRepository.save(savingsAccount);
        log.info("✅ Created default savings account {} for member {} under product {}",
                savingsAccount.getAccountNumber(), member.getMemberNumber(), defaultProduct.getProductName());
    }

    private SavingsProduct createSystemDefaultProduct() {
        log.info("⚙️ First Member Detected: Initializing Default Savings Product from System Settings...");

        // ✅ Get value from System Settings (seeded in DataInitializer)
        String minDepositStr = systemSettingService.getString("min_weekly_deposit");
        BigDecimal minBalance = (minDepositStr != null) ? new BigDecimal(minDepositStr) : new BigDecimal("500.00");

        SavingsProduct newProduct = SavingsProduct.builder()
                .productCode("SAV001")
                .productName("Recurring Savings")
                .description("Default savings account (Auto-generated from System Config)")
                .currencyCode("KES")
                .type(SavingsProduct.ProductType.SAVINGS)
                .interestRate(new BigDecimal("5.00"))
                .minBalance(minBalance) // ✅ Used System Configuration
                .minDurationMonths(0)
                .allowWithdrawal(true)
                .active(true)
                .build();

        return savingsProductRepository.save(newProduct);
    }

    // ... (Existing methods kept)

    // ✅ FIX: Suppressed warning if this is used by Controller via Spring Reflection
    @SuppressWarnings("unused")
    public MemberDTO getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    // ... (Rest of file including convertToDTO, updateProfile etc.)

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
                        .firstName(bDto.getFirstName())
                        .lastName(bDto.getLastName())
                        .relationship(bDto.getRelationship())
                        .identityNumber(bDto.getIdentityNumber())
                        .phoneNumber(bDto.getPhoneNumber())
                        .allocationPercentage(bDto.getAllocationPercentage())
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

            if (eDto.getEmploymentTerms() != null && !eDto.getEmploymentTerms().isEmpty()) {
                try {
                    details.setEmploymentTerms(EmploymentDetails.EmploymentTerms.valueOf(eDto.getEmploymentTerms()));
                } catch (Exception e) {
                    log.warn("Invalid employment term: {}", eDto.getEmploymentTerms());
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
            String systemRef = referenceCodeService.generateReferenceCode();

            Transaction.PaymentMethod payMethod = Transaction.PaymentMethod.MPESA;
            String sourceAccount = "1002"; // Default Paybill (MPESA)

            if ("BANK".equalsIgnoreCase(paymentMethodStr) || "BANK_TRANSFER".equalsIgnoreCase(paymentMethodStr)) {
                payMethod = Transaction.PaymentMethod.BANK;
                if (bankAccountCode != null && !bankAccountCode.isEmpty()) {
                    sourceAccount = bankAccountCode;
                } else {
                    sourceAccount = systemSettingService.getString("DEFAULT_BANK_GL_CODE", "1010");
                }
            } else if ("CASH".equalsIgnoreCase(paymentMethodStr)) {
                payMethod = Transaction.PaymentMethod.CASH;
                sourceAccount = "1001";
            }

            accountingService.postEvent(
                    "REGISTRATION_FEE",
                    "Registration Fee - " + member.getMemberNumber(),
                    systemRef,
                    amount,
                    sourceAccount,
                    "4001"
            );

            Transaction registrationTx = Transaction.builder()
                    .member(member)
                    .type(Transaction.TransactionType.REGISTRATION_FEE)
                    .amount(amount)
                    .paymentMethod(payMethod)
                    .referenceCode(systemRef)
                    .externalReference(userExternalRef)
                    .description("Registration Fee")
                    .balanceAfter(BigDecimal.ZERO)
                    .build();

            transactionRepository.save(registrationTx);
        }
    }

    public MemberDTO getMemberById(UUID id) {
        return memberRepository.findById(id).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll().stream().map(this::convertToDTO).toList(); // ✅ FIX: toList()
    }

    public List<MemberDTO> getActiveMembers() {
        return memberRepository.findByMemberStatus(Member.MemberStatus.ACTIVE).stream().map(this::convertToDTO).toList(); // ✅ FIX: toList()
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
        member.setMemberStatus(Member.MemberStatus.EXITED);
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
                .nationalId(member.getNationalId())
                .kraPin(member.getKraPin())
                .address(member.getAddress())
                .dateOfBirth(member.getDateOfBirth())
                .profileImageUrl(member.getProfileImageUrl())
                .memberStatus(member.getMemberStatus() != null ? member.getMemberStatus().toString() : "ACTIVE")
                .totalShares(member.getTotalShares())
                .totalSavings(member.getTotalSavings())
                .membershipDate(member.getMembershipDate())
                .build();

        if (member.getBeneficiaries() != null) {
            dto.setBeneficiaries(member.getBeneficiaries().stream().map(b -> BeneficiaryDTO.builder()
                    .firstName(b.getFirstName())
                    .lastName(b.getLastName())
                    .relationship(b.getRelationship())
                    .identityNumber(b.getIdentityNumber())
                    .phoneNumber(b.getPhoneNumber())
                    .allocationPercentage(b.getAllocationPercentage())
                    .build()).toList()); // ✅ FIX: toList()
        }

        if (member.getEmploymentDetails() != null) {
            EmploymentDetails ed = member.getEmploymentDetails();
            dto.setEmploymentDetails(EmploymentDetailsDTO.builder()
                    .employmentTerms(ed.getEmploymentTerms() != null ? ed.getEmploymentTerms().toString() : "PERMANENT")
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