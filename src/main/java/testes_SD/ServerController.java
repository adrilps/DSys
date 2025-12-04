package testes_SD;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerController {

    @FXML private TextField portField;
    @FXML private TextArea logArea;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Label statusLabel;

    private ServerSocket serverSocket;
    private boolean isRunning = false;

    private final UsuarioService usuarioService = new UsuarioService();
    private final AuthService authService = new AuthService();
    private final MovieService movieService = new MovieService();
    private final ReviewService reviewService = new ReviewService();

    @FXML
    public void initialize() {
        PrintStream printStream = new PrintStream(new ConsoleOutputStream(logArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    @FXML
    private void handleStartServer() {
        String portText = portField.getText();

        try {
            int port = Integer.parseInt(portText);

            Thread serverThread = new Thread(() -> runServerLoop(port));
            serverThread.setDaemon(true);
            serverThread.start();

            isRunning = true;
            updateUIState();
            System.out.println("--- Servidor Iniciado na porta " + port + " ---");

        } catch (NumberFormatException e) {
            System.out.println("Erro: Porta inválida.");
        }
    }

    @FXML
    private void handleStopServer() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("--- Servidor Parado ---");
        } catch (IOException e) {
            System.err.println("Erro ao fechar servidor: " + e.getMessage());
        } finally {
            updateUIState();
        }
    }

    private void runServerLoop(int port) {
        try {
            serverSocket = new ServerSocket(port);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("Novo cliente conectado: " + clientIp);

                    new EchoServerTCP_Thread_Server(clientSocket, usuarioService, authService, movieService, reviewService);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Erro no accept: " + e.getMessage());
                    } else {
                        System.out.println("Socket fechado.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Não foi possível ouvir a porta " + port);
            Platform.runLater(this::handleStopServer);
        }
    }

    private void updateUIState() {
        btnStart.setDisable(isRunning);
        portField.setDisable(isRunning);
        btnStop.setDisable(!isRunning);

        if (isRunning) {
            statusLabel.setText("Status: Rodando");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("Status: Parado");
            statusLabel.setStyle("-fx-text-fill: grey;");
        }
    }

    /**
     * Classe mágica para redirecionar System.out para o JavaFX TextArea
     */
    private static class ConsoleOutputStream extends OutputStream {
        private final TextArea textArea;

        public ConsoleOutputStream(TextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            appendToTextArea(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            appendToTextArea(new String(b, off, len));
        }

        private void appendToTextArea(String text) {
            Platform.runLater(() -> textArea.appendText(text));
        }
    }
}