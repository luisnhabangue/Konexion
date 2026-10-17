
package test;

import jakarta.persistence.EntityManagerFactory;
import util.EntityManagerFactoryUtil;

public class TestarHibernate {

    public static void main(String[] args) {

        try {
            EntityManagerFactory emf =
                    EntityManagerFactoryUtil.getEntityManagerFactory();

            System.out.println("Hibernate inicializado com sucesso!");

            emf.close();

        } catch (Throwable erro) {
            erro.printStackTrace();
        }
    }
}
