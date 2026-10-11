package dao.interfaces;

import model.Cycle;
import java.util.List;

/**
 * Interface de contratos DAO para gestão de {@link Cycle}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public interface CycleDaoInterface {

    /**
     * Guarda um novo ciclo na base de dados.
     * @param cycle Ciclo a ser registado
     */
    void save(Cycle cycle);

    /**
     * Procura um ciclo pelo seu identificador.
     * @param id ID do ciclo
     * @return {@link Cycle} ou {@code null}
     */
    Cycle findById(Integer id);

    /**
     * Procura o ciclo atualmente em andamento de um grupo.
     * @param groupId ID do grupo
     * @return {@link Cycle} ativo ou {@code null}
     */
    Cycle findActiveCycleByGroup(Long groupId);

    /**
     * Procura todos os ciclos pertencentes a um determinado grupo.
     * @param groupId ID do grupo
     * @return Lista de ciclos
     */
    List<Cycle> findByGroupId(Long groupId);

    /**
     * Obtém o número da última rodada realizada num grupo.
     * @param groupId ID do grupo
     * @return Número do último ciclo ou 0 caso não existam ciclos
     */
    Integer findLastCycleNumberByGroup(Long groupId);

    /**
     * Atualiza os dados de um ciclo.
     * @param cycle Ciclo a atualizar
     */
    void update(Cycle cycle);

    /**
     * Elimina um ciclo da base de dados.
     * @param cycle Ciclo a eliminar
     */
    void delete(Cycle cycle);
}