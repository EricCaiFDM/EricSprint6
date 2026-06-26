package com.example.banking.api.auth;

import java.util.List;

class AuthIndex {
    private final List<String> routes;

    AuthIndex() {
        this.routes = List.of(
                "POST /auth/register",
                "POST /auth/login",
                "POST /auth/password-reset/request",
                "POST /auth/token/refresh");
    }

    List<String> routes() {
        return routes;
    }
}
