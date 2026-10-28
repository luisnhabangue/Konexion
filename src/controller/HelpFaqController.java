package controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import model.enums.Page;
import util.Navigator;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Controlador JavaFX do ecrã "Perguntas frequentes" ({@code help-faq.fxml}).
 * <p>
 * Mostra perguntas que se expandem ao clicar e permite filtrá-las pelo texto da pesquisa.
 * </p>
 *
 * @since 1.0
 */
public class HelpFaqController {

    /** Pergunta e resposta apresentadas no ecrã. */
    private record Faq(String question, String answer) { }

    private static final List<Faq> FAQS = List.of(
            new Faq("Porque não consigo criar um grupo?",
                    "A criação de grupos requer verificação aprovada por um administrador. Em “Meu perfil”, "
                            + "envie o BI ou NUIT e as fotografias pedidas e aguarde a aprovação. "
                            + "Entretanto, a conta pode participar em grupos existentes."),
            new Faq("Como entro num grupo?",
                    "O responsável do grupo adiciona-o ou convida-o. Os convites aparecem no ícone de notificações, "
                            + "onde pode aceitar ou recusar. Os grupos privados só aceitam membros por convite. "
                            + "Depois de o grupo iniciar ciclos, já não aceita novos membros."),
            new Faq("Posso adicionar ou remover membros depois de o grupo começar?",
                    "Não. Depois de iniciado o histórico de ciclos, a lista de membros fica fechada, "
                            + "para preservar a fila de recebimento e o histórico financeiro."),
            new Faq("Quantos membros são precisos para abrir um ciclo?",
                    "Pelo menos dois. Um grupo só pode ter um ciclo ativo de cada vez."),
            new Faq("Posso pagar mais do que uma vez no mesmo ciclo?",
                    "Não. Cada membro paga uma quota por ciclo, e o valor não pode ser inferior à quota definida no grupo."),
            new Faq("O que acontece se eu pagar depois do prazo?",
                    "Se pagar depois do prazo, ou se não pagar, são descontados 10 pontos de pontualidade "
                            + "e o responsável do grupo é notificado."),
            new Faq("Posso eliminar um grupo?",
                    "Só o criador pode eliminar o grupo, e apenas se ainda não tiver iniciado ciclos. "
                            + "Depois disso, o grupo fica guardado para preservar o histórico financeiro."),
            new Faq("Esqueci a palavra-passe. O que faço?",
                    "No ecrã de início de sessão, use a opção de recuperar a senha. Receberá por e-mail um código "
                            + "de 6 dígitos, válido durante 10 minutos, e poderá definir uma nova palavra-passe "
                            + "(8 a 64 caracteres, com pelo menos um número e um símbolo)."),
            new Faq("Como contacto o suporte?",
                    "No Centro de Ajuda, escolha “Contactar suporte” e preencha o assunto e a mensagem. "
                            + "Pode acompanhar o estado do pedido em “Os meus pedidos”.")
    );

    @FXML private Button btnBack;
    @FXML private TextField txtSearch;
    @FXML private VBox faqList;
    @FXML private Label lblNoResults;

    /** Construtor padrão exigido pelo {@code FXMLLoader}. */
    public HelpFaqController() {
    }

    @FXML
    private void initialize() {
        btnBack.setOnAction(event -> Navigator.go(Page.HELP_CENTER));
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> render(newValue));
        render("");
    }

    /**
     * Preenche a pesquisa e filtra as perguntas. Usado pelo Centro de Ajuda ao abrir este ecrã.
     *
     * @param query Texto a pesquisar.
     */
    public void search(String query) {
        txtSearch.setText(query == null ? "" : query);
    }

    private void render(String query) {
        faqList.getChildren().clear();
        String[] words = normalize(query).trim().split("\\s+");
        for (Faq faq : FAQS) {
            String text = normalize(faq.question() + " " + faq.answer());
            boolean matches = true;
            for (String word : words) {
                if (!word.isEmpty() && !text.contains(word)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                faqList.getChildren().add(buildItem(faq));
            }
        }
        boolean empty = faqList.getChildren().isEmpty();
        lblNoResults.setVisible(empty);
        lblNoResults.setManaged(empty);
    }

    /** Cria um cartão de pergunta cuja resposta aparece ao clicar. */
    private VBox buildItem(Faq faq) {
        Label question = new Label(faq.question());
        question.getStyleClass().add("t-name");
        question.setWrapText(true);
        question.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(question, Priority.ALWAYS);

        SVGPath chevron = new SVGPath();
        chevron.setContent("M9,6 L15,12 L9,18");
        chevron.getStyleClass().addAll("ico", "ico-sm");
        StackPane iconBox = new StackPane(chevron);
        iconBox.getStyleClass().add("ico-box-sm");
        StackPane arrow = new StackPane(iconBox);
        arrow.getStyleClass().add("round-arrow");

        HBox header = new HBox(12, question, arrow);
        header.setAlignment(Pos.CENTER_LEFT);

        Label answer = new Label(faq.answer());
        answer.getStyleClass().add("t-td");
        answer.setWrapText(true);
        answer.setVisible(false);
        answer.setManaged(false);
        VBox.setMargin(answer, new Insets(10, 0, 0, 0));

        VBox item = new VBox(header, answer);
        item.getStyleClass().add("card");
        item.setPadding(new Insets(14, 18, 14, 18));
        item.setCursor(Cursor.HAND);
        item.setOnMouseClicked(event -> {
            boolean show = !answer.isVisible();
            answer.setVisible(show);
            answer.setManaged(show);
            arrow.setRotate(show ? 90 : 0);
        });
        return item;
    }

    /** Remove acentos e passa para minúsculas, para a pesquisa ignorar acentuação e maiúsculas. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
