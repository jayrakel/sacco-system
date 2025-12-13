package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.MemberDTO;
import com.sacco.sacco_system.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMember(@RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO created = memberService.createMember(memberDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Member created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
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
    
    @GetMapping("/number/{memberNumber}")
    public ResponseEntity<Map<String, Object>> getMemberByNumber(@PathVariable String memberNumber) {
        try {
            MemberDTO member = memberService.getMemberByMemberNumber(memberNumber);
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
}
