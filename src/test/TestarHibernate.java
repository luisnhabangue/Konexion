
package test;

import jakarta.persistence.EntityManagerFactory;
import util.EntityManagerFactoryUtil;
/**
 * Componente de apoio à aplicação, responsável pela funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

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
