package com.arovia.api.customer.application;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException() {
        super("The requested customer was not found.");
    }
}