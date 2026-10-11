package util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Utilitário singleton para disponibilizar a fábrica do Entity Manager da JPA.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class EntityManagerFactoryUtil {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("iXitique");

    private EntityManagerFactoryUtil() {
    }

    /**
     * Obtém a instância global do {@link EntityManagerFactory}.
     * @return Instância singleton
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}