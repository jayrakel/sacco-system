package com.sacco.sacco_system.modules.member.domain.service;

import com.sacco.sacco_system.modules.core.util.ValidationUtils;
import com.sacco.sacco_system.modules.member.api.dto.CreateMemberRequest;
import com.sacco.sacco_system.modules.member.api.dto.UpdateMemberRequest;
import org.springframework.stereotype.Component;
import com.sacco.sacco_system.modules.member.domain.entity.Member;

/**
 * Validator for Member requests
 */
@Component
public class MemberValidator {
    
    public void validateCreateRequest(CreateMemberRequest request) {
        ValidationUtils.validateNotEmpty(request.getFirstName(), "firstName");
        ValidationUtils.validateNotEmpty(request.getLastName(), "lastName");
        ValidationUtils.validateNotEmpty(request.getEmail(), "email");
        ValidationUtils.validateEmail(request.getEmail());
        ValidationUtils.validateNotEmpty(request.getPhone(), "phone");
        ValidationUtils.validateNotEmpty(request.getIdNumber(), "idNumber");
    }
    
    public void validateUpdateRequest(UpdateMemberRequest request) {
        if (request.getFirstName() != null) {
            ValidationUtils.validateNotEmpty(request.getFirstName(), "firstName");
        }
        if (request.getLastName() != null) {
            ValidationUtils.validateNotEmpty(request.getLastName(), "lastName");
        }
        if (request.getPhone() != null) {
            ValidationUtils.validateNotEmpty(request.getPhone(), "phone");
        }
    }
}




