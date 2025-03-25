package com.victor.payments;

public record PaymentRequest(
    String email,
    double amount
) {
}
