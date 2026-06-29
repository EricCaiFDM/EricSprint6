package com.example.banking.jobs.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.config.StatementModuleConfig;
import com.example.banking.models.AccountEntity;
import com.example.banking.services.statement.StatementGenerationService;

class MonthlyStatementGenerationJobTest {

    @Test
    void runScheduledGenerationSkipsWhenSchedulerDisabled() {
        StatementModuleConfig config = new StatementModuleConfig();
        config.setSchedulerEnabled(false);

        AccountJpaRepository accountJpaRepository = accountRepository(List.of(account("acc-1")));
        CapturingStatementGenerationService generationService = new CapturingStatementGenerationService();

        MonthlyStatementGenerationJob job = new MonthlyStatementGenerationJob(config, accountJpaRepository, generationService);
        job.runScheduledGeneration();

        assertTrue(generationService.calls.isEmpty());
    }

    @Test
    void runScheduledGenerationProcessesAccountsInBatches() {
        StatementModuleConfig config = new StatementModuleConfig();
        config.setSchedulerEnabled(true);
        config.setGenerationBatchSize(2);

        AccountJpaRepository accountJpaRepository = accountRepository(List.of(
                account("acc-1"),
                account("acc-2"),
                account("acc-3")));
        CapturingStatementGenerationService generationService = new CapturingStatementGenerationService();

        MonthlyStatementGenerationJob job = new MonthlyStatementGenerationJob(config, accountJpaRepository, generationService);
        job.runScheduledGeneration();

        String previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1).toString();
        assertEquals(3, generationService.calls.size());
        assertTrue(generationService.calls.contains("acc-1|" + previousMonth));
        assertTrue(generationService.calls.contains("acc-2|" + previousMonth));
        assertTrue(generationService.calls.contains("acc-3|" + previousMonth));
    }

    private AccountJpaRepository accountRepository(List<AccountEntity> accounts) {
        return (AccountJpaRepository) Proxy.newProxyInstance(
                AccountJpaRepository.class.getClassLoader(),
                new Class<?>[] { AccountJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByDeletedAtIsNull".equals(method.getName())) {
                        return accounts;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    return null;
                });
    }

    private AccountEntity account(String accountId) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        return account;
    }

    private static final class CapturingStatementGenerationService extends StatementGenerationService {
        private final List<String> calls = new ArrayList<>();

        private CapturingStatementGenerationService() {
            super(null, null, null, null, null, new StatementModuleConfig());
        }

        @Override
        public void generateForScheduler(String accountId, String periodYearMonth) {
            calls.add(accountId + "|" + periodYearMonth);
        }
    }
}
