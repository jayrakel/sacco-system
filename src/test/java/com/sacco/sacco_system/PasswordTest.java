package com.sacco.sacco_system;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "S@cc0_.Adm!n123";
        String encoded = encoder.encode(rawPassword);
        
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("Encoded: " + encoded);
        System.out.println("Matches: " + encoder.matches(rawPassword, encoded));
        
        // Test with another encoding to see if it matches
        String encoded2 = encoder.encode(rawPassword);
        System.out.println("Encoded2: " + encoded2);
        System.out.println("Matches with first: " + encoder.matches(rawPassword, encoded));
        System.out.println("Matches with second: " + encoder.matches(rawPassword, encoded2));
    }
}
