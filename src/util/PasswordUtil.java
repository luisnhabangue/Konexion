package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitário para cálculo de hash seguro de palavras-passe através da biblioteca BCrypt.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Cria a hash encriptada da palavra-passe com sal ajustado a 12 rondas.
     *
     * @param password Palavra-passe em texto simples
     * @return String contendo a hash BCrypt gerada
     */
    public static String createHashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}