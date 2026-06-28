package com.example.banking.api.standingorders;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.banking.api.standingorders.routes.CreateStandingOrderRoute;
import com.example.banking.api.standingorders.routes.ListExecutionsRoute;
import com.example.banking.api.standingorders.routes.ListStandingOrdersRoute;
import com.example.banking.api.standingorders.routes.StandingOrderLifecycleRoute;
import com.example.banking.api.standingorders.routes.UpdateStandingOrderRoute;

@Configuration
@Import({
        CreateStandingOrderRoute.class,
        UpdateStandingOrderRoute.class,
        StandingOrderLifecycleRoute.class,
        ListExecutionsRoute.class,
        ListStandingOrdersRoute.class
})
public class StandingOrderApiModule {
}
