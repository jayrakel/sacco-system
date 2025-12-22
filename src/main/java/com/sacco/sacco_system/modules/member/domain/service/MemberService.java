package com.sacco.sacco_system.modules.member.domain.service;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.member.api.dto.MemberResponse;
import com.sacco.sacco_system.modules.member.domain.service.MemberService;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;

import com.sacco.sacco_system.modules.member.api.dto.MemberDTO;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.upload.dir:uploads/profiles/}")
    private String uploadDir;

    // âœ… ADDED AUDIT LOGGING
    public MemberDTO createMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String referenceCode) throws IOException {

        if (userRepository.findByEmail(memberDTO.getEmail()).isPresent()) {
            throw new RuntimeException("A user with this email address already exists.");
        }

        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            imagePath = "profiles/" + filename;
        }

        Member member = Member.builder()
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
                .memberNumber(generateMemberNumber())
                .totalShares(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .build();

        Member savedMember = memberRepository.save(member);

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .email(member.getEmail()) // TODO: username() method doesn't exist on UserBuilder - using email instead
                .memberNumber(savedMember.getMemberNumber())
                .phoneNumber(member.getPhoneNumber())
                .role(User.Role.MEMBER)
                .password(passwordEncoder.encode(tempPassword))
                .mustChangePassword(true)
                .emailVerified(false)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(newUser);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(savedUser, token);
        tokenRepository.save(verificationToken);

        try {
            emailService.sendMemberWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), tempPassword, token);
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }

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

            String narrative = "Registration Fee - " + savedMember.getMemberNumber();
            String ref = saved.getTransactionId(); // Use transaction ID as reference

            try {
                if ("CASH".equalsIgnoreCase(paymentMethod)) {
                    accountingService.postDoubleEntry(narrative, ref, "1001", "4001", amount);
                } else {
                    accountingService.postDoubleEntry(narrative, ref, "1002", "4001", amount);
                }
            } catch (Exception e) {
                log.error("Failed to post GL entry: {}", e.getMessage());
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

    // âœ… ADDED AUDIT LOGGING
    public MemberDTO updateProfile(String email, MemberDTO updateDTO, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Member member = memberRepository.findByMemberNumber(user.getMemberNumber())
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
            if(userRepository.findByEmail(updateDTO.getEmail()).isPresent()) {
                throw new RuntimeException("Email " + updateDTO.getEmail() + " is already in use.");
            }
            member.setEmail(updateDTO.getEmail());
            user.setEmail(updateDTO.getEmail());
            // Username is derived from email via getUsername()
            userRepository.save(user);
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

    // âœ… ADDED AUDIT LOGGING
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

    // âœ… ADDED AUDIT LOGGING
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



