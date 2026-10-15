
module iXitique {

    // JPA
    requires jakarta.persistence;



    requires org.hibernate.orm.core;

    // JavaFX
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    // MaterialFX
    requires MaterialFX;
    // Segurança
    requires jbcrypt;


    // JavaFX / FXML
    exports view;

    opens view;
    opens controller;

    // Hibernate / JPA
    opens model;

    opens model.enums;
}
