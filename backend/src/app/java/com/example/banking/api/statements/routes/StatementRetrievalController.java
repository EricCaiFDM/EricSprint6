package com.example.banking.api.statements.routes;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.TransactionRepository;
import com.example.banking.services.statement.StatementAuthorizationService;

@RestController
@RequestMapping("/statements")
@Validated
public class StatementRetrievalController {
    private final StatementAuthorizationService statementAuthorizationService;
    private final CustomerPrincipalResolver principalResolver;
    private final StatementResponseMapper statementResponseMapper;
    private final TransactionRepository transactionRepository;

    public StatementRetrievalController(
            StatementAuthorizationService statementAuthorizationService,
            CustomerPrincipalResolver principalResolver,
            StatementResponseMapper statementResponseMapper,
            TransactionRepository transactionRepository) {
        this.statementAuthorizationService = statementAuthorizationService;
        this.principalResolver = principalResolver;
        this.statementResponseMapper = statementResponseMapper;
        this.transactionRepository = transactionRepository;
    }

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

        byte[] artifact = renderStatementPdf(statement, statementTransactions);
        String fileName = buildArtifactFileName(statement, artifactVersion);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(artifact);
    }

    private byte[] renderStatementPdf(MonthlyStatement statement, List<TransactionEntity> statementTransactions) {
        String contentStream = buildStatementContentStream(statement, statementTransactions);
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

    private String buildStatementContentStream(MonthlyStatement statement, List<TransactionEntity> statementTransactions) {
        String status = safeText(statement.getStatus() == null ? null : statement.getStatus().name());
        String generatedAt = safeInstant(statement.getGeneratedAtUtc());
        String currencyCode = safeText(statement.getCurrencyCode());
        BigDecimal openingBalance = safeAmountValue(statement.getOpeningBalance());
        BigDecimal closingBalance = safeAmountValue(statement.getClosingBalance());
        BigDecimal netMovement = closingBalance.subtract(openingBalance);
        List<TransactionEntity> transactions = statementTransactions == null ? List.of() : statementTransactions;

        StringBuilder builder = new StringBuilder();
        builder.append("0.94 g\n");
        builder.append("36 728 540 44 re f\n");

        builder.append("0.80 G\n");
        builder.append("0.8 w\n");
        appendRectangleStroke(builder, 36, 548, 540, 160);
        appendRectangleStroke(builder, 36, 486, 540, 52);
        appendRectangleStroke(builder, 36, 212, 540, 264);
        appendRectangleStroke(builder, 36, 146, 540, 56);

        builder.append("0.88 G\n");
        builder.append("0.5 w\n");
        appendHorizontalRule(builder, 36, 576, 692);
        appendHorizontalRule(builder, 36, 576, 502);
        appendHorizontalRule(builder, 36, 576, 458);
        appendHorizontalRule(builder, 36, 576, 442);
        appendVerticalRule(builder, 318, 548, 692);
        appendVerticalRule(builder, 146, 212, 442);
        appendVerticalRule(builder, 286, 212, 442);
        appendVerticalRule(builder, 406, 212, 442);
        appendVerticalRule(builder, 496, 212, 442);

        builder.append("0 g\n");
        appendText(builder, "F2", 18, 50, 748, "NorthBridge Bank");
        appendText(builder, "F1", 11, 50, 734, "Monthly Account Statement");
        appendText(builder, "F1", 10, 380, 748, "Status: " + status);
        appendText(builder, "F1", 10, 380, 734, "Generated: " + generatedAt);

        appendText(builder, "F2", 11, 50, 700, "Account Summary");
        appendText(builder, "F1", 10, 50, 674, "Statement ID: " + safeText(statement.getStatementId()));
        appendText(builder, "F1", 10, 50, 656, "Account ID: " + safeText(statement.getAccountId()));
        appendText(builder, "F1", 10, 50, 638, "Statement Period: " + safeText(statement.getPeriodYearMonth()));
        appendText(builder, "F1", 10, 50, 620, "Artifact Version: " + safeVersion(statement.getArtifactVersion()));

        appendText(builder, "F1", 10, 332, 674, "Currency: " + currencyCode);
        appendText(builder, "F1", 10, 332, 656, "Opening Balance: " + formatMoney(openingBalance, currencyCode));
        appendText(builder, "F1", 10, 332, 638, "Closing Balance: " + formatMoney(closingBalance, currencyCode));
        appendText(builder, "F1", 10, 332, 620, "Net Movement: " + formatSignedMoney(netMovement, currencyCode));

        appendText(builder, "F2", 11, 50, 516, "Balance Snapshot");
        appendText(builder, "F1", 10, 50, 496, "Opening this period: " + formatMoney(openingBalance, currencyCode));
        appendText(builder, "F1", 10, 332, 496, "Closing this period: " + formatMoney(closingBalance, currencyCode));

        appendText(builder, "F2", 11, 50, 462, "Statement Transactions");
        appendText(builder, "F2", 9, 50, 446, "Date");
        appendText(builder, "F2", 9, 152, 446, "Type");
        appendText(builder, "F2", 9, 292, 446, "Amount");
        appendText(builder, "F2", 9, 412, 446, "Balance");
        appendText(builder, "F2", 9, 502, 446, "Txn Ref");

        int rowY = 428;
        int maxRows = 13;
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
            appendText(builder, "F1", 9, 50, 426, "No transactions were posted in this statement period.");
        } else if (transactions.size() > rendered) {
            appendText(builder, "F1", 8, 50, 220,
                    "Showing first " + rendered + " of " + transactions.size() + " transactions in this period.");
        }

        appendText(builder, "F2", 11, 50, 186, "Important Information");
        appendText(builder, "F1", 9, 50, 168,
                "This statement reflects posted transactions within month-end UTC boundaries for the selected period.");
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
        if (value == null) {
            return "N/A";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC)
                .format(value);
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

    private String safeVersion(Integer value) {
        if (value == null || value < 1) {
            return "1";
        }
        return Integer.toString(value);
    }

    private String safeInstant(Instant value) {
        if (value == null) {
            return "N/A";
        }
        return value.toString();
    }
}
