package testes_SD;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ReviewService {

    private static final String REVIEWS_FILE_PATH = "C:\\Users\\paulo\\eclipse-workspace\\DSys\\SDistribuidos\\Reviews.txt";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    public ReviewService() {
        inicializarDatabase();
    }

    private void inicializarDatabase() {
        writeLock.lock();
        try {
            File file = new File(REVIEWS_FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }

    private List<ReviewDBModel> lerTodasReviews() throws IOException {
        return Files.lines(Paths.get(REVIEWS_FILE_PATH))
                .map(ReviewDBModel::deCsvString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void salvarTodasReviews(List<ReviewDBModel> reviews) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(REVIEWS_FILE_PATH, false))) {
            for (ReviewDBModel review : reviews) {
                writer.println(review.paraCsvString());
            }
        }
    }

    public String criarReview(ReviewClass dto, String nomeUsuario) {
        if (dto.getNota() == null || !dto.getNota().matches("[1-5](\\.[0-9])?")) {
            return "405";
        }

        writeLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();

            boolean jaFezReview = reviews.stream().anyMatch(r ->
                    r.getId_filme().equals(dto.getId_filme()) &&
                            r.getNome_usuario().equals(nomeUsuario)
            );

            if (jaFezReview) {
                return "409";
            }

            int maxId = reviews.stream().mapToInt(r -> Integer.parseInt(r.getId())).max().orElse(0);
            String novoId = String.valueOf(maxId + 1);

            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            ReviewDBModel novaReview = new ReviewDBModel(
                    novoId,
                    dto.getId_filme(),
                    nomeUsuario,
                    dto.getNota(),
                    dto.getTitulo(),
                    dto.getDescricao(),
                    dataAtual,
                    "false"
            );

            reviews.add(novaReview);
            salvarTodasReviews(reviews);

            System.out.println("Review criada: ID " + novoId + " por " + nomeUsuario);


            return "201";

        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    public List<ReviewDBModel> listarReviewsDoFilme(String idFilme) {
        readLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();
            return reviews.stream()
                    .filter(r -> r.getId_filme().equals(idFilme))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            readLock.unlock();
        }
    }

    public List<ReviewDBModel> listarReviewsPorUsuario(String nomeUsuario) {
        readLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();
            return reviews.stream()
                    .filter(r -> r.getNome_usuario().equals(nomeUsuario))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            readLock.unlock();
        }
    }

    public ReviewDBModel buscarReviewPorId(String id) {
        readLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();
            return reviews.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public String atualizarReview(String id, String titulo, String descricao, String novaNota) {
        writeLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();

            for (ReviewDBModel r : reviews) {
                if (r.getId().equals(id)) {
                    r.setTitulo(titulo);
                    r.setDescricao(descricao);
                    r.setNota(novaNota);
                    r.setEditado("true");
                    salvarTodasReviews(reviews);
                    return "200";
                }
            }
            return "404";
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    public boolean deletarReview(String id) {
        writeLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();
            boolean removeu = reviews.removeIf(r -> r.getId().equals(id));

            if (removeu) {
                salvarTodasReviews(reviews);
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    public void deletarReviewsDoFilme(String idFilme) {
        writeLock.lock();
        try {
            List<ReviewDBModel> reviews = lerTodasReviews();
            boolean removeu = reviews.removeIf(r -> r.getId_filme().equals(idFilme));

            if (removeu) {
                salvarTodasReviews(reviews);
                System.out.println("Todas as reviews do filme ID " + idFilme + " foram excluídas.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }
}