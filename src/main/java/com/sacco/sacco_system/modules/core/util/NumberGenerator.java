package com.sacco.sacco_system.modules.core.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for generating unique identifiers and numbers
 */
public class NumberGenerator {
    
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis());
    
    /**
     * Generate a unique UUID string
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a sequential number (useful for account/loan/member numbers)
     */
    public static long generateSequential() {
        return counter.incrementAndGet();
    }
    
    /**
     * Generate a formatted reference number with prefix
     */
    public static String generateReferenceNumber(String prefix) {
        long number = generateSequential();
        return String.format("%s%010d", prefix, number % 10000000000L);
    }
}


