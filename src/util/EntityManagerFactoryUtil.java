package util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class EntityManagerFactoryUtil {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("iXitique");

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
