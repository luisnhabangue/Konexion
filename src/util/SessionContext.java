package util;

import model.User;

public final class SessionContext {
    private static User currentUser;

    private SessionContext() {
    }

    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Não existe uma sessão autenticada.");
        }
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("O utilizador autenticado é obrigatório.");
        }
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
