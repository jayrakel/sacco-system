package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.MemberDTO;
import com.sacco.sacco_system.entity.*;
import com.sacco.sacco_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added Logging
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

    // ✅ REQUIRED FOR GL POSTING
    private final AccountingService accountingService;

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Use absolute path or a configured property in real prod
    private final String UPLOAD_DIR = "uploads/profiles/";

    public MemberDTO createMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String referenceCode) throws IOException {

        // 1. Check for duplicate user email
        if (userRepository.findByEmail(memberDTO.getEmail()).isPresent()) {
            throw new RuntimeException("A user with this email address already exists.");
        }

        // 2. Handle Profile Image Upload
        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            imagePath = filename;
        }

        // 3. Create Member Entity
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

        // 4. Create User Login Account
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .username(member.getEmail())
                .memberNumber(savedMember.getMemberNumber())
                .phoneNumber(member.getPhoneNumber())
                .role(User.Role.MEMBER)
                .password(passwordEncoder.encode(tempPassword))
                .mustChangePassword(true)
                .emailVerified(false)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(newUser);

        // 5. Send Welcome Email with Token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(savedUser, token);
        tokenRepository.save(verificationToken);

        // Note: Ensure emailService has this method or use a generic sendEmail
        try {
            emailService.sendMemberWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    tempPassword,
                    token
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }

        // 6. Process Registration Fee & Post to GL
        double feeAmount = 0.0;
        try {
            feeAmount = systemSettingService.getDouble("REGISTRATION_FEE");
        } catch (Exception e) {
            log.warn("Registration Fee setting not found, defaulting to 0");
        }

        if (feeAmount > 0) {
            Transaction registrationTx = Transaction.builder()
                    .member(savedMember)
                    .type(Transaction.TransactionType.REGISTRATION_FEE)
                    .amount(BigDecimal.valueOf(feeAmount))
                    .paymentMethod(Transaction.PaymentMethod.valueOf(paymentMethod))
                    .referenceCode(referenceCode)
                    .description("Registration Fee - " + referenceCode)
                    .balanceAfter(BigDecimal.ZERO)
                    .build();

            transactionRepository.save(registrationTx);

            // ✅ POST TO GENERAL LEDGER
            BigDecimal amount = BigDecimal.valueOf(feeAmount);
            String narrative = "Registration Fee - " + savedMember.getMemberNumber();
            String ref = registrationTx.getTransactionId();

            try {
                if (paymentMethod.equals("CASH")) {
                    accountingService.postDoubleEntry(narrative, ref, "1001", "4001", amount); // Debit Cash, Credit Reg Fees
                } else {
                    accountingService.postDoubleEntry(narrative, ref, "1002", "4001", amount); // Debit Bank, Credit Reg Fees
                }
            } catch (Exception e) {
                log.error("Failed to post GL entry for registration: {}", e.getMessage());
            }
        }

        // 7. Auto-Create Savings Account
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .member(savedMember)
                .accountNumber(generateSavingsAccountNumber())
                .balance(BigDecimal.ZERO)
                .status(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        savingsAccountRepository.save(savingsAccount);
        log.info("✅ Savings Account Created: {}", savingsAccount.getAccountNumber());

        return convertToDTO(savedMember);
    }

    // --- Standard CRUD Methods ---

    public MemberDTO getMemberById(UUID id) {
        return memberRepository.findById(id).map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public MemberDTO getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber).map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MemberDTO> getActiveMembers() {
        return memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public long getActiveMembersCount() { return memberRepository.countActiveMembers(); }

    public void deleteMember(UUID id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        member.setStatus(Member.MemberStatus.INACTIVE);
        memberRepository.save(member);
    }

    // In MemberService.java

    public MemberDTO updateProfile(String email, MemberDTO updateDTO, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Member member = memberRepository.findByMemberNumber(user.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member profile not found"));

        // 1. Update Allowed Fields
        member.setPhoneNumber(updateDTO.getPhoneNumber());
        member.setAddress(updateDTO.getAddress());
        member.setEmail(updateDTO.getEmail()); // Ensure email uniqueness logic if needed

        // Next of Kin
        member.setNextOfKinName(updateDTO.getNextOfKinName());
        member.setNextOfKinPhone(updateDTO.getNextOfKinPhone());
        member.setNextOfKinRelation(updateDTO.getNextOfKinRelation());

        // 2. Handle Image Upload
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String filename = "PROFILE_" + member.getMemberNumber() + "_" + UUID.randomUUID() + ".jpg";
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            member.setProfileImageUrl("profiles/" + filename);
        }

        // 3. Update User Email if changed
        if (!user.getEmail().equals(updateDTO.getEmail())) {
            if(userRepository.findByEmail(updateDTO.getEmail()).isPresent()) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(updateDTO.getEmail());
            user.setUsername(updateDTO.getEmail());
            userRepository.save(user);
        }

        Member saved = memberRepository.save(member);
        return convertToDTO(saved);
    }

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

    private String generateMemberNumber() {
        long count = memberRepository.count();
        return "MEM" + String.format("%06d", count + 1);
    }

    private String generateSavingsAccountNumber() {
        long count = savingsAccountRepository.count();
        return "SAV" + String.format("%05d", count + 1);
    }

    // ✅ FIXED: Changed from 'private' to 'public' so Controller can use it
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