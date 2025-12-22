package com.sacco.sacco_system.modules.finance.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Centralized Reference Code Generator
 * Generates unique, non-guessable reference codes for all financial transactions
 * 
 * Format: PCM7K9H2R5X (11 characters, M-Pesa style)
 * Example: PCM7K9H2R5X
 * 
 * Components:
 * - Date Encoded (3 chars): PCM
 *   - P = Year offset from 2000 in Base36 (2025-2000=25='P')
 *   - C = Month in Base36 (12='C')
 *   - M = Day in Base36 (22='M')
 * - Random (8 chars): 7K9H2R5X (cryptographically secure)
 * 
 * Benefits:
 * - Compact and easy to communicate verbally
 * - Sortable by date (year/month/day)
 * - Non-guessable (random suffix)
 * - No separators (easier to type)
 * 
 * These reference codes are displayed in:
 * - Audit logs
 * - Transaction records
 * - Financial ledgers
 * - All money operations across the system
 */
@Service
@Slf4j
public class ReferenceCodeService {

    private static final int RANDOM_LENGTH = 8;
    private static final int BASE_YEAR = 2000; // Year offset base
    // Using alphanumeric chars excluding confusing ones: 0, O, 1, I
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate next unique reference code
     * Thread-safe using synchronized and SecureRandom for cryptographic randomness
     * 
     * @return Unique reference code (e.g., PCM7K9H2R5X)
     */
    @Transactional
    public synchronized String generateReferenceCode() {
        LocalDateTime now = LocalDateTime.now();
        
        // Encode date in Base36 (3 characters)
        String dateEncoded = encodeDateBase36(now);
        
        // Generate cryptographically secure random suffix (8 characters)
        String randomSuffix = generateRandomString(RANDOM_LENGTH);
        
        // Combine: PCM7K9H2R5X (11 chars total)
        String referenceCode = dateEncoded + randomSuffix;
        
        log.debug("Generated reference code: {} (Date: {}-{}-{})", 
                  referenceCode, now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        return referenceCode;
    }
    
    /**
     * Encode date as 3-character Base36 string
     * Format: YMD where Y=year offset, M=month, D=day
     * Example: 2025-12-22 → PCM (P=25, C=12, M=22)
     */
    private String encodeDateBase36(LocalDateTime dateTime) {
        int yearOffset = dateTime.getYear() - BASE_YEAR; // 2025 - 2000 = 25
        int month = dateTime.getMonthValue(); // 1-12
        int day = dateTime.getDayOfMonth(); // 1-31
        
        // Convert to Base36 (0-9, A-Z)
        char yearChar = Character.forDigit(yearOffset, 36);
        char monthChar = Character.forDigit(month, 36);
        char dayChar = Character.forDigit(day, 36);
        
        return String.valueOf(yearChar).toUpperCase() 
             + String.valueOf(monthChar).toUpperCase()
             + String.valueOf(dayChar).toUpperCase();
    }

    /**
     * Generates a cryptographically secure random alphanumeric string
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * Validates if a reference code matches the expected format
     * 
     * @param referenceCode The reference code to validate
     * @return true if valid format, false otherwise
     */
    public boolean isValidReferenceCode(String referenceCode) {
        if (referenceCode == null || referenceCode.isEmpty()) {
            return false;
        }
        
        // Check format: YMD + 8 random chars = 11 total chars
        // Pattern: 3 alphanumeric (date) + 8 alphanumeric (random)
        String pattern = "^[A-Z2-9]{11}$";
        return referenceCode.matches(pattern);
    }

    /**
     * Get current counter value (for admin purposes)
     * NOTE: Not applicable for random-based reference codes
     * Kept for backward compatibility
     */
    @Deprecated
    public long getCurrentCounter() {
        log.warn("getCurrentCounter() is deprecated - reference codes are now timestamp+random based");
        return 0L;
    }

    /**
     * Reset counter (DANGEROUS - only for testing/maintenance)
     * NOTE: Not applicable for random-based reference codes
     * Kept for backward compatibility
     */
    @Deprecated
    @Transactional
    public void resetCounter(long startValue) {
        log.warn("resetCounter() is deprecated - reference codes are now timestamp+random based");
    }
}