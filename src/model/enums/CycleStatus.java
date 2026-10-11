package model.enums;

/**
 * Enumeração que representa os estados de um ciclo/rodada de poupança no grupo.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public enum CycleStatus {

    /** Ciclo planeado ou agendado mas ainda não iniciado. */
    PLANNED,

    /** Ciclo em andamento, a aguardar contribuições dos membros. */
    IN_PROGRESS,

    /** Ciclo encerrado com todas as contribuições liquidadas e prémio entregue. */
    CONCLUDED,

    /** Ciclo cancelado. */
    CANCELLED
}