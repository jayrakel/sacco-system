package com.sacco.sacco_system.modules.finance.domain.service;

import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyDataImportService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final TransactionRepository transactionRepository;

    // Cache to avoid "Method Not Found" errors in Repositories
    private Map<String, Member> memberCache = new HashMap<>();
    private SavingsProduct defaultSavingsProduct;
    private LoanProduct defaultLoanProduct;

    // CSV Headers Mapping
    private static final int COL_NAME = 1;
    private static final int COL_LOAN_FACE_VALUE = 2;
    private static final int COL_WEEKLY_SAVINGS = 8;
    private static final int COL_LOAN_PAID = 11;
    private static final int COL_OTHER_PAYMENTS = 15;
    private static final int COL_REMARKS = 16;

    @Transactional
    public void seedFromFolder(String folderPath) {
        log.info("Starting legacy data seeding from: {}", folderPath);
        preloadCache();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            List<Path> sortedFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String filename = p.toString().toLowerCase();
                        return filename.endsWith(".csv") || filename.endsWith(".xlsx") || filename.endsWith(".xls");
                    })
                    .filter(p -> !p.toString().contains("SAMPLE"))
                    .sorted(Comparator.comparing(this::extractDateFromFilename))
                    .toList();

            log.info("Found {} files. Processing...", sortedFiles.size());

            for (Path file : sortedFiles) {
                processFile(file);
            }
        } catch (Exception e) {
            log.error("Seeding failed", e);
            throw new RuntimeException("Seeding failed: " + e.getMessage());
        }
        log.info("Data seeding completed.");
    }

    /**
     * Import data from an uploaded file (Excel or CSV)
     * @param inputStream The file input stream
     * @param filename Original filename (to detect file type)
     * @param date The date to associate with transactions (optional, will try to extract from filename)
     */
    @Transactional
    public void importFromUpload(InputStream inputStream, String filename, LocalDate date) {
        log.info("Starting data import from uploaded file: {}", filename);
        preloadCache();

        try {
            if (date == null) {
                // Try to extract date from filename
                date = extractDateFromString(filename);
            }

            String lowerFilename = filename.toLowerCase();

            if (lowerFilename.endsWith(".csv")) {
                processUploadedCSV(inputStream, date);
            } else if (lowerFilename.endsWith(".xlsx")) {
                processUploadedExcel(inputStream, date, true);
            } else if (lowerFilename.endsWith(".xls")) {
                processUploadedExcel(inputStream, date, false);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Only CSV, XLS, and XLSX are supported.");
            }

            log.info("Data import completed successfully.");
        } catch (Exception e) {
            log.error("Import failed for file: " + filename, e);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    private void processUploadedCSV(InputStream inputStream, LocalDate date) throws Exception {
        try (CSVReader reader = new CSVReader(new java.io.InputStreamReader(inputStream))) {
            List<String[]> rows = reader.readAll();
            for (String[] row : rows) {
                if (!isDataRow(row)) continue;

                String rawName = row[COL_NAME];
                if (rawName == null || rawName.trim().isEmpty()) continue;

                Member member = findOrCreateMember(rawName, date);
                processSavings(row, member, date);
                processLoans(row, member, date);
                processOtherPayments(row, member, date);
            }
        }
    }

    private void processUploadedExcel(InputStream inputStream, LocalDate date, boolean isXLSX) throws Exception {
        Workbook workbook = isXLSX ? new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0); // Process first sheet

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row

            String[] rowData = extractRowData(row);
            if (!isDataRow(rowData)) continue;

            String rawName = rowData[COL_NAME];
            if (rawName == null || rawName.trim().isEmpty()) continue;

            Member member = findOrCreateMember(rawName, date);
            processSavings(rowData, member, date);
            processLoans(rowData, member, date);
            processOtherPayments(rowData, member, date);
        }

        workbook.close();
    }

    private void preloadCache() {
        // Load all members into memory to avoid finding by name in DB (which caused errors)
        memberRepository.findAll().forEach(m -> {
            String key = (m.getFirstName() + " " + m.getLastName()).trim().toUpperCase();
            memberCache.put(key, m);
        });

        // Ensure default products exist
        defaultSavingsProduct = savingsProductRepository.findAll().stream().findFirst().orElseGet(() -> {
            SavingsProduct p = new SavingsProduct();
            p.setProductName("Ordinary Savings");
            p.setProductCode("SAV001");
            p.setCurrencyCode("KES");
            p.setActive(true);
            return savingsProductRepository.save(p);
        });

        defaultLoanProduct = loanProductRepository.findAll().stream().findFirst().orElseGet(() -> {
            LoanProduct p = new LoanProduct();
            p.setProductName("Development Loan");
            p.setProductCode("LOAN001");
            p.setInterestRate(BigDecimal.valueOf(10.0));
            p.setMaxAmount(BigDecimal.valueOf(1000000));
            p.setMaxDurationWeeks(52);
            p.setCurrencyCode("KES");
            p.setActive(true);
            return loanProductRepository.save(p);
        });
    }

    private void processFile(Path filePath) {
        LocalDate date = extractDateFromFilename(filePath);
        log.info("Processing: {} (Date: {})", filePath.getFileName(), date);

        String filename = filePath.toString().toLowerCase();

        try {
            if (filename.endsWith(".csv")) {
                processCSVFile(filePath, date);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                processExcelFile(filePath, date);
            }
        } catch (Exception e) {
            log.error("Error processing file: " + filePath, e);
        }
    }

    private void processCSVFile(Path filePath, LocalDate date) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            List<String[]> rows = reader.readAll();
            for (String[] row : rows) {
                if (!isDataRow(row)) continue;

                String rawName = row[COL_NAME];
                if (rawName == null || rawName.trim().isEmpty()) continue;

                Member member = findOrCreateMember(rawName, date);
                processSavings(row, member, date);
                processLoans(row, member, date);
                processOtherPayments(row, member, date);
            }
        }
    }

    private void processExcelFile(Path filePath, LocalDate date) throws Exception {
        try (InputStream is = new FileInputStream(filePath.toFile())) {
            Workbook workbook = null;

            // Determine workbook type based on file extension
            if (filePath.toString().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else {
                workbook = new HSSFWorkbook(is);
            }

            Sheet sheet = workbook.getSheetAt(0); // Process first sheet

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                String[] rowData = extractRowData(row);
                if (!isDataRow(rowData)) continue;

                String rawName = rowData[COL_NAME];
                if (rawName == null || rawName.trim().isEmpty()) continue;

                Member member = findOrCreateMember(rawName, date);
                processSavings(rowData, member, date);
                processLoans(rowData, member, date);
                processOtherPayments(rowData, member, date);
            }

            workbook.close();
        }
    }

    private String[] extractRowData(Row row) {
        List<String> cells = new ArrayList<>();

        // Extract up to COL_REMARKS + 1 columns
        for (int i = 0; i <= Math.max(COL_REMARKS, row.getLastCellNum()); i++) {
            Cell cell = row.getCell(i);
            cells.add(getCellValueAsString(cell));
        }

        return cells.toArray(new String[0]);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    // Format numbers without scientific notation
                    double value = cell.getNumericCellValue();
                    if (value == (long) value) {
                        yield String.valueOf((long) value);
                    } else {
                        yield String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }

    private boolean isDataRow(String[] row) {
        if (row.length < 5) return false;
        try {
            Double.parseDouble(row[0].trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Member findOrCreateMember(String rawName, LocalDate date) {
        String cleanName = rawName.replace("\n", " ").replaceAll("\\s+", " ").trim().toUpperCase();

        if (memberCache.containsKey(cleanName)) {
            return memberCache.get(cleanName);
        }

        String[] parts = cleanName.split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Member";

        Member newMember = new Member();
        newMember.setFirstName(firstName);
        newMember.setLastName(lastName);
        newMember.setMemberNumber("MEM-" + System.nanoTime());
        newMember.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@legacy.local");
        newMember.setPhoneNumber("+254700" + String.format("%06d", System.nanoTime() % 1000000));
        newMember.setNationalId("ID-" + System.nanoTime());
        newMember.setMemberStatus(Member.MemberStatus.ACTIVE);
        newMember.setActive(true);

        Member saved = memberRepository.save(newMember);
        memberCache.put(cleanName, saved);
        return saved;
    }

    private void processSavings(String[] row, Member member, LocalDate date) {
        BigDecimal amount = parseAmount(row[COL_WEEKLY_SAVINGS]);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {

            // Find account in memory filter to avoid Repo errors
            SavingsAccount account = savingsAccountRepository.findAll().stream()
                    .filter(a -> a.getMember().equals(member) && a.getProduct().equals(defaultSavingsProduct))
                    .findFirst()
                    .orElseGet(() -> {
                        SavingsAccount acc = new SavingsAccount();
                        acc.setMember(member);
                        acc.setProduct(defaultSavingsProduct);
                        acc.setBalanceAmount(BigDecimal.ZERO);
                        acc.setAccountNumber("SAV-" + member.getMemberNumber());
                        acc.setActive(true);
                        return savingsAccountRepository.save(acc);
                    });

            account.setBalanceAmount(account.getBalanceAmount().add(amount));
            savingsAccountRepository.save(account);

            recordTransaction(amount, "SAVINGS_DEPOSIT", "Weekly Savings", date);
        }
    }

    private void processLoans(String[] row, Member member, LocalDate date) {
        BigDecimal paidAmount = parseAmount(row[COL_LOAN_PAID]);
        BigDecimal faceValue = parseAmount(row[COL_LOAN_FACE_VALUE]);

        if (faceValue.compareTo(BigDecimal.ZERO) > 0) {
            boolean hasActiveLoan = loanRepository.findAll().stream()
                    .anyMatch(l -> l.getMember().equals(member)); // Simplified check

            if (!hasActiveLoan) {
                Loan newLoan = new Loan();
                newLoan.setMember(member);
                newLoan.setProduct(defaultLoanProduct);
                newLoan.setPrincipalAmount(faceValue);
                newLoan.setOutstandingPrincipal(faceValue);
                newLoan.setLoanStatus(Loan.LoanStatus.ACTIVE);
                newLoan.setDisbursementDate(date);
                newLoan.setLoanNumber("LOAN-" + System.nanoTime());
                newLoan.setInterestRate(BigDecimal.valueOf(10.0)); // Default 10%
                loanRepository.save(newLoan);
            }
        }

        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Find loan and reduce balance logic here
            recordTransaction(paidAmount, "LOAN_REPAYMENT", "Weekly Repayment", date);
        }
    }

    private void processOtherPayments(String[] row, Member member, LocalDate date) {
        BigDecimal otherAmount = parseAmount(row[COL_OTHER_PAYMENTS]);
        String remarks = row.length > COL_REMARKS ? row[COL_REMARKS] : "Other";

        if (otherAmount.compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(otherAmount, "CHARGE", remarks, date);
        }
    }

    private void recordTransaction(BigDecimal amount, String type, String description, LocalDate date) {
        Transaction txn = new Transaction();
        txn.setAmount(amount);
        txn.setTransactionDate(date.atStartOfDay());
        txn.setDescription(description);
        txn.setReferenceCode("LEGACY-" + System.nanoTime()); // Auto-generate reference
        transactionRepository.save(txn);
    }

    private LocalDate extractDateFromFilename(Path path) {
        return extractDateFromString(path.getFileName().toString());
    }

    private LocalDate extractDateFromString(String filename) {
        try {
            String datePart = filename.substring(filename.lastIndexOf("-") + 1, filename.lastIndexOf(".")).trim();
            datePart = datePart.toUpperCase()
                    .replace("SEPT", "SEP").replace("JULY", "JUL")
                    .replace("JUNE", "JUN").replace("MARCH", "MAR").replace("APRIL", "APR");

            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("[dd MMM yy][dd MMM yyyy]")
                    .toFormatter(Locale.ENGLISH);

            return LocalDate.parse(datePart, formatter);
        } catch (Exception e) {
            return LocalDate.now(); // Default to today if parsing fails
        }
    }

    private BigDecimal parseAmount(String val) {
        if (val == null || val.trim().isEmpty() || val.trim().equals("-")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim().replace(",", "").replace("\"", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}