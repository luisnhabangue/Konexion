package test;

import model.User;
import model.enums.Gender;
import service.AuthService;

public class Testes {




    static void main(String[] args) {
        //Teste registro

        AuthService as = new AuthService();

        User user = new User(null,"Luis","Nhabangue","21214214142", Gender.M,"nhabangue","12345678","luisnhabangue@gmail.com","+258 870252417");
        as.register(user, "12345678");





    }
}
