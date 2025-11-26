package testes_SD;

import java.net.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EchoServerTCP_Thread_Server extends Thread {

    protected Socket clientSocket;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final MovieService movieService;
    private final ReviewService reviewService;
    
    private String nomeUsuarioLogado = null;
    private String roleUsuarioLogado = null;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        System.out.println("Qual porta o servidor deve usar? ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int porta = Integer.parseInt(br.readLine());

        System.out.println("Servidor carregado na porta " + porta);

        final UsuarioService usuarioService = new UsuarioService();
        final AuthService authService = new AuthService();
        final MovieService movieService = new MovieService();
        final ReviewService reviewService = new ReviewService();

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Aguardando conexao....\n ");
            try {
                while (true) {
                    Socket newClientSocket = serverSocket.accept();
                    String clientIpAddress = newClientSocket.getInetAddress().getHostAddress();
                    System.out.println("Accept ativado. Novo cliente conectado de: " + clientIpAddress + "\n");
                    
                    new EchoServerTCP_Thread_Server(newClientSocket, usuarioService, authService, movieService, reviewService);
                }
            } catch (IOException e) {
                System.err.println("Accept falhou!");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Nao foi possivel ouvir a porta " + porta);
            System.exit(1);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Nao foi possivel fechar a porta " + porta);
                System.exit(1);
            }
        }
    }

    private EchoServerTCP_Thread_Server(Socket clientSoc, UsuarioService uService, AuthService aService, MovieService mService, ReviewService rService) {
        this.clientSocket = clientSoc;
        this.usuarioService = uService;
        this.authService = aService;
        this.movieService = mService;
        this.reviewService = rService;
        start();
    }

    @Override
    public void run() {
        System.out.println("Nova thread de comunicacao iniciada.\n");

        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                System.out.println("Servidor recebeu: " + inputLine);
                String operacao = "";
                JsonNode requestNode = null;
                boolean operacaoEncerra = false;

                try {
                    requestNode = objectMapper.readTree(inputLine);
                    if (requestNode.has("operacao")) {
                        operacao = requestNode.get("operacao").asText().toUpperCase();
                    } else {
                        sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas (sem 'operacao')");
                        continue;
                    }

                    boolean precisaAutenticar = !operacao.equals("LOGIN") && !operacao.equals("CRIAR_USUARIO");

                    if (precisaAutenticar) {
                        this.nomeUsuarioLogado = null;
                        this.roleUsuarioLogado = null;

                        if (!requestNode.has("token")) {
                            sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas (sem 'token')");
                            continue;
                        }
                        String token = requestNode.get("token").asText();
                        DecodedJWT jwtValido = authService.validarToken(token);

                        if (jwtValido == null) {
                            sendJsonError(out, "401", "Erro: Token inválido");
                            continue;
                        }
                        this.nomeUsuarioLogado = jwtValido.getClaim("nome").asString();
                        this.roleUsuarioLogado = jwtValido.getClaim("funcao").asString();
                    }

                    switch (operacao) {
                        case "LOGIN":
                            replyLogin(inputLine, out);
                            break;
                        case "CRIAR_USUARIO":
                            replyCreateUser(inputLine, out);
                            break;
                        case "LOGOUT":
                            replyLogout(out);
                            operacaoEncerra = true;
                            break;
                        case "LISTAR_PROPRIO_USUARIO":
                            replyListarProprioUsuario(out);
                            break;
                        case "EDITAR_PROPRIO_USUARIO":
                            replyEditarProprioUsuario(inputLine, out);
                            break;
                        case "EXCLUIR_PROPRIO_USUARIO":
                            replyExcluirProprioUsuario(out);
                            operacaoEncerra = true;
                            break;
                        case "LISTAR_USUARIOS":
                            replyListarUsuarios(out);
                            break;
                        case "ADMIN_EDITAR_USUARIO":
                            replyAdminEditarUsuario(inputLine, out);
                            break;
                        case "ADMIN_EXCLUIR_USUARIO":
                            replyAdminExcluirUsuario(inputLine, out);
                            break;
                        case "CRIAR_FILME":
                            replyAdminCreateMovie(inputLine, out);
                            break;
                        case "EDITAR_FILME":
                            replyAdminEditarFilme(inputLine, out);
                            break;
                        case "EXCLUIR_FILME":
                            replyAdminExcluirFilme(inputLine, out);
                            break;
                        case "LISTAR_FILMES":
                            replyListarFilmes(out);
                            break;
                        case "CRIAR_REVIEW":
                            replyCriarReview(inputLine, out);
                            break;
                        case "LISTAR_REVIEWS_USUARIO":
                            replyListarReviewsUsuario(out);
                            break;
                        case "EDITAR_REVIEW":
                            replyEditarReview(inputLine, out);
                            break;
                        case "EXCLUIR_REVIEW":
                            replyExcluirReview(inputLine, out);
                            break;
                        case "BUSCAR_FILME_ID":
                            replyBuscarFilmeId(inputLine, out);
                            break;
                        default:
                            sendJsonError(out, "400", "Erro: Operação não encontrada ou inválida");
                    }

                    if (operacaoEncerra) {
                        break;
                    }

                } catch (JsonProcessingException e) {
                    sendJsonError(out, "400", "Erro: JSON mal formatado");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonError(out, "500", "Erro: Falha interna do servidor");
                }
            }
            System.out.println("Cliente " + (this.nomeUsuarioLogado != null ? this.nomeUsuarioLogado : "[NAO LOGADO]") + " desconectado.");

        } catch (IOException e) {
            System.err.println("Problema com Servidor de Communicacao! " + e.getMessage());
        }
    }

    private void sendJsonError(PrintWriter out, String status, String mensagem) {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("status", status);
            response.put("mensagem", mensagem);
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println("Servidor enviou: " + jsonResponse);
            out.println(jsonResponse);
        } catch (JsonProcessingException e) {
            out.println("{\"status\":\"500\", \"mensagem\":\"Erro ao gerar JSON de erro\"}");
        }
    }

    private void sendJsonSuccess(PrintWriter out, String status, String mensagem, Map<String, Object> data) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("mensagem", mensagem);
            if (data != null) {
                response.putAll(data);
            }
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println("Servidor enviou: " + jsonResponse);
            out.println(jsonResponse);
        } catch (JsonProcessingException e) {
            sendJsonError(out, "500", "Erro ao gerar JSON de sucesso");
        }
    }

    public void replyLogin(String recievedJson, PrintWriter out) {
        try {
            UserClass obj = objectMapper.readValue(recievedJson, UserClass.class);
            UsuarioDBModel usuarioAutenticado = usuarioService.autenticar(obj.getUsuario(), obj.getSenha());

            if (usuarioAutenticado != null) {
                String tokenGerado = authService.gerarToken(
                        usuarioAutenticado.getId(),
                        usuarioAutenticado.getNome(),
                        usuarioAutenticado.getRole());
                Map<String, Object> data = new HashMap<>();
                data.put("token", tokenGerado);
                sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);
            } else {
                sendJsonError(out, "401", "Erro: Credenciais inválidas");
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas no JSON de login");
        }
    }

    public void replyCreateUser(String recievedJson, PrintWriter out) {
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("usuario") || !requestNode.get("usuario").has("nome") || !requestNode.get("usuario").has("senha")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'usuario.nome' e 'usuario.senha')");
                return;
            }
            String nome = requestNode.get("usuario").get("nome").asText();
            String senha = requestNode.get("usuario").get("senha").asText();
            String resultado = usuarioService.criarUsuario(nome, senha);
            
            switch (resultado) {
                case "201": sendJsonSuccess(out, "201", "Sucesso: Recurso cadastrado", null); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos (não segue REGEX)"); break;
                case "409": sendJsonError(out, "409", "Erro: Recurso ja existe (usuário duplicado)"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyLogout(PrintWriter out) {
        this.nomeUsuarioLogado = null;
        this.roleUsuarioLogado = null;
        sendJsonSuccess(out, "200", "Sucesso: Operação realizada com sucesso", null);
    }

    public void replyListarProprioUsuario(PrintWriter out) {
        UsuarioDBModel user = usuarioService.buscarUsuarioPorNome(this.nomeUsuarioLogado);
        if (user == null) {
            sendJsonError(out, "404", "Erro: Recurso (usuário) inexistente");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("usuario", user.getNome());
        sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);
    }

    public void replyEditarProprioUsuario(String recievedJson, PrintWriter out) {
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("usuario") || !requestNode.get("usuario").has("senha")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'usuario.senha')");
                return;
            }
            String novaSenha = requestNode.get("usuario").get("senha").asText();
            String resultado = usuarioService.atualizarSenhaPropria(this.nomeUsuarioLogado, novaSenha);
            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null); break;
                case "403": sendJsonError(out, "403", "Erro: sem permissão (admin não pode ser editado)"); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos (não segue REGEX)"); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso (usuário) inexistente"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyExcluirProprioUsuario(PrintWriter out) {
        String resultado = usuarioService.excluirUsuarioProprio(this.nomeUsuarioLogado);
        switch (resultado) {
            case "200":
                sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null);
                this.nomeUsuarioLogado = null;
                this.roleUsuarioLogado = null;
                break;
            case "403": sendJsonError(out, "403", "Erro: sem permissão (admin não pode ser excluído)"); break;
            case "404": sendJsonError(out, "404", "Erro: Recurso (usuário) inexistente"); break;
            case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
        }
    }

    public void replyListarUsuarios(PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        List<UsuarioDBModel> todosUsuarios = usuarioService.listarTodosUsuarios();
        List<Map<String, String>> usuariosFormatados = new ArrayList<>();
        for (UsuarioDBModel user : todosUsuarios) {
            Map<String, String> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("nome", user.getNome());
            usuariosFormatados.add(userData);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("usuarios", usuariosFormatados);
        sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);
    }

    public void replyAdminEditarUsuario(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("id") || !requestNode.has("usuario") || !requestNode.get("usuario").has("senha")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id' e 'usuario.senha')");
                return;
            }
            String idAlvo = requestNode.get("id").asText();
            String novaSenha = requestNode.get("usuario").get("senha").asText();
            String resultado = usuarioService.adminEditarSenhaPorId(idAlvo, novaSenha);
            
            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null); break;
                case "403": sendJsonError(out, "403", "Erro: sem permissão (não pode alterar admin)"); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso (usuário) inexistente"); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos (nova senha não segue REGEX)"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyAdminExcluirUsuario(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("id")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id')");
                return;
            }
            String idUsuario = requestNode.get("id").asText();
            String resultado = usuarioService.adminExcluirUsuarioPorId(idUsuario);
            
            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: Usuário excluído", null); break;
                case "403": sendJsonError(out, "403", "Erro: sem permissão (admin não pode ser excluído)"); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso (usuário) inexistente"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyAdminCreateMovie(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("filme")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'filme')");
                return;
            }
            JsonNode filmeNode = requestNode.get("filme");
            MovieClass movieDTO = objectMapper.treeToValue(filmeNode, MovieClass.class);

            if (movieDTO == null || movieDTO.getTitulo() == null || movieDTO.getGenero() == null || movieDTO.getDiretor() == null || movieDTO.getAno() == null) {
                 sendJsonError(out, "422", "Erro: Campos faltantes no objeto 'filme' (titulo, diretor, ano, genero)");
                 return;
            }

            String resultado = movieService.criarFilme(movieDTO);
            
            switch (resultado) {
                case "201": sendJsonSuccess(out, "201", "Sucesso: Recurso (filme) cadastrado", null); break;
                case "409": sendJsonError(out, "409", "Erro: Recurso ja existe (título/diretor/ano duplicado)"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado ou objeto 'filme' inválido");
        }
    }

    public void replyAdminEditarFilme(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("filme") || !requestNode.get("filme").has("id")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'filme' e 'filme.id')");
                return;
            }
            JsonNode filmeNode = requestNode.get("filme");
            String idFilme = filmeNode.get("id").asText();
            MovieClass movieDTO = objectMapper.treeToValue(filmeNode, MovieClass.class);

            if (movieDTO == null || movieDTO.getTitulo() == null || movieDTO.getGenero() == null || movieDTO.getDiretor() == null || movieDTO.getAno() == null) {
                 sendJsonError(out, "422", "Erro: Campos faltantes no objeto 'filme' (titulo, diretor, ano, genero)");
                 return;
            }

            String resultado = movieService.editarFilme(idFilme, movieDTO);
            
            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso (filme) inexistente"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado ou objeto 'filme' inválido");
        }
    }

    public void replyAdminExcluirFilme(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("id")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id')");
                return;
            }
            String idFilme = requestNode.get("id").asText();

            // 1. Tenta excluir o filme
            String resultado = movieService.excluirFilme(idFilme);

            switch (resultado) {
                case "200":
                    // --- ALTERAÇÃO AQUI ---
                    // Se o filme foi excluído com sucesso, limpa as reviews associadas
                    reviewService.deletarReviewsDoFilme(idFilme);
                    // -----------------------

                    sendJsonSuccess(out, "200", "Sucesso: Recurso (filme) e suas reviews excluídos", null);
                    break;
                case "404": sendJsonError(out, "404", "Erro: Recurso (filme) inexistente"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna do servidor"); break;
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyListarFilmes(PrintWriter out) {
        List<MovieDBModel> filmesDoDB = movieService.listarTodosFilmes();
        List<Map<String, Object>> filmesParaJson = new ArrayList<>();
        for (MovieDBModel filme : filmesDoDB) {
            Map<String, Object> filmeMap = new HashMap<>();
            filmeMap.put("id", filme.getId());
            filmeMap.put("titulo", filme.getTitulo());
            filmeMap.put("diretor", filme.getDiretor());
            filmeMap.put("ano", filme.getAno());
            filmeMap.put("genero", filme.getGenero());
            filmeMap.put("nota", filme.getNota());
            filmeMap.put("qtd_avaliacoes", filme.getQtd_avaliacoes());
            filmeMap.put("sinopse", filme.getSinopse());
            filmesParaJson.add(filmeMap);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("filmes", filmesParaJson);
        sendJsonSuccess(out, "200", "Sucesso: Operação realizada com sucesso", data);
    }

    public void replyCriarReview(String recievedJson, PrintWriter out) {
        // Requer autenticação (nomeUsuarioLogado != null)
        if (this.nomeUsuarioLogado == null) {
            sendJsonError(out, "403", "Erro: Você precisa estar logado.");
            return;
        }

        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("review")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'review')");
                return;
            }

            JsonNode reviewNode = requestNode.get("review");
            ReviewClass reviewDTO = objectMapper.treeToValue(reviewNode, ReviewClass.class);

            if (reviewDTO == null || reviewDTO.getId_filme() == null || reviewDTO.getNota() == null) {
                sendJsonError(out, "422", "Erro: Campos faltantes na review (id_filme, nota)");
                return;
            }

            // O nome do usuário vem do token (já validado), não do JSON
            String resultado = reviewService.criarReview(reviewDTO, this.nomeUsuarioLogado);

            switch (resultado) {
                case "201":
                    try {
                        double notaNova = Double.parseDouble(reviewDTO.getNota());
                        // Nota antiga é 0.0 porque é uma criação
                        // Operação é "ADD"
                        movieService.recalcularMedia(reviewDTO.getId_filme(), notaNova, 0.0, "ADD");

                        System.out.println("Média do filme " + reviewDTO.getId_filme() + " recalculada.");

                    } catch (Exception e) {
                        System.err.println("Erro ao recalcular média: " + e.getMessage());
                        // Não paramos o processo, pois a review já foi salva
                    }

                    // SÓ AGORA enviamos a resposta para o cliente
                    sendJsonSuccess(out, "201", "Sucesso: Review cadastrada", null);

                    // Se for status 201, devemos atualizar a média no MovieService
                    // Precisamos buscar a nota antiga (0 se for nova)
                    // movieService.recalcularMedia(reviewDTO.getId_filme(), Double.parseDouble(reviewDTO.getNota()), 0.0, "ADD");
                    break;
                case "405": sendJsonError(out, "405", "Erro: Nota inválida (1-5)"); break;
                case "409": sendJsonError(out, "409", "Erro: Você já avaliou este filme"); break;
                case "500": sendJsonError(out, "500", "Erro: Falha interna"); break;
            }

        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyListarReviewsUsuario(PrintWriter out) {
        // Verifica se está logado
        if (this.nomeUsuarioLogado == null) {
            sendJsonError(out, "403", "Erro: Você precisa estar logado.");
            return;
        }

        // Busca no serviço
        List<ReviewDBModel> reviewsDoUsuario = reviewService.listarReviewsPorUsuario(this.nomeUsuarioLogado);

        // Formata para JSON (List of Maps)
        List<Map<String, String>> reviewsFormatadas = new ArrayList<>();
        for (ReviewDBModel review : reviewsDoUsuario) {
            Map<String, String> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("id_filme", review.getId_filme());
            map.put("nome_usuario", review.getNome_usuario());
            map.put("nota", review.getNota());
            map.put("titulo", review.getTitulo());
            map.put("descricao", review.getDescricao());
            map.put("data", review.getData());
            reviewsFormatadas.add(map);
        }

        // Envia
        Map<String, Object> data = new HashMap<>();
        data.put("reviews", reviewsFormatadas);
        sendJsonSuccess(out, "200", "Sucesso: Operação realizada com sucesso", data);
    }

    public void replyEditarReview(String recievedJson, PrintWriter out) {
        // Verifica login
        if (this.nomeUsuarioLogado == null) {
            sendJsonError(out, "403", "Erro: Você precisa estar logado.");
            return;
        }

        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);

            // Valida estrutura
            if (!requestNode.has("review") || !requestNode.get("review").has("id")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'review.id')");
                return;
            }

            JsonNode reviewNode = requestNode.get("review");
            String idReview = reviewNode.get("id").asText();

            // Mapeia para DTO para facilitar leitura dos dados
            ReviewClass reviewDTO = objectMapper.treeToValue(reviewNode, ReviewClass.class);

            if (reviewDTO.getNota() == null || !reviewDTO.getNota().matches("[1-5](\\.[0-9])?")) {
                sendJsonError(out, "405", "Erro: Nota inválida (1-5)");
                return;
            }

            // --- LÓGICA DE NEGÓCIO CRÍTICA ---

            // 1. Busca a review original para checar dono e pegar nota antiga
            ReviewDBModel reviewOriginal = reviewService.buscarReviewPorId(idReview);

            if (reviewOriginal == null) {
                sendJsonError(out, "404", "Erro: Review não encontrada.");
                return;
            }

            // 2. Verifica propriedade (Apenas o dono pode editar)
            if (!reviewOriginal.getNome_usuario().equals(this.nomeUsuarioLogado)) {
                sendJsonError(out, "403", "Erro: Você não pode editar a review de outro usuário.");
                return;
            }

            // 3. Guarda dados antigos para o recálculo da média
            double notaAntiga = Double.parseDouble(reviewOriginal.getNota());
            String idFilme = reviewOriginal.getId_filme();

            // 4. Atualiza a review
            String resultado = reviewService.atualizarReview(
                    idReview,
                    reviewDTO.getTitulo(),
                    reviewDTO.getDescricao(),
                    reviewDTO.getNota()
            );

            if ("200".equals(resultado)) {
                // 5. Recalcula a média do filme
                double notaNova = Double.parseDouble(reviewDTO.getNota());
                // O parametro "UPDATE" usa a fórmula: (M*n - old + new) / n
                movieService.recalcularMedia(idFilme, notaNova, notaAntiga, "UPDATE");

                sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null);
            } else {
                sendJsonError(out, resultado, "Erro ao atualizar review.");
            }

        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        } catch (Exception e) {
            sendJsonError(out, "500", "Erro: Falha interna");
        }
    }

    public void replyExcluirReview(String recievedJson, PrintWriter out) {
        // Verifica login
        if (this.nomeUsuarioLogado == null) {
            sendJsonError(out, "403", "Erro: Você precisa estar logado.");
            return;
        }

        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);

            if (!requestNode.has("id")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id')");
                return;
            }

            String idReview = requestNode.get("id").asText();

            // --- LÓGICA DE NEGÓCIO ---

            // 1. Busca a review ANTES de apagar para pegar os dados
            ReviewDBModel reviewAlvo = reviewService.buscarReviewPorId(idReview);

            if (reviewAlvo == null) {
                sendJsonError(out, "404", "Erro: Review não encontrada.");
                return;
            }

            // 2. Verifica Permissão:
            // Pode apagar se for o DONO da review OU se for ADMIN
            boolean isDono = reviewAlvo.getNome_usuario().equals(this.nomeUsuarioLogado);
            boolean isAdmin = "admin".equals(this.roleUsuarioLogado);

            if (!isDono && !isAdmin) {
                sendJsonError(out, "403", "Erro: Sem permissão para excluir review de outro usuário.");
                return;
            }

            // 3. Guarda dados para o recálculo
            String idFilme = reviewAlvo.getId_filme();
            double notaParaRemover = Double.parseDouble(reviewAlvo.getNota());

            // 4. Executa a exclusão
            boolean excluiu = reviewService.deletarReview(idReview);

            if (excluiu) {
                // 5. Recalcula a média do filme (Operação "DELETE")
                movieService.recalcularMedia(idFilme, notaParaRemover, 0.0, "DELETE");

                sendJsonSuccess(out, "200", "Sucesso: Review excluída e média atualizada.", null);
            } else {
                sendJsonError(out, "500", "Erro: Falha ao excluir review.");
            }

        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonError(out, "500", "Erro: Falha interna");
        }
    }

    public void replyBuscarFilmeId(String recievedJson, PrintWriter out) {
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("id_filme")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id_filme')");
                return;
            }
            String idFilme = requestNode.get("id_filme").asText();

            // 1. Buscar o Filme (MovieService) - Você precisará adicionar um método 'buscarFilmePorId' público no MovieService
            //    ou usar o listarTodosFilmes() e filtrar (menos eficiente, mas funciona)
            MovieDBModel filme = null;
            for (MovieDBModel f : movieService.listarTodosFilmes()) {
                if (f.getId().equals(idFilme)) {
                    filme = f;
                    break;
                }
            }

            if (filme == null) {
                sendJsonError(out, "404", "Erro: Filme não encontrado");
                return;
            }

            // 2. Buscar as Reviews (ReviewService)
            List<ReviewDBModel> reviews = reviewService.listarReviewsDoFilme(idFilme);

            // 3. Montar a resposta
            Map<String, Object> data = new HashMap<>();

            // Objeto Filme
            Map<String, Object> filmeMap = new HashMap<>();
            filmeMap.put("id", filme.getId());
            filmeMap.put("titulo", filme.getTitulo());
            filmeMap.put("diretor", filme.getDiretor());
            filmeMap.put("ano", filme.getAno());
            filmeMap.put("genero", filme.getGenero());
            filmeMap.put("nota", filme.getNota());
            filmeMap.put("qtd_avaliacoes", filme.getQtd_avaliacoes());
            filmeMap.put("sinopse", filme.getSinopse());
            data.put("filme", filmeMap);

            // Lista de Reviews
            List<Map<String, String>> reviewsList = new ArrayList<>();
            for (ReviewDBModel r : reviews) {
                Map<String, String> rMap = new HashMap<>();
                rMap.put("id", r.getId());
                rMap.put("id_filme", r.getId_filme());
                rMap.put("nome_usuario", r.getNome_usuario());
                rMap.put("nota", r.getNota());
                rMap.put("titulo", r.getTitulo());
                rMap.put("descricao", r.getDescricao());
                rMap.put("data", r.getData());
                reviewsList.add(rMap);
            }
            data.put("reviews", reviewsList);

            sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);

        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }
}