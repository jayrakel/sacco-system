package com.sacco.sacco_system.modules.member.domain.service;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.member.api.dto.MemberResponse;
import com.sacco.sacco_system.modules.member.domain.service.MemberService;

import com.sacco.sacco_system.modules.member.api.dto.MemberDTO;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;

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

    @Value("${app.upload.dir:uploads/profiles/}")
    private String uploadDir;

    //  ADDED AUDIT LOGGING
    public MemberDTO createMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String referenceCode, String bankAccountCode) throws IOException {

        // ✅ Check for duplicate email
        if (memberRepository.findByEmail(memberDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email " + memberDTO.getEmail() + " is already registered");
        }

        // ✅ Check for duplicate phone number
        if (memberRepository.findByPhoneNumber(memberDTO.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number " + memberDTO.getPhoneNumber() + " is already registered");
        }

        // ✅ Check for duplicate ID number
        if (memberRepository.findByIdNumber(memberDTO.getIdNumber()).isPresent()) {
            throw new RuntimeException("ID number " + memberDTO.getIdNumber() + " is already registered");
        }

        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            imagePath = "profiles/" + filename;
        }

        String memberNumber = generateMemberNumber();

        // Create Member entity
        Member member = Member.builder()
                .memberNumber(memberNumber)
                .firstName(memberDTO.getFirstName())
                .lastName(memberDTO.getLastName())
                .email(memberDTO.getEmail())
                .phoneNumber(memberDTO.getPhoneNumber())
                .idNumber(memberDTO.getIdNumber())
                .kraPin(memberDTO.getKraPin())
                .nextOfKinName(memberDTO.getNextOfKinName())
                .nextOfKinPhone(memberDTO.getNextOfKinPhone())
                .nextOfKinRelation(memberDTO.getNextOfKinRelation())
                .address(memberDTO.getAddress())
                .dateOfBirth(memberDTO.getDateOfBirth())
                .profileImageUrl(imagePath)
                .status(Member.MemberStatus.ACTIVE)
                .totalShares(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .build();

        Member savedMember = memberRepository.save(member);

        double feeAmount = 0.0;
        try {
            feeAmount = systemSettingService.getDouble("REGISTRATION_FEE");
        } catch (Exception e) {
            log.warn("Registration Fee setting not found, defaulting to 0");
        }

        if (feeAmount > 0) {
            BigDecimal amount = BigDecimal.valueOf(feeAmount);
            
            // Create transaction record for registration fee
            Transaction registrationTx = Transaction.builder()
                    .member(savedMember)
                    .type(Transaction.TransactionType.REGISTRATION_FEE)
                    .amount(amount)
                    .paymentMethod(Transaction.PaymentMethod.valueOf(paymentMethod))
                    .referenceCode(referenceCode)
                    .description("Registration Fee - " + savedMember.getMemberNumber())
                    .balanceAfter(BigDecimal.ZERO)
                    .build();

            Transaction saved = transactionRepository.save(registrationTx);

            String paymentSuffix = paymentMethod.toUpperCase();

            if ("BANK_TRANSFER".equals(paymentSuffix) && bankAccountCode != null && !bankAccountCode.isEmpty()) {

            String narrative = "Registration Fee - " + savedMember.getMemberNumber();
            accountingService.postDoubleEntry(narrative, saved.getTransactionId(), bankAccountCode, "4001", amount);
            } else {
                // For standard methods (CASH, MPESA), use the System Mappings
                 String eventName = "REGISTRATION_FEE_" + paymentSuffix;
                 String narrative = "Registration Fee - " + savedMember.getMemberNumber();

            try {
                     // This looks up "REGISTRATION_FEE_CASH" in the database to find "1001" automatically
                     accountingService.postEvent(eventName, narrative, saved.getTransactionId(), amount);
                 } catch (Exception e) {
                     log.error("Accounting Error: Mapping not found for event {}", eventName);
                     // Optional: Fail safely or throw exception depending on policy
                 }
             }
                        
        }

        SavingsAccount savingsAccount = SavingsAccount.builder()
                .member(savedMember)
                .accountNumber(generateSavingsAccountNumber())
                .balance(BigDecimal.ZERO)
                .status(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        savingsAccountRepository.save(savingsAccount);
        log.info("âœ… Savings Account Created: {}", savingsAccount.getAccountNumber());

        return convertToDTO(savedMember);
    }

    // Update member profile by member ID (no User dependency)
    public MemberDTO updateProfile(UUID memberId, MemberDTO updateDTO, MultipartFile file) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member profile not found"));

        member.setPhoneNumber(updateDTO.getPhoneNumber());
        member.setAddress(updateDTO.getAddress());

        if (updateDTO.getIdNumber() != null && !updateDTO.getIdNumber().equals(member.getIdNumber())) {
            if (memberRepository.findByIdNumber(updateDTO.getIdNumber()).isPresent()) {
                throw new RuntimeException("ID Number " + updateDTO.getIdNumber() + " is already in use.");
            }
            member.setIdNumber(updateDTO.getIdNumber());
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(member.getEmail())) {
            if (memberRepository.findByEmail(updateDTO.getEmail()).isPresent()) {
                throw new RuntimeException("Email " + updateDTO.getEmail() + " is already in use.");
            }
            member.setEmail(updateDTO.getEmail());
        }

        member.setNextOfKinName(updateDTO.getNextOfKinName());
        member.setNextOfKinPhone(updateDTO.getNextOfKinPhone());
        member.setNextOfKinRelation(updateDTO.getNextOfKinRelation());

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

    // ADDED AUDIT LOGGING
    public MemberDTO updateMember(UUID id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));

        member.setFirstName(memberDTO.getFirstName());
        member.setLastName(memberDTO.getLastName());
        member.setEmail(memberDTO.getEmail());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setAddress(memberDTO.getAddress());
        member.setDateOfBirth(memberDTO.getDateOfBirth());
        member.setKraPin(memberDTO.getKraPin());
        member.setNextOfKinName(memberDTO.getNextOfKinName());
        member.setNextOfKinPhone(memberDTO.getNextOfKinPhone());
        member.setNextOfKinRelation(memberDTO.getNextOfKinRelation());

        Member updatedMember = memberRepository.save(member);
        return convertToDTO(updatedMember);
    }

    // ADDED AUDIT LOGGING
    public void deleteMember(UUID id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        member.setStatus(Member.MemberStatus.INACTIVE);
        memberRepository.save(member);
    }

    // ... Standard Getters/Helpers (No Changes Needed) ...
    public MemberDTO getMemberById(UUID id) { return memberRepository.findById(id).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found")); }
    public MemberDTO getMemberByMemberNumber(String memberNumber) { return memberRepository.findByMemberNumber(memberNumber).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Member not found")); }
    public List<MemberDTO> getAllMembers() { return memberRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public List<MemberDTO> getActiveMembers() { return memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public long getActiveMembersCount() { return memberRepository.countActiveMembers(); }
    private String generateMemberNumber() { long count = memberRepository.count(); return "MEM" + String.format("%06d", count + 1); }
    private String generateSavingsAccountNumber() { long count = savingsAccountRepository.count(); return "SAV" + String.format("%05d", count + 1); }

    public MemberDTO convertToDTO(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .idNumber(member.getIdNumber())
                .kraPin(member.getKraPin())
                .nextOfKinName(member.getNextOfKinName())
                .nextOfKinPhone(member.getNextOfKinPhone())
                .nextOfKinRelation(member.getNextOfKinRelation())
                .profileImageUrl(member.getProfileImageUrl())
                .address(member.getAddress())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus().toString())
                .totalShares(member.getTotalShares())
                .totalSavings(member.getTotalSavings())
                .registrationDate(member.getRegistrationDate())
                .build();
    }
}



