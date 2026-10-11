package dao.interfaces;

import model.UserGroup;
import java.util.List;

/**
 * Interface de contratos de persistência para a relação {@link UserGroup}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public interface UserGroupInterface {

    /**
     * Associa um utilizador a um grupo.
     * @param userGroup Associação a guardar
     */
    void save(UserGroup userGroup);

    /**
     * Verifica se determinado utilizador pertence a um grupo.
     * @param userId ID do utilizador
     * @param groupId ID do grupo
     * @return {@code true} se o utilizador pertence ao grupo; {@code false} caso contrário
     */
    boolean isUserInGroup(Long userId, Long groupId);

    /**
     * Conta o número total de membros num grupo.
     * @param groupId ID do grupo
     * @return Quantidade de membros
     */
    int countMembersInGroup(Long groupId);

    /**
     * Procura todos os vínculos de membros de um determinado grupo.
     * @param groupId ID do grupo
     * @return Lista de {@link UserGroup}
     */
    List<UserGroup> findByGroupId(Long groupId);

    /**
     * Procura o vínculo específico entre um utilizador e um grupo.
     * @param userId ID do utilizador
     * @param groupId ID do grupo
     * @return {@link UserGroup} encontrado ou {@code null}
     */
    UserGroup findByUserAndGroup(Long userId, Long groupId);

    /**
     * Atualiza os dados da associação membro-grupo.
     * @param userGroup Vínculo a atualizar
     */
    void update(UserGroup userGroup);
}