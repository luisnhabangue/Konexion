package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import model.enums.Page;
import util.Navigator;

/**
 * Controlador JavaFX do ecrã "Guia de utilização" ({@code help-guide.fxml}).
 * O conteúdo é estático e está definido no FXML; este controlador só trata a navegação.
 *
 * @since 1.0
 */
public class HelpGuideController {

    @FXML private Button btnBack;
    @FXML private Button btnFaq;

    /** Construtor padrão exigido pelo {@code FXMLLoader}. */
    public HelpGuideController() {
    }

    @FXML
    private void initialize() {
        btnBack.setOnAction(event -> Navigator.go(Page.HELP_CENTER));
        btnFaq.setOnAction(event -> Navigator.go(Page.HELP_FAQ));
    }
}
