package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import model.Cycle;
import model.Group;
import model.enums.CycleStatus;
import model.enums.Page;
import service.DemoDataService;
import service.GroupService;
import util.ModalDialogs;
import util.Navigator;
import util.SessionContext;

import java.util.List;
import java.util.Locale;

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
        btnSeedDemoData.setOnAction(event -> seedDemoData());
        refresh();
    }

    public void search(String value) {
        groupSearchField.setText(value == null ? "" : value);
    }

    private void refresh() {
        try {
            groups = groupService.findAllForUser(SessionContext.getCurrentUser().getId());
            renderGroups();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

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
            column = (column + 1) % 3;
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
            groupsGrid.add(empty, 0, 0, 3, 1);
        }
        filterAll.setText("Todos  " + groups.size());
        filterActive.setText("Ativos  " + groups.stream()
                .filter(group -> groupService.findActiveCycle(group.getGroupId()) != null).count());
        filterFinished.setText("Com ciclo concluído  " + groups.stream().filter(group -> {
            List<Cycle> cycles = groupService.findCycles(group.getGroupId());
            return !cycles.isEmpty() && cycles.get(cycles.size() - 1).getStatus() == CycleStatus.CONCLUDED;
        }).count());
    }

    private void openGroup(Long groupId) {
        Navigator.setSelectedGroupId(groupId);
        Navigator.go(Page.GROUP_DETAIL);
    }

    private void openCreateGroup() {
        Window owner = groupsGrid.getScene().getWindow();
        ModalDialogs.showCreateGroup(owner, this::refresh);
    }

    private void seedDemoData() {
        try {
            String message = new DemoDataService().seedForCurrentUser();
            new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
            refresh();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
