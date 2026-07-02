package com.example.banking.api.statements.routes;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.statements.StatementResponseMapper;
import com.example.banking.api.statements.schemas.StatementResponseSchema;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.services.AccountRepository;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.CustomerRepository;
import com.example.banking.services.TransactionRepository;
import com.example.banking.services.statement.StatementAuthorizationService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Statements")
@RestController
@RequestMapping("/statements")
@Validated
public class StatementRetrievalController {
    private static final ZoneId PDF_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(PDF_ZONE);
    private static final DateTimeFormatter PDF_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StatementAuthorizationService statementAuthorizationService;
    private final CustomerPrincipalResolver principalResolver;
    private final StatementResponseMapper statementResponseMapper;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public StatementRetrievalController(
            StatementAuthorizationService statementAuthorizationService,
            CustomerPrincipalResolver principalResolver,
            StatementResponseMapper statementResponseMapper,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {
        this.statementAuthorizationService = statementAuthorizationService;
        this.principalResolver = principalResolver;
        this.statementResponseMapper = statementResponseMapper;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Operation(
            summary = "Get statement by id",
            description = "Returns statement metadata, status, and summary details for a single statement identifier.")
        @ApiResponse(
            responseCode = "200",
            description = "Statement details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StatementResponseSchema.class),
                examples = @ExampleObject(value = "{\"statementId\":\"8d8a1415-b894-4cc3-ac6e-4f6c9836d5a2\",\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"periodYearMonth\":\"2026-07\",\"status\":\"READY\",\"artifactVersion\":1,\"periodStartUtc\":\"2026-07-01T00:00:00Z\",\"periodEndUtc\":\"2026-08-01T00:00:00Z\",\"generatedAtUtc\":\"2026-07-31T23:59:59Z\"}")))
    @GetMapping("/{statementId}")
    public ResponseEntity<StatementResponseSchema> getById(
            @PathVariable
            @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "statementId must be a UUID")
            String statementId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        MonthlyStatement statement = statementAuthorizationService.readStatementById(
                statementId,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(statementResponseMapper.toResponse(statement));
    }

        @Operation(
            summary = "Download statement artifact",
            description = "Downloads the generated PDF artifact for the requested statement and artifact version.")
        @ApiResponse(
            responseCode = "200",
            description = "PDF statement artifact",
            content = @Content(
                mediaType = MediaType.APPLICATION_PDF_VALUE,
                schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{statementId}/artifact/v{artifactVersion}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadArtifact(
            @PathVariable
            @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "statementId must be a UUID")
            String statementId,
            @PathVariable
            @Min(value = 1, message = "artifactVersion must be greater than 0")
            int artifactVersion,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        MonthlyStatement statement = statementAuthorizationService.readStatementById(
                statementId,
                principal.userId(),
                principal.role());

        if (statement.getArtifactVersion() == null || statement.getArtifactVersion() != artifactVersion) {
            throw StatementErrors.notFound(statementId);
        }

        List<TransactionEntity> statementTransactions = transactionRepository.findAccountTransactionsForPeriod(
            statement.getAccountId(),
            statement.getPeriodStartUtc(),
            statement.getPeriodEndUtc());

        AccountEntity account = accountRepository.findActiveById(statement.getAccountId()).orElse(null);
        CustomerEntity customer = account == null
            ? null
            : customerRepository.findActiveById(account.getCustomerId()).orElse(null);

        byte[] artifact = renderStatementPdf(statement, statementTransactions, account, customer);
        String fileName = buildArtifactFileName(statement, artifactVersion);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(artifact);
    }

    private byte[] renderStatementPdf(
            MonthlyStatement statement,
            List<TransactionEntity> statementTransactions,
            AccountEntity account,
            CustomerEntity customer) {
        String contentStream = buildStatementContentStream(statement, statementTransactions, account, customer);
        byte[] contentBytes = contentStream.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        offsets.add(writeAscii(output, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
        offsets.add(writeAscii(output, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
        offsets.add(writeAscii(output,
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>\nendobj\n"));
        offsets.add(writeAscii(output, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"));
        offsets.add(writeAscii(output, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n"));

        int contentObjectOffset = output.size();
        writeAscii(output, "6 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
        output.write(contentBytes, 0, contentBytes.length);
        writeAscii(output, "\nendstream\nendobj\n");
        offsets.add(contentObjectOffset);

        int xrefOffset = output.size();
        writeAscii(output, "xref\n0 7\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (int offset : offsets) {
            writeAscii(output, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        writeAscii(output, "trailer\n<< /Size 7 /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xrefOffset + "\n%%EOF\n");

        return output.toByteArray();
    }

    private String buildStatementContentStream(
            MonthlyStatement statement,
            List<TransactionEntity> statementTransactions,
            AccountEntity account,
            CustomerEntity customer) {
        String currencyCode = safeText(statement.getCurrencyCode());
        String accountName = resolveAccountName(account);
        String customerName = safeText(customer == null ? null : customer.getLegalName());
        String statementPeriod = formatStatementPeriodRange(statement);
        String generatedDate = formatGeneratedDate(statement.getGeneratedAtUtc());
        BigDecimal openingBalance = safeAmountValue(statement.getOpeningBalance());
        BigDecimal closingBalance = safeAmountValue(statement.getClosingBalance());
        BigDecimal netMovement = closingBalance.subtract(openingBalance);
        List<TransactionEntity> transactions = statementTransactions == null ? List.of() : statementTransactions;

        StringBuilder builder = new StringBuilder();
        builder.append("0.18 g\n");
        builder.append("36 728 540 44 re f\n");
        builder.append("0.95 g\n");
        builder.append("36 688 540 18 re f\n");
        builder.append("36 516 540 18 re f\n");
        builder.append("0.92 g\n");
        builder.append("36 500 540 16 re f\n");
        builder.append("0.97 g\n");
        builder.append("36 146 540 56 re f\n");

        builder.append("0.72 G\n");
        builder.append("0.8 w\n");
        appendRectangleStroke(builder, 36, 548, 540, 160);
        appendRectangleStroke(builder, 36, 212, 540, 326);
        appendRectangleStroke(builder, 36, 146, 540, 56);

        builder.append("0.88 G\n");
        builder.append("0.5 w\n");
        appendHorizontalRule(builder, 36, 576, 688);
        appendHorizontalRule(builder, 36, 576, 516);
        appendHorizontalRule(builder, 36, 576, 500);
        appendVerticalRule(builder, 318, 548, 688);
        appendVerticalRule(builder, 146, 212, 500);
        appendVerticalRule(builder, 286, 212, 500);
        appendVerticalRule(builder, 406, 212, 500);
        appendVerticalRule(builder, 496, 212, 500);

        builder.append("1 g\n");
        appendText(builder, "F2", 18, 50, 748, "NorthBridge Bank");
        appendText(builder, "F1", 10, 50, 734, "Official Monthly Statement");
        appendText(builder, "F1", 9, 332, 748, "Statement Ref: " + formatStatementReference(statement.getStatementId()));

        builder.append("0 g\n");
        appendText(builder, "F2", 11, 50, 694, "Account Summary");
        appendText(builder, "F1", 10, 50, 668, "Customer Name: " + customerName);
        appendText(builder, "F1", 10, 50, 650, "Account Name: " + accountName);
        appendText(builder, "F1", 10, 50, 632, "Statement Period: " + statementPeriod);
        appendText(builder, "F1", 10, 50, 614, "Currency: " + currencyCode);

        appendText(builder, "F1", 10, 332, 668, "Opening Balance: " + formatMoney(openingBalance, currencyCode));
        appendText(builder, "F1", 10, 332, 650, "Closing Balance: " + formatMoney(closingBalance, currencyCode));
        appendText(builder, "F1", 10, 332, 632, "Net Activity: " + formatSignedMoney(netMovement, currencyCode));
        appendText(builder, "F1", 10, 332, 614, "Generated Date: " + generatedDate);

        appendText(builder, "F2", 11, 50, 522, "Statement Transactions");
        appendText(builder, "F2", 9, 50, 504, "Date");
        appendText(builder, "F2", 9, 152, 504, "Type");
        appendText(builder, "F2", 9, 292, 504, "Amount");
        appendText(builder, "F2", 9, 412, 504, "Balance");
        appendText(builder, "F2", 9, 502, 504, "Txn Ref");

        int rowY = 486;
        int maxRows = 16;
        int rendered = 0;
        for (TransactionEntity transaction : transactions) {
            if (rendered >= maxRows) {
                break;
            }
            appendText(builder, "F1", 8, 50, rowY, formatTransactionDate(transaction.getPostedAtUtc()));
            appendText(builder, "F1", 8, 152, rowY, toReadableTransactionType(transaction.getTransactionType()));
            appendText(builder, "F1", 8, 292, rowY,
                    formatSignedMoney(calculateSignedAmount(transaction), safeText(transaction.getCurrencyCode())));
            appendText(builder, "F1", 8, 412, rowY,
                    formatMoney(safeAmountValue(transaction.getBalanceAfter()), safeText(transaction.getCurrencyCode())));
            appendText(builder, "F1", 8, 502, rowY, shortenTransactionId(transaction.getTransactionId()));
            rowY -= 16;
            rendered += 1;
        }

        if (transactions.isEmpty()) {
            appendText(builder, "F1", 9, 50, 484, "No transactions were posted in this statement period.");
        } else if (transactions.size() > rendered) {
            appendText(builder, "F1", 8, 50, 244,
                    "Showing first " + rendered + " of " + transactions.size() + " transactions in this period.");
        }

        appendText(builder, "F2", 11, 50, 186, "Important Information");
        appendText(builder, "F1", 9, 50, 168,
            "This statement is an official record of your account activity for the period shown above.");
        appendText(builder, "F1", 9, 50, 154,
                "If you spot discrepancies, contact NorthBridge support within 30 days of statement delivery.");

        appendText(builder, "F1", 9, 50, 132, "NorthBridge Bank | Secure digital statement");
        return builder.toString();
    }

    private void appendRectangleStroke(StringBuilder builder, int x, int y, int width, int height) {
        builder.append(x).append(' ')
                .append(y).append(' ')
                .append(width).append(' ')
                .append(height).append(" re S\n");
    }

    private void appendHorizontalRule(StringBuilder builder, int startX, int endX, int y) {
        builder.append(startX).append(' ')
                .append(y).append(" m ")
                .append(endX).append(' ')
                .append(y).append(" l S\n");
    }

    private void appendVerticalRule(StringBuilder builder, int x, int startY, int endY) {
        builder.append(x).append(' ')
                .append(startY).append(" m ")
                .append(x).append(' ')
                .append(endY).append(" l S\n");
    }

    private void appendText(StringBuilder builder, String fontAlias, int fontSize, int x, int y, String text) {
        builder.append("BT\n");
        builder.append('/').append(fontAlias).append(' ').append(fontSize).append(" Tf\n");
        builder.append(x).append(' ').append(y).append(" Td\n");
        builder.append('(').append(escapePdfText(text)).append(") Tj\n");
        builder.append("ET\n");
    }

    private int writeAscii(ByteArrayOutputStream output, String value) {
        int offset = output.size();
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        output.write(bytes, 0, bytes.length);
        return offset;
    }

    private String buildArtifactFileName(MonthlyStatement statement, int artifactVersion) {
        String period = safeText(statement.getPeriodYearMonth()).replaceAll("[^0-9-]", "");
        if (period.isBlank()) {
            period = "statement";
        }
        return "statement-" + period + "-v" + artifactVersion + ".pdf";
    }

    private String escapePdfText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.trim();
    }

    private String safeAmount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal safeAmountValue(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal amount, String currencyCode) {
        return safeAmount(amount) + " " + safeText(currencyCode);
    }

    private String formatSignedMoney(BigDecimal amount, String currencyCode) {
        BigDecimal normalized = safeAmountValue(amount);
        String prefix = normalized.signum() >= 0 ? "+" : "-";
        return prefix + safeAmount(normalized.abs()) + " " + safeText(currencyCode);
    }

    private BigDecimal calculateSignedAmount(TransactionEntity transaction) {
        BigDecimal amount = safeAmountValue(transaction == null ? null : transaction.getAmount());
        TransactionType type = transaction == null ? null : transaction.getTransactionType();
        if (type == TransactionType.WITHDRAWAL || type == TransactionType.TRANSFER_DEBIT) {
            return amount.negate();
        }
        return amount;
    }

    private String formatTransactionDate(Instant value) {
        return formatLocalDate(value);
    }

    private String formatStatementPeriodRange(MonthlyStatement statement) {
        String yearMonth = safeText(statement == null ? null : statement.getPeriodYearMonth());
        if (statement == null) {
            return yearMonth;
        }

        try {
            YearMonth period = YearMonth.parse(yearMonth);
            return PDF_PERIOD_DATE_FORMAT.format(period.atDay(1))
                    + "-"
                    + PDF_PERIOD_DATE_FORMAT.format(period.atEndOfMonth());
        } catch (DateTimeParseException ignored) {
            // Fall through to the stored UTC boundaries when periodYearMonth is not parseable.
        }

        if (statement.getPeriodStartUtc() == null || statement.getPeriodEndUtc() == null) {
            return yearMonth;
        }

        Instant startUtc = statement.getPeriodStartUtc();
        Instant endExclusiveUtc = statement.getPeriodEndUtc();
        Instant endInclusiveUtc = endExclusiveUtc.isAfter(startUtc)
                ? endExclusiveUtc.minusMillis(1)
                : endExclusiveUtc;

        return PDF_PERIOD_DATE_FORMAT.format(startUtc.atZone(ZoneOffset.UTC).toLocalDate())
            + "-"
            + PDF_PERIOD_DATE_FORMAT.format(endInclusiveUtc.atZone(ZoneOffset.UTC).toLocalDate());
    }

    private String formatLocalDate(Instant value) {
        if (value == null) {
            return "N/A";
        }
        return PDF_DATE_FORMAT.format(value);
    }

    private String formatGeneratedDate(Instant value) {
        if (value == null) {
            return "N/A";
        }
        return PDF_PERIOD_DATE_FORMAT.format(value.atZone(PDF_ZONE).toLocalDate());
    }

    private String resolveAccountName(AccountEntity account) {
        if (account == null) {
            return "N/A";
        }

        if (account.getNickname() != null && !account.getNickname().isBlank()) {
            return safeText(account.getNickname());
        }

        String type = safeText(account.getAccountType());
        Integer checkingNumber = account.getCheckingNumber();
        if (checkingNumber != null && checkingNumber > 0) {
            return type + " #" + checkingNumber;
        }

        return type + " Account";
    }

    private String toReadableTransactionType(TransactionType type) {
        if (type == null) {
            return "Transaction";
        }
        return switch (type) {
            case DEPOSIT -> "Deposit";
            case WITHDRAWAL -> "Withdrawal";
            case TRANSFER_DEBIT -> "Transfer Debit";
            case TRANSFER_CREDIT -> "Transfer Credit";
        };
    }

    private String shortenTransactionId(String value) {
        String transactionId = safeText(value);
        if ("N/A".equals(transactionId) || transactionId.length() <= 12) {
            return transactionId;
        }
        return transactionId.substring(0, 8) + "...";
    }

    private String formatStatementReference(String value) {
        String statementReference = safeText(value);
        if ("N/A".equals(statementReference) || statementReference.length() <= 24) {
            return statementReference;
        }

        return statementReference.substring(0, 12)
                + "..."
                + statementReference.substring(statementReference.length() - 8);
    }

}

