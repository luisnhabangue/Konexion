package payment;

import java.math.BigDecimal;

public record PaymentRequest(String account, BigDecimal amount, String reference, String description) {}