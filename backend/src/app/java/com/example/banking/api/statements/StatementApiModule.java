package com.example.banking.api.statements;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.banking.api.statements.routes.StatementGenerationController;
import com.example.banking.api.statements.routes.StatementQueryController;
import com.example.banking.api.statements.routes.StatementRetrievalController;

@Configuration
@Import({
        StatementGenerationController.class,
        StatementRetrievalController.class,
        StatementQueryController.class
})
public class StatementApiModule {
}
