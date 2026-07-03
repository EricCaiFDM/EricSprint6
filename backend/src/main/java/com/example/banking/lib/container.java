package com.example.banking.lib;

class Container {
    private final Object authIndex;

    Container(Object authIndex) {
        this.authIndex = authIndex;
    }

    Object authIndex() {
        return authIndex;
    }
}
