package testes_SD;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Gerencia a lógica de negócios e a persistência de dados para Filmes.
 * Esta classe é thread-safe.
 */
public class MovieService {

    // Caminho corrigido para o arquivo de Filmes
    private static final String MOVIES_FILE_PATH = "C:\\Users\\paulo\\eclipse-workspace\\DSys\\SDistribuidos\\Filmes.txt";
    
    // Lock para garantir que apenas uma thread modifique o arquivo por vez
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public MovieService() {
        inicializarMovieDatabase();
    }

    /**
     * Garante que o arquivo Filmes.txt exista.
     */
    private void inicializarMovieDatabase() {
        writeLock.lock();
        try {
            File file = new File(MOVIES_FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs(); // Cria diretórios se não existirem
                file.createNewFile();
                System.out.println("Arquivo 'Filmes.txt' criado.");
            }
        } catch (IOException e) {
            System.err.println("Erro crítico ao inicializar o 'Filmes.txt': " + e.getMessage());
        } finally {
            writeLock.unlock(); // Libera o lock
        }
    }

    /**
     * (HELPER) Lê todos os filmes do arquivo.
     * Este método SÓ deve ser chamado por outro método que já tenha o lock.
     */
    private List<MovieDBModel> lerTodosFilmes() throws IOException {
        return Files.lines(Paths.get(MOVIES_FILE_PATH))
                .map(MovieDBModel::deCsvString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * (HELPER) Sobrescreve o arquivo com a nova lista de filmes.
     * Este método SÓ deve ser chamado por outro método que já tenha o lock de escrita.
     */
    private void salvarTodosFilmes(List<MovieDBModel> filmes) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MOVIES_FILE_PATH, false))) { // false = sobrescrever
            for (MovieDBModel filme : filmes) {
                writer.println(filme.paraCsvString());
            }
        }
    }

    // --- MÉTODOS PÚBLICOS (CRUD) ---

    /**
     * Cria um novo filme no "banco de dados".
     * Requer que o filme seja único pelo conjunto de título, diretor e ano.
     */
    public String criarFilme(MovieClass movieDTO) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();

            // Requisito: Filmes serão únicos pelo conjunto de título, diretor e ano.
            boolean jaExiste = filmes.stream().anyMatch(filme -> 
                filme.getTitulo().equalsIgnoreCase(movieDTO.getTitulo()) &&
                filme.getDiretor().equalsIgnoreCase(movieDTO.getDiretor()) &&
                filme.getAno().equals(movieDTO.getAno())
            );

            if (jaExiste) {
                return "409"; // Conflito: Recurso já existe
            }

            // Achar o maior ID e somar 1
            int maxId = filmes.stream()
                              .mapToInt(f -> Integer.parseInt(f.getId()))
                              .max()
                              .orElse(0);
            String novoId = String.valueOf(maxId + 1);

            // Adiciona o novo filme (a nota começa em 0)
            filmes.add(new MovieDBModel(novoId, movieDTO));
            salvarTodosFilmes(filmes);
            
            System.out.println("Filme criado: " + movieDTO.getTitulo());
            return "201"; // Criado com sucesso

        } catch (IOException e) {
            e.printStackTrace();
            return "500"; // Erro interno
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Retorna uma cópia da lista de todos os filmes.
     */
    public List<MovieDBModel> listarTodosFilmes() {
        readLock.lock();
        try {
            // Retorna uma cópia para evitar que outras threads modifiquem a lista
            return new ArrayList<>(lerTodosFilmes());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Retorna lista vazia em caso de erro
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Atualiza os detalhes de um filme existente (título, diretor, etc.).
     * NÃO atualiza a nota.
     */
    public String editarFilme(String idFilme, MovieClass movieDTO) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            boolean encontrou = false;

            for (MovieDBModel filme : filmes) {
                if (filme.getId().equals(idFilme)) {
                    // Atualiza os detalhes do filme com base no DTO
                    filme.updateDetails(movieDTO); 
                    encontrou = true;
                    break;
                }
            }

            if (encontrou) {
                salvarTodosFilmes(filmes);
                System.out.println("Filme ID " + idFilme + " atualizado.");
                return "200"; // Sucesso
            } else {
                return "404"; // Não encontrado
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Exclui um filme do "banco de dados" usando seu ID.
     */
    public String excluirFilme(String idFilme) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            boolean removeu = filmes.removeIf(filme -> filme.getId().equals(idFilme));

            if (removeu) {
                salvarTodosFilmes(filmes);
                System.out.println("Filme ID " + idFilme + " excluído.");
                // TODO: Você também deve deletar todas as REVIEWS associadas a este filme.
                // (Isso exigirá um ReviewService)
                return "200"; // Sucesso
            } else {
                return "404"; // Não encontrado
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Atualiza a nota média de um filme.
     * Esta é a lógica central baseada nos seus requisitos.
     * * @param idFilme O ID do filme a ser atualizado.
     * @param notaDaReview A nota da review (ex: 4.0).
     * @param notaAntiga A nota antiga (SÓ é necessário para "UPDATE").
     * @param operacao "ADD" (adicionar nova), "DELETE" (remover) ou "UPDATE" (mudar).
     * @return String de status ("200", "404", "500").
     */
    public String recalcularMedia(String idFilme, double notaDaReview, double notaAntiga, String operacao) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            MovieDBModel filmeAlvo = null;

            // Encontra o filme
            for (MovieDBModel filme : filmes) {
                if (filme.getId().equals(idFilme)) {
                    filmeAlvo = filme;
                    break;
                }
            }

            if (filmeAlvo == null) {
                return "404"; // Filme não encontrado
            }

            // Pega os valores atuais
            double mediaAtual = Double.parseDouble(filmeAlvo.getNota());
            int n = Integer.parseInt(filmeAlvo.getQtd_avaliacoes());
            
            double novaMedia;
            int novoN;

            // Calcula a nova média com base na operação
            switch (operacao.toUpperCase()) {
                case "ADD":
                    novoN = n + 1;
                    // Fórmula: M = (M*n + x) / (n+1)
                    novaMedia = (mediaAtual * n + notaDaReview) / novoN;
                    break;
                
                case "DELETE":
                    novoN = n - 1;
                    // Fórmula: M = (M*n - x) / (n-1)
                    novaMedia = (novoN == 0) ? 0.0 : (mediaAtual * n - notaDaReview) / novoN;
                    break;

                case "UPDATE":
                    novoN = n; // Contagem de reviews não muda
                    if (novoN == 0) { // Segurança
                         novaMedia = 0.0;
                    } else {
                        // Fórmula: M = (M*n - x_antiga + x_nova) / n
                         novaMedia = (mediaAtual * n - notaAntiga + notaDaReview) / n;
                    }
                    break;
                
                default:
                    return "500"; // Operação inválida
            }

            // Salva os novos valores (formatados como String)
            filmeAlvo.setNota(String.format("%.1f", novaMedia).replace(",", ".")); // Formata para 1 casa decimal e usa ponto
            filmeAlvo.setQtd_avaliacoes(String.valueOf(novoN));

            // Salva a lista inteira de filmes de volta no arquivo
            salvarTodosFilmes(filmes);
            return "200";

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }
}