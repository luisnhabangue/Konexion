package payment;

import java.time.LocalDateTime;

public record PaymentResponse(boolean success, String transactionRef,
                              String providerCode, String message, LocalDateTime timestamp) {}