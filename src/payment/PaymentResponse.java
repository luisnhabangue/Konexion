package payment;

import java.time.LocalDateTime;

/**
 * Registo que transporta o resultado do processamento de um pagamento.
 *
 * @param success Indica se a transação foi efetuada com sucesso.
 * @param transactionRef A referência da transação gerada pelo gateway (ou {@code null} se falhar).
 * @param providerCode O código de estado retornado pelo provedor.
 * @param message A mensagem explicativa do resultado do pagamento.
 * @param timestamp A data e hora em que a resposta foi processada.
 *
 * @since 1.0
 */
public record PaymentResponse(boolean success, String transactionRef,
                              String providerCode, String message, LocalDateTime timestamp) {}