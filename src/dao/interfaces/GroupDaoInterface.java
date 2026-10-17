package dao.interfaces;

import model.Group;
import java.util.List;

/**
 * Interface de operações DAO para a entidade {@link Group}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public interface GroupDaoInterface {

    /**
     * Procura um grupo pelo seu ID.
     * @param id Identificador do grupo
     * @return {@link Group} encontrado ou {@code null}
     */
    Group findById(Long id);

    /**
     * Procura um grupo pelo seu nome.
     * @param name Nome do grupo
     * @return {@link Group} encontrado ou {@code null}
     */
    Group findByName(String name);

    /**
     * Retorna todos os grupos registados.
     * @return Lista de grupos
     */
    List<Group> findAll();

    List<Group> findAllForUser(Long userId);

    Group findByIdWithMembers(Long id);

    /**
     * Atualiza as informações de um grupo.
     * @param group Grupo a atualizar
     */
    void updateGroup(Group group);

    /**
     * Elimina um grupo da base de dados.
     * @param group Grupo a eliminar
     */
    void deleteGroup(Group group);

    /**
     * Persiste um novo grupo.
     * @param group Grupo a criar
     */
    void createGroup(Group group);
}