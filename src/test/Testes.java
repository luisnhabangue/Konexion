package test;

import model.Cycle;
import model.Group;
import model.User;
import model.enums.Gender;
import model.enums.PaymentMethod;
import service.AuthService;
import service.GroupService;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * Componente de apoio à aplicação, responsável pela funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

public class Testes {




    static void main(String[] args) {
        //Teste registro

        AuthService as = new AuthService();

        User user = new User(null,"Luis","Nhabangue","21214214142", Gender.M,"nhabangue","12345678","luisnhabangue@gmail.com","+258 870252417");
      //  as.register(user, "12345678");


             //User user_2 = new User(null,"Wagner","Vilanculo","212142141242", Gender.M,"vilanculo","12345678","luisnhaban@gmail.com","+258 870252617");


        GroupService  gs = new GroupService();

        Group grupo = new Group(null,"Friends Forever 1", "Grupo destinado a qualquer coisa", BigDecimal.valueOf(200),5, LocalDate.now(),as.login("nhabangue","12345678"),15);
      //   gs.createNewGroup(grupo);

//     gs.findById(2L);
    //    as.register(user_2,"12345678");
     //   gs.addMemberToGroup(1L,  as.findById(1L).getId());


        //gs.addNewCycleToGroup(1L,1L);
      //  gs.addContributionToCycle(1L,1L,BigDecimal.valueOf(200), PaymentMethod.EMOLA);
       // gs.addContributionToCycle(1L,1L,BigDecimal.valueOf(200), PaymentMethod.MPESA);


    }
}
