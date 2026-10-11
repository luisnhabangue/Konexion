package dao.interfaces;

import model.User;
import java.util.List;

/**
 * Interface que define os contratos de acesso a dados para a entidade {@link User}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public interface UserDaoInterface {

    /**
     * Persiste um novo utilizador na base de dados.
     * @param user Utilizador a criar
     */
    void createUser(User user);

    /**
     * Procura um utilizador pelo seu ID.
     * @param id Identificador do utilizador
     * @return {@link User} se encontrado, ou {@code null}
     */
    User findById(Long id);

    /**
     * Procura um utilizador pelo seu nome de utilizador.
     * @param username Nome de utilizador
     * @return {@link User} se encontrado, ou {@code null}
     */
    User findByUsername(String username);

    /**
     * Procura um utilizador pelo seu endereço de e-mail.
     * @param email Endereço de e-mail
     * @return {@link User} se encontrado, ou {@code null}
     */
    User findByEmail(String email);

    /**
     * Atualiza os dados de um utilizador existente.
     * @param user Utilizador com dados atualizados
     */
    void updateUser(User user);

    /**
     * Desativa logicamente a conta de um utilizador.
     * @param id Identificador do utilizador
     */
    void disableUser(Long id);

    /**
     * Remove fisicamente um utilizador da base de dados.
     * @param id Identificador do utilizador
     */
    void deleteUser(Long id);

    /**
     * Lista todos os utilizadores registados no sistema.
     * @return Lista de utilizadores
     */
    List<User> listAllUsers();
}