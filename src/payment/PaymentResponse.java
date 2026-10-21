package payment;

import java.time.LocalDateTime;

/**
 * Registo que transporta o resultado do processamento de um pagamento[cite: 4].
 *
 * @param success Indica se a transação foi efetuada com sucesso[cite: 4].
 * @param transactionRef A referência da transação gerada pelo gateway (ou {@code null} se falhar)[cite: 4].
 * @param providerCode O código de estado retornado pelo provedor[cite: 4].
 * @param message A mensagem explicativa do resultado do pagamento[cite: 4].
 * @param timestamp A data e hora em que a resposta foi processada[cite: 4].
 *
 * @since 1.0[cite: 4]
 */
public record PaymentResponse(boolean success, String transactionRef,
                              String providerCode, String message, LocalDateTime timestamp) {}