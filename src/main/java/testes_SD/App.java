package testes_SD;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Carrega o login.fxml
        scene = new Scene(loadFXML("login"), 400, 400);
        stage.setTitle("VoteFlix Client");
        stage.setScene(scene);
        stage.show();
    }

    // Método utilitário para trocar de tela facilmente
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        // Fecha a conexão ao fechar a janela
        Context.getInstance().getModel().disconnect();
    }
}