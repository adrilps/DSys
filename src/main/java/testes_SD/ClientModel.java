package testes_SD;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientModel {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper mapper;

    private String tokenJWT;
    private String userRole;

    public ClientModel() {
        this.mapper = new ObjectMapper();
        this.tokenJWT = null;
        this.userRole = null;
    }

    public String getUserRole() {
        return this.userRole;
    }

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao desconectar: " + e.getMessage());
        }
    }

    public JsonNode login(String user, String pass) throws IOException {
        UserClass loginUser = new UserClass("LOGIN", user, pass);
        String jsonRequest = mapper.writeValueAsString(loginUser);
        
        String jsonResponse = sendAndReceive(jsonRequest);
        JsonNode responseNode = mapper.readTree(jsonResponse);

        if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
            if (responseNode.has("token")) {
                this.tokenJWT = responseNode.get("token").asText();
                this.userRole = getRoleFromToken(this.tokenJWT);
            }
        }
        return responseNode;
    }

    public JsonNode createUser(String user, String pass) throws IOException {
        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("nome", user);
        usuarioMap.put("senha", pass);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "CRIAR_USUARIO");
        requestMap.put("usuario", usuarioMap);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode fetchProfile() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_PROPRIO_USUARIO");
        requestMap.put("token", this.tokenJWT);

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode updatePassword(String newPassword) throws IOException {
        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("senha", newPassword);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EDITAR_PROPRIO_USUARIO");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("usuario", usuarioMap);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode logout() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "LOGOUT");
        requestMap.put("token", this.tokenJWT);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        JsonNode responseNode = mapper.readTree(jsonResponse);

        if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
            this.tokenJWT = null;
            this.userRole = null;
        }
        return responseNode;
    }

    public JsonNode deleteAccount() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
        requestMap.put("token", this.tokenJWT);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        JsonNode responseNode = mapper.readTree(jsonResponse);

        if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
            this.tokenJWT = null;
            this.userRole = null;
        }
        return responseNode;
    }

    public JsonNode adminListUsers() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_USUARIOS");
        requestMap.put("token", this.tokenJWT);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode adminEditUser(String id, String newPassword) throws IOException {
        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("senha", newPassword);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "ADMIN_EDITAR_USUARIO");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("id", id);
        requestMap.put("usuario", usuarioMap);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode adminEditMovie(Map<String, Object> movieData) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EDITAR_FILME");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("filme", movieData);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    private String sendAndReceive(String jsonRequest) throws IOException {
        System.out.println("\nCliente enviou: " + jsonRequest);
        out.println(jsonRequest);
        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);
        return jsonResponse;
    }

    private String getRoleFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();

            String payloadJson = new String(decoder.decode(chunks[1]));
            
            JsonNode payloadNode = mapper.readTree(payloadJson);
            
            if (payloadNode.has("funcao")) {
                return payloadNode.get("funcao").asText();
            }
        } catch (Exception e) {
            System.err.println("Erro ao decodificar o token JWT: " + e.getMessage());
        }
        return "user";
    }
    
    public JsonNode adminCreateMovie(Map<String, Object> movieData) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "CRIAR_FILME");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("filme", movieData);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }
    
    public JsonNode listarFilmes() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_FILMES");
        requestMap.put("token", this.tokenJWT);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }
    
    public JsonNode adminExcluirFilme(String idFilme) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EXCLUIR_FILME");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("id", idFilme);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }
    
    public JsonNode adminExcluirUsuario(String idUsuario) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "ADMIN_EXCLUIR_USUARIO");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("id", idUsuario);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode createReview(String idFilme, Map<String, String> reviewData) throws IOException {
        Map<String, Object> reviewObj = new HashMap<>();
        reviewObj.put("id_filme", idFilme);
        reviewObj.put("titulo", reviewData.get("titulo"));
        reviewObj.put("descricao", reviewData.get("descricao"));
        reviewObj.put("nota", reviewData.get("nota"));

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "CRIAR_REVIEW");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("review", reviewObj);

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode listarMinhasReviews() throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_REVIEWS_USUARIO");
        requestMap.put("token", this.tokenJWT);

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode editReview(Map<String, String> reviewData) throws IOException {
        // Monta objeto review
        Map<String, Object> reviewObj = new HashMap<>();
        reviewObj.put("id", reviewData.get("id"));
        reviewObj.put("titulo", reviewData.get("titulo"));
        reviewObj.put("descricao", reviewData.get("descricao"));
        reviewObj.put("nota", reviewData.get("nota"));

        // Monta requisição principal
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EDITAR_REVIEW");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("review", reviewObj);

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode deleteReview(String idReview) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EXCLUIR_REVIEW");
        requestMap.put("token", this.tokenJWT);
        requestMap.put("id", idReview); // Envia apenas o ID da review

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }

    public JsonNode getMovieDetails(String idFilme) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "BUSCAR_FILME_ID");
        requestMap.put("id_filme", idFilme);
        requestMap.put("token", this.tokenJWT); // Enviamos o token por segurança

        String jsonRequest = mapper.writeValueAsString(requestMap);
        String jsonResponse = sendAndReceive(jsonRequest);
        return mapper.readTree(jsonResponse);
    }
}