package com.sacco.sacco_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // ✅ Import
import com.sacco.sacco_system.dto.MemberDTO;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // ✅ Import
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // ✅ Import

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // ✅ NEW: Get Logged-in Member Profile
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member profile not linked to this account"));

        return ResponseEntity.ok(Map.of("success", true, "data", memberService.convertToDTO(member)));
    }

    // ✅ FIXED: Accepts Multipart File + JSON String + Payment Params
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createMember(
            @RequestPart("member") String memberDtoString,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("referenceCode") String referenceCode
    ) {
        try {
            // Convert JSON string to DTO
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules(); // Handle Dates
            MemberDTO memberDTO = mapper.readValue(memberDtoString, MemberDTO.class);

            // Call service with all 4 arguments
            MemberDTO created = memberService.createMember(memberDTO, file, paymentMethod, referenceCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Member created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMemberById(@PathVariable UUID id) {
        try {
            MemberDTO member = memberService.getMemberById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", member);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMembers() {
        List<MemberDTO> members = memberService.getAllMembers();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", members);
        response.put("count", members.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveMembers() {
        List<MemberDTO> members = memberService.getActiveMembers();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", members);
        response.put("count", members.size());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMember(@PathVariable UUID id, @RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO updated = memberService.updateMember(id, memberDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Member updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMember(@PathVariable UUID id) {
        try {
            memberService.deleteMember(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/count/active")
    public ResponseEntity<Map<String, Object>> getActiveMembersCount() {
        long count = memberService.getActiveMembersCount();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("activeMembersCount", count);
        return ResponseEntity.ok(response);
    }

    // In MemberController.java

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @RequestPart("member") String memberDtoString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();

            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            MemberDTO memberDTO = mapper.readValue(memberDtoString, MemberDTO.class);

            MemberDTO updated = memberService.updateProfile(email, memberDTO, file);

            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully", "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}