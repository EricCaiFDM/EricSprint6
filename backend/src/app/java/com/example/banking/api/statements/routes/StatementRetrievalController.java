package com.example.banking.api.statements.routes;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.statement.StatementAuthorizationService;

@RestController
@RequestMapping("/statements")
@Validated
public class StatementRetrievalController {
    private final StatementAuthorizationService statementAuthorizationService;
    private final CustomerPrincipalResolver principalResolver;
    private final StatementResponseMapper statementResponseMapper;

    public StatementRetrievalController(
            StatementAuthorizationService statementAuthorizationService,
            CustomerPrincipalResolver principalResolver,
            StatementResponseMapper statementResponseMapper) {
        this.statementAuthorizationService = statementAuthorizationService;
        this.principalResolver = principalResolver;
        this.statementResponseMapper = statementResponseMapper;
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

        byte[] artifact = renderStatementPdf(statement);
        String fileName = buildArtifactFileName(statement, artifactVersion);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(artifact);
    }

    private byte[] renderStatementPdf(MonthlyStatement statement) {
        List<String> lines = new ArrayList<>();
        lines.add("NorthBridge Monthly Statement");
        lines.add("Statement ID: " + safeText(statement.getStatementId()));
        lines.add("Account ID: " + safeText(statement.getAccountId()));
        lines.add("Period: " + safeText(statement.getPeriodYearMonth()));
        lines.add("Version: " + safeVersion(statement.getArtifactVersion()));
        lines.add("Opening Balance: " + safeAmount(statement.getOpeningBalance()) + " " + safeText(statement.getCurrencyCode()));
        lines.add("Closing Balance: " + safeAmount(statement.getClosingBalance()) + " " + safeText(statement.getCurrencyCode()));
        lines.add("Status: " + safeText(statement.getStatus() == null ? null : statement.getStatus().name()));
        lines.add("Generated At: " + safeInstant(statement.getGeneratedAtUtc()));

        String contentStream = toPdfTextStream(lines);
        byte[] contentBytes = contentStream.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        offsets.add(writeAscii(output, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
        offsets.add(writeAscii(output, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
        offsets.add(writeAscii(output,
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"));
        offsets.add(writeAscii(output, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"));

        int contentObjectOffset = output.size();
        writeAscii(output, "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
        output.write(contentBytes, 0, contentBytes.length);
        writeAscii(output, "\nendstream\nendobj\n");
        offsets.add(contentObjectOffset);

        int xrefOffset = output.size();
        writeAscii(output, "xref\n0 6\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (int offset : offsets) {
            writeAscii(output, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        writeAscii(output, "trailer\n<< /Size 6 /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xrefOffset + "\n%%EOF\n");

        return output.toByteArray();
    }

    private String toPdfTextStream(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        builder.append("BT\n");
        builder.append("/F1 12 Tf\n");
        builder.append("50 760 Td\n");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                builder.append("0 -18 Td\n");
            }
            builder.append('(').append(escapePdfText(lines.get(index))).append(") Tj\n");
        }
        builder.append("ET\n");
        return builder.toString();
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
        return value.toPlainString();
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
