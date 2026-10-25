package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import model.Cycle;
import model.Group;
import model.enums.CycleStatus;
import model.enums.Page;
import service.GroupService;
import util.ModalDialogs;
import util.Navigator;
import util.SessionContext;
import util.StyledDialogs;

import java.util.List;
import java.util.Locale;

/**
 * Controlador JavaFX para a vista de listagem e exploração de grupos de Xitique.
 * <p>
 * Permite filtrar os grupos do utilizador (todos, ativos, concluídos), pesquisar por nome,
 * consultar/aderir a grupos públicos e semear dados de demonstração.
 * </p>

 * @since 1.0
 */
public class GroupsController {

    private final GroupService groupService = new GroupService();
    private List<Group> groups;
    private String searchText = "";

    @FXML private GridPane groupsGrid;
    @FXML private Button btnCreateNewGroup;
    @FXML private Button btnSeedDemoData;
    @FXML private TextField groupSearchField;
    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterActive;
    @FXML private ToggleButton filterFinished;
    @FXML private VBox publicGroupRows;

    /** Número de colunas no grelha de grupos (responsivo). */
    private int responsiveColumns = 3;

    /**
     * Configura os ouvintes de eventos da pesquisa, botões de filtro e redimensionamento da grelha.
     */
    @FXML
    private void initialize() {
        groupsGrid.getChildren().clear();
        groupSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            renderGroups();
        });
        filterAll.setOnAction(event -> renderGroups());
        filterActive.setOnAction(event -> renderGroups());
        filterFinished.setOnAction(event -> renderGroups());
        btnCreateNewGroup.setOnAction(event -> openCreateGroup());
       // btnSeedDemoData.setOnAction(event -> seedDemoData());
        groupsGrid.widthProperty().addListener((observable, oldWidth, newWidth) -> updateGridColumns(newWidth.doubleValue()));
        refresh();
    }

    /**
     * Define o texto da pesquisa programaticamente e atualiza a filtragem.
     *
     * @param value Texto de pesquisa a aplicar.
     */
    public void search(String value) {
        groupSearchField.setText(value == null ? "" : value);
    }

    /**
     * Recarrega a lista de grupos do utilizador e a lista de grupos públicos disponíveis.
     */
    private void refresh() {
        try {
            groups = groupService.findAllForUser(SessionContext.getCurrentUser().getId());
            renderPublicGroups();
            renderGroups();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Renderiza os cartões dos grupos na grelha com base nos filtros e pesquisa ativos.
     */
    private void renderGroups() {
        if (groups == null) {
            return;
        }
        groupsGrid.getChildren().clear();
        int column = 0;
        int row = 0;
        int visibleCount = 0;
        for (Group group : groups) {
            List<Cycle> cycles = groupService.findCycles(group.getGroupId());
            Cycle latest = cycles.isEmpty() ? null : cycles.get(cycles.size() - 1);
            boolean active = groupService.findActiveCycle(group.getGroupId()) != null;
            if (filterActive.isSelected() && !active) {
                continue;
            }
            if (filterFinished.isSelected() && (latest == null || latest.getStatus() != CycleStatus.CONCLUDED)) {
                continue;
            }
            if (!searchText.isBlank() && !group.getGroupName().toLowerCase(Locale.ROOT).contains(searchText)) {
                continue;
            }
            int memberCount = group.getMembers().size();
            int paidCount = latest == null ? 0 : (int) groupService.findContributionsByCycle(latest.getId()).stream()
                    .filter(contribution -> contribution.getStatus() == model.enums.ContributionStatus.PAID).count();
            groupsGrid.add(GroupCards.create(group, memberCount, latest == null ? 0 : latest.getNumero(),
                    paidCount, selected -> openGroup(selected.getGroupId())), column, row);
            column = (column + 1) % responsiveColumns;
            if (column == 0) {
                row++;
            }
            visibleCount++;
        }
        if (visibleCount == 0) {
            Label empty = new Label(groups.isEmpty()
                    ? "Ainda não participa em nenhum Xitique. Crie um grupo para começar."
                    : "Nenhum grupo corresponde aos filtros selecionados.");
            empty.getStyleClass().add("section-sub");
            groupsGrid.add(empty, 0, 0, responsiveColumns, 1);
        }

        filterAll.setText("Todos  " + groups.size());
        filterActive.setText("Ativos  " + groups.stream()
                .filter(group -> groupService.findActiveCycle(group.getGroupId()) != null).count());
        filterFinished.setText("Com ciclo concluído  " + groups.stream().filter(group -> {
            List<Cycle> cycles = groupService.findCycles(group.getGroupId());
            return !cycles.isEmpty() && cycles.get(cycles.size() - 1).getStatus() == CycleStatus.CONCLUDED;
        }).count());
    }

    /**
     * Ajusta o número de colunas da grelha dinamicamente de acordo com a largura da janela.
     *
     * @param width Largura atual do contentor.
     */
    private void updateGridColumns(double width) {
        int columns = width < 650 ? 1 : width < 980 ? 2 : 3;
        if (columns == responsiveColumns) {
            return;
        }
        responsiveColumns = columns;
        groupsGrid.getColumnConstraints().clear();
        for (int index = 0; index < columns; index++) {
            javafx.scene.layout.ColumnConstraints constraint = new javafx.scene.layout.ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            groupsGrid.getColumnConstraints().add(constraint);
        }
        renderGroups();
    }

    /**
     * Renderiza a lista de grupos públicos disponíveis para participação.
     */
    private void renderPublicGroups() {
        publicGroupRows.getChildren().clear();
        for (Group group : groupService.findPublicGroups()) {
            if (groups.stream().anyMatch(existing -> existing.getGroupId().equals(group.getGroupId()))) {
                continue;
            }
            HBox row = new HBox(10);
            row.getStyleClass().add("admin-row");
            Label details = new Label(group.getGroupName() + " · " + group.getMembers().size()
                    + " membros · Criado por " + group.getCreatedBy().getFirstName()
                    + (group.getCreatedBy().isVerified() ? " · Verificado" : " · Não verificado"));
            details.getStyleClass().add("t-name");
            HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
            Button join = new Button("Participar");
            join.getStyleClass().addAll("btn", "btn-primary");
            join.setDisable(!groupService.findCycles(group.getGroupId()).isEmpty());
            join.setOnAction(event -> joinPublicGroup(group));
            row.getChildren().addAll(details, join);
            publicGroupRows.getChildren().add(row);
        }
        if (publicGroupRows.getChildren().isEmpty()) {
            Label empty = new Label("Não há grupos públicos disponíveis para participação.");
            empty.getStyleClass().add("section-sub");
            publicGroupRows.getChildren().add(empty);
        }
    }

    /**
     * Junta o utilizador atual a um grupo público.
     *
     * @param group Grupo público selecionado.
     */
    private void joinPublicGroup(Group group) {
        try {
            groupService.joinPublicGroup(group.getGroupId(), SessionContext.getCurrentUser().getId());
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                    "Agora participa no grupo " + group.getGroupName() + "."));
            refresh();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    /** Abre a vista detalhada de um grupo especificando o seu ID. */
    private void openGroup(Long groupId) {
        Navigator.setSelectedGroupId(groupId);
        Navigator.go(Page.GROUP_DETAIL);
    }

    /** Abre a janela modal de criação de novo grupo. */
    private void openCreateGroup() {
        Window owner = groupsGrid.getScene().getWindow();
        ModalDialogs.showCreateGroup(owner, this::refresh);
    }

//    /** Popula a base de dados com dados de demonstração de teste. */
//    private void seedDemoData() {
//        try {
//            String message = new DemoDataService().seedForCurrentUser();
//            StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION, message));
//            refresh();
//        } catch (RuntimeException e) {
//            showError(e.getMessage());
//        }
//    }

    /** Apresenta uma mensagem de erro ao utilizador. */
    private void showError(String message) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, message));
    }
}