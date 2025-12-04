package testes_SD;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MovieFormController {

    @FXML private TextField titleField;
    @FXML private TextField directorField;
    @FXML private TextField yearField;

    @FXML private MenuButton genreSelector;

    @FXML private TextArea synopsisArea;
    @FXML private Label errorLabel;

    private ClientModel model;
    private boolean saveClicked = false;

    private final String[] GENEROS_PADRAO = {
            "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
            "Ficção Científica", "Terror", "Romance", "Documentário",
            "Musical", "Animação"
    };

    public void initialize() {
        this.model = Context.getInstance().getModel();

        yearField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[0-9]*") && newText.length() <= 4) {
                return change;
            }
            return null;
        }));

        configurarGeneros();
    }

    private void configurarGeneros() {
        for (String genero : GENEROS_PADRAO) {
            CheckMenuItem item = new CheckMenuItem(genero);

            item.selectedProperty().addListener((obs, wasSelected, isSelected) -> atualizarTextoBotao());

            genreSelector.getItems().add(item);
        }
    }

    private void atualizarTextoBotao() {
        List<String> selecionados = getGenerosSelecionados();
        if (selecionados.isEmpty()) {
            genreSelector.setText("Selecione os Gêneros...");
        } else {
            genreSelector.setText(String.join(", ", selecionados));
        }
    }

    private List<String> getGenerosSelecionados() {
        List<String> selecionados = new ArrayList<>();
        for (MenuItem item : genreSelector.getItems()) {
            if (item instanceof CheckMenuItem) {
                CheckMenuItem checkItem = (CheckMenuItem) item;
                if (checkItem.isSelected()) {
                    selecionados.add(checkItem.getText());
                }
            }
        }
        return selecionados;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (!validarCampos()) return;

        try {
            Map<String, Object> movieData = new HashMap<>();
            movieData.put("titulo", titleField.getText());
            movieData.put("diretor", directorField.getText());
            movieData.put("ano", yearField.getText());
            movieData.put("sinopse", synopsisArea.getText());

            movieData.put("genero", getGenerosSelecionados());

            JsonNode response = model.adminCreateMovie(movieData);

            if (response.has("status") && response.get("status").asText().equals("201")) {
                saveClicked = true;
                closeWindow();
            } else {
                String msg = response.has("mensagem") ? response.get("mensagem").asText() : "Erro desconhecido";
                errorLabel.setText("Erro: " + msg);
            }

        } catch (IOException e) {
            errorLabel.setText("Erro de conexão: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private boolean validarCampos() {
        if (titleField.getText().isEmpty() || directorField.getText().isEmpty() ||
                yearField.getText().isEmpty() || getGenerosSelecionados().isEmpty()) {

            errorLabel.setText("Por favor, preencha todos os campos e selecione ao menos um gênero.");
            return false;
        }
        return true;
    }
}