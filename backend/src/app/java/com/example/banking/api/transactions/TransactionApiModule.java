package com.example.banking.api.transactions;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;

import com.example.banking.api.transactions.routes.DepositRoute;
import com.example.banking.api.transactions.routes.HistoryRoute;
import com.example.banking.api.transactions.routes.TransferRoute;
import com.example.banking.api.transactions.routes.WithdrawalRoute;

@Configuration
@Import({DepositRoute.class, WithdrawalRoute.class, TransferRoute.class, HistoryRoute.class})
public class TransactionApiModule {
}
