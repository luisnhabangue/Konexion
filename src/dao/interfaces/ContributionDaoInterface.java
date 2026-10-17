package dao.interfaces;

import model.Contribution;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface que especifica as operações de acesso a dados de {@link Contribution}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public interface ContributionDaoInterface {

    /**
     * Regista uma nova contribuição.
     * @param contribution Contribuição a guardar
     */
    void save(Contribution contribution);

    /**
     * Procura uma contribuição pelo ID.
     * @param id ID da contribuição
     * @return {@link Contribution} encontrada ou {@code null}
     */
    Contribution findById(Long id);

    /**
     * Procura o registo de pagamento de um membro num ciclo específico.
     * @param userGroupId ID da associação membro-grupo
     * @param cycleId ID do ciclo
     * @return {@link Contribution} encontrada ou {@code null}
     */
    Contribution findByUserGroupAndCycle(Long userGroupId, Integer cycleId);

    /**
     * Lista todas as contribuições efetuadas num determinado ciclo.
     * @param cycleId ID do ciclo
     * @return Lista de contribuições
     */
    List<Contribution> findByCycleId(Integer cycleId);

    /**
     * Lista o histórico de contribuições de um determinado membro.
     * @param userGroupId ID da associação membro-grupo
     * @return Lista de contribuições
     */
    List<Contribution> findByUserGroupId(Long userGroupId);

    /**
     * Calcula o montante total já arrecadado/pago num determinado ciclo.
     * @param cycleId ID do ciclo
     * @return Valor total somado
     */
    BigDecimal sumTotalPaidByCycle(Integer cycleId);

    /**
     * Atualiza os dados de uma contribuição.
     * @param contribution Contribuição a atualizar
     */
    void update(Contribution contribution);

    /**
     * Elimina uma contribuição da base de dados.
     * @param contribution Contribuição a eliminar
     */
    void delete(Contribution contribution);
}