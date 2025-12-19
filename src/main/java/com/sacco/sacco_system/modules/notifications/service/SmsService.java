package com.sacco.sacco_system.modules.notifications.service;

import lombok.extern.slf4j.Slf4j; // ✅ Import
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j // ✅ Annotation
public class SmsService {

    @Async
    public void sendSms(String phoneNumber, String message) {
        log.info(">> 📱 SMS SENT to {}: {}", phoneNumber, message); // ✅ Replaced Sysout
    }
}