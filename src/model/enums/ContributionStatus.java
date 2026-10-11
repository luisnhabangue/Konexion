package model.enums;

/**
 * Enumeração que representa os diferentes estados de uma contribuição/pagamento.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public enum ContributionStatus {

    /** Pagamento efetuado e confirmado com sucesso. */
    PAID,

    /** Pagamento pendente de liquidação pelo membro. */
    PENDING,

    /** Pagamento em atraso em relação à data limite. */
    OVERDUE,

    /** Pagamento cancelado ou anulado. */
    CANCELLED
}