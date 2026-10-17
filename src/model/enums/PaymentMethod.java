package model.enums;

/**
 * Enumeração que representa os métodos de pagamento suportados para efetuar contribuições.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public enum PaymentMethod {

    /** Pagamento efetuado via M-Pesa. */
    MPESA,

    /** Pagamento efetuado via e-Mola. */
    EMOLA,

    /** Pagamento efetuado via transferência ou depósito bancário. */
    BANK,

    /** Pagamento em numerário. */
    CASH
}