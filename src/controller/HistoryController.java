package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Contribution;
import service.GroupService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Controlador JavaFX responsável pela visualização do histórico completo de contribuições do utilizador.
 * <p>
 * Permite ao utilizador consultar todas as suas contribuições efetuadas e exportar o respetivo registo em formato CSV.
 * </p>

 * @since 1.0
 */
public class HistoryController {

    private final GroupService groupService = new GroupService();
    private List<Contribution> contributions;

    @FXML private VBox activityRows;
    @FXML private Button btnExportCsv;

    /**
     * Inicializa a vista recarregando as contribuições do utilizador ativo e renderizando as linhas da tabela.
     */
    @FXML
    private void initialize() {
        contributions = groupService.findContributionsForUser(SessionContext.getCurrentUser().getId());
        btnExportCsv.setOnAction(event -> exportCsv());
        activityRows.getChildren().clear();
        for (Contribution contribution : contributions) {
            HBox row = new HBox(18);
            row.getStyleClass().add("t-row");
            Label date = styled(UiFormatting.date(contribution.getPaymentDate()), "t-td", "c-w150");
            Label group = styled(contribution.getUserGroup().getGroup().getGroupName(), "t-name");
            HBox.setHgrow(group, javafx.scene.layout.Priority.ALWAYS);
            Label amount = styled(UiFormatting.money(contribution.getAmount()), "t-amount", "c-w130");
            Label status = styled(contribution.getStatus().toString(), "badge",
                    contribution.getStatus() == model.enums.ContributionStatus.PAID ? "badge-success" : "badge-warning");
            row.getChildren().addAll(date, group, amount, status);
            activityRows.getChildren().add(row);
        }
        if (contributions.isEmpty()) {
            activityRows.getChildren().add(styled("O seu histórico aparecerá aqui após o primeiro pagamento.", "section-sub"));
        }
    }

    /**
     * Exporta o histórico de contribuições apresentadas para um ficheiro com extensão CSV.
     */
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar histórico de contribuições");
        chooser.setInitialFileName("historico-xitique.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiro CSV", "*.csv"));
        java.io.File file = chooser.showSaveDialog(activityRows.getScene().getWindow());
        if (file == null) {
            return;
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add("Data;Grupo;Valor;Método;Estado");
        for (Contribution contribution : contributions) {
            lines.add(String.join(";",
                    csv(UiFormatting.date(contribution.getPaymentDate())),
                    csv(contribution.getUserGroup().getGroup().getGroupName()),
                    csv(UiFormatting.money(contribution.getAmount())),
                    csv(contribution.getPaymentMethod().toString()),
                    csv(contribution.getStatus().toString())));
        }
        try {
            Files.write(file.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível exportar o histórico: " + e.getMessage()));
        }
    }

    /**
     * Formata um valor de texto para escape adequado numa célula CSV.
     *
     * @param value Texto a ser formatado.
     * @return Texto entre aspas e com plicas duplicadas.
     */
    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Instancia um rótulo com estilos atribuídos. */
    private Label styled(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }
}