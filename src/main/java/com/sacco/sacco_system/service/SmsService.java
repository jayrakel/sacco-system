package com.sacco.sacco_system.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    // Plug in Africa's Talking / Twilio here later
    @Async
    public void sendSms(String phoneNumber, String message) {
        // Logic to validate phone number (e.g., ensure +254 format)
        System.out.println(">> 📱 SMS SENT to " + phoneNumber + ": " + message);
    }
}