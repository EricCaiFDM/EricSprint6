package com.example.banking.jobs.statements;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.config.StatementModuleConfig;
import com.example.banking.models.AccountEntity;
import com.example.banking.services.statement.StatementGenerationService;

@Component
public class MonthlyStatementGenerationJob {
    private static final Logger logger = LoggerFactory.getLogger(MonthlyStatementGenerationJob.class);

    private final StatementModuleConfig statementModuleConfig;
    private final AccountJpaRepository accountJpaRepository;
    private final StatementGenerationService statementGenerationService;

    public MonthlyStatementGenerationJob(
            StatementModuleConfig statementModuleConfig,
            AccountJpaRepository accountJpaRepository,
            StatementGenerationService statementGenerationService) {
        this.statementModuleConfig = statementModuleConfig;
        this.accountJpaRepository = accountJpaRepository;
        this.statementGenerationService = statementGenerationService;
    }

    @Scheduled(cron = "${statement.scheduler-cron:0 5 0 1 * *}", zone = "UTC")
    public void runScheduledGeneration() {
        if (!statementModuleConfig.isSchedulerEnabled()) {
            return;
        }

        YearMonth previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        List<AccountEntity> accounts = accountJpaRepository.findByDeletedAtIsNull();

        int batchSize = Math.max(1, statementModuleConfig.getGenerationBatchSize());
        for (int offset = 0; offset < accounts.size(); offset += batchSize) {
            int endIndex = Math.min(offset + batchSize, accounts.size());
            List<AccountEntity> batch = accounts.subList(offset, endIndex);
            for (AccountEntity account : batch) {
                try {
                    statementGenerationService.generateForScheduler(account.getAccountId(), previousMonth.toString());
                } catch (Exception exception) {
                    logger.warn(
                            "Monthly statement generation failed accountId={} period={} reason={}",
                            account.getAccountId(),
                            previousMonth,
                            exception.getMessage());
                }
            }
        }
    }
}
