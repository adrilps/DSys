package testes_SD;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/server.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 600, 500);
        stage.setTitle("VoteFlix Server Manager");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            ServerController controller = fxmlLoader.getController();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}