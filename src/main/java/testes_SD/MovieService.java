package testes_SD;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.Locale;


public class MovieService {


    private static final String MOVIES_FILE_PATH = "C:\\Users\\paulo\\eclipse-workspace\\DSys\\SDistribuidos\\Filmes.txt";
    

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public MovieService() {
        inicializarMovieDatabase();
    }


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


    private List<MovieDBModel> lerTodosFilmes() throws IOException {
        return Files.lines(Paths.get(MOVIES_FILE_PATH))
                .map(MovieDBModel::deCsvString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private void salvarTodosFilmes(List<MovieDBModel> filmes) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MOVIES_FILE_PATH, false))) { // false = sobrescrever
            for (MovieDBModel filme : filmes) {
                writer.println(filme.paraCsvString());
            }
        }
    }


    public String criarFilme(MovieClass movieDTO) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();

            
            boolean jaExiste = filmes.stream().anyMatch(filme -> 
                filme.getTitulo().equalsIgnoreCase(movieDTO.getTitulo()) &&
                filme.getDiretor().equalsIgnoreCase(movieDTO.getDiretor()) &&
                filme.getAno().equals(movieDTO.getAno())
            );

            if (jaExiste) {
                return "409";
            }


            int maxId = filmes.stream()
                              .mapToInt(f -> Integer.parseInt(f.getId()))
                              .max()
                              .orElse(0);
            String novoId = String.valueOf(maxId + 1);


            filmes.add(new MovieDBModel(novoId, movieDTO));
            salvarTodosFilmes(filmes);
            
            System.out.println("Filme criado: " + movieDTO.getTitulo());
            return "201";

        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }


    public List<MovieDBModel> listarTodosFilmes() {
        readLock.lock();
        try {

            return new ArrayList<>(lerTodosFilmes());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            readLock.unlock();
        }
    }


    public String editarFilme(String idFilme, MovieClass movieDTO) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            boolean encontrou = false;

            for (MovieDBModel filme : filmes) {
                if (filme.getId().equals(idFilme)) {
                   
                    filme.updateDetails(movieDTO); 
                    encontrou = true;
                    break;
                }
            }

            if (encontrou) {
                salvarTodosFilmes(filmes);
                System.out.println("Filme ID " + idFilme + " atualizado.");
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }


    public String excluirFilme(String idFilme) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            boolean removeu = filmes.removeIf(filme -> filme.getId().equals(idFilme));

            if (removeu) {
                salvarTodosFilmes(filmes);
                System.out.println("Filme ID " + idFilme + " excluído.");
                // TODO: deletar review do filme junto
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }


    public String recalcularMedia(String idFilme, double notaDaReview, double notaAntiga, String operacao) {
        writeLock.lock();
        try {
            List<MovieDBModel> filmes = lerTodosFilmes();
            MovieDBModel filmeAlvo = null;

            for (MovieDBModel filme : filmes) {
                if (filme.getId().equals(idFilme)) {
                    filmeAlvo = filme;
                    break;
                }
            }

            if (filmeAlvo == null) return "404";

            // Garante que lemos com ponto, mesmo se salvou com vírgula antes
            double mediaAtual = Double.parseDouble(filmeAlvo.getNota().replace(",", "."));
            int n = Integer.parseInt(filmeAlvo.getQtd_avaliacoes());

            double novaMedia = 0.0;
            int novoN = n;

            switch (operacao.toUpperCase()) {
                case "ADD":
                    novoN = n + 1;
                    novaMedia = (mediaAtual * n + notaDaReview) / novoN;
                    break;
                case "DELETE":
                    novoN = n - 1;
                    novaMedia = (novoN == 0) ? 0.0 : (mediaAtual * n - notaDaReview) / novoN;
                    break;
                case "UPDATE":
                    // (media * n) - notaAntiga + notaNova / n
                    if (n > 0) {
                        novaMedia = (mediaAtual * n - notaAntiga + notaDaReview) / n;
                    }
                    break;
            }

            // --- CORREÇÃO DE FORMATAÇÃO ---
            // Usa Locale.US para garantir que o decimal seja PONTO (ex: "4.5")
            String mediaFormatada = String.format(Locale.US, "%.1f", novaMedia);

            filmeAlvo.setNota(mediaFormatada);
            filmeAlvo.setQtd_avaliacoes(String.valueOf(novoN));

            salvarTodosFilmes(filmes);
            return "200";

        } catch (Exception e) {
            e.printStackTrace(); // Veja no console se aparecer erro aqui
            return "500";
        } finally {
            writeLock.unlock();
        }
    }
}