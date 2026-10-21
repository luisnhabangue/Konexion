package util;

import model.User;

/**
 * Utilitário responsável pelo armazenamento e gestão do estado do utilizador autenticado na sessão.
 *
 * @since 1.0
 */
public final class SessionContext {

    /** Instância do utilizador atualmente autenticado no sistema. */
    private static User currentUser;

    /**
     * Construtor privado para impedir a instanciação da classe utilitária.
     */
    private SessionContext() {
    }

    /**
     * Obtém o utilizador atualmente autenticado na sessão.
     *
     * @return O utilizador autenticado ({@link User}).
     * @throws IllegalStateException Se não existir uma sessão autenticada ativa.
     */
    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Não existe uma sessão autenticada.");
        }
        return currentUser;
    }

    /**
     * Define o utilizador autenticado na sessão.
     *
     * @param user O utilizador ({@link User}) a definir.
     * @throws IllegalArgumentException Se o utilizador fornecido for {@code null}.
     */
    public static void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("O utilizador autenticado é obrigatório.");
        }
        currentUser = user;
    }

    /**
     * Encerra e limpa a sessão atual, removendo o utilizador autenticado.
     */
    public static void clear() {
        currentUser = null;
    }
}