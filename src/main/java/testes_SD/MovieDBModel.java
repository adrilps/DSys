package testes_SD;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Representa um registro de Filme completo como armazenado no 'Filmes.txt'.
 * Inclui campos calculados (id, nota, etc.) e lógica de persistência CSV.
 */
public class MovieDBModel {

    // Delimitadores para o arquivo .txt
    private static final String CSV_DELIMITER = ";";
    private static final String GENRE_DELIMITER = "|"; // Usado para separar a lista de gêneros

    private String id;
    private String titulo;
    private String diretor;
    private String ano;
    private List<String> genero;
    private String nota;            // Armazenado como String para consistência do protocolo
    private String qtd_avaliacoes;  // Armazenado como String
    private String sinopse;

    /**
     * Construtor para criar um filme TOTALMENTE NOVO a partir de um DTO.
     * A nota e a contagem são inicializadas como zero.
     */
    public MovieDBModel(String id, MovieClass movieDTO) {
        this.id = id;
        this.titulo = movieDTO.getTitulo();
        this.diretor = movieDTO.getDiretor();
        this.ano = movieDTO.getAno();
        this.genero = movieDTO.getGenero();
        this.sinopse = movieDTO.getSinopse();
        this.nota = "0.0";
        this.qtd_avaliacoes = "0";
    }

    /**
     * Construtor completo usado pelo método 'deCsvString' para re-hidratar
     * um objeto a partir do arquivo de banco de dados.
     */
    public MovieDBModel(String id, String titulo, String diretor, String ano, String sinopse, String nota, String qtd_avaliacoes, List<String> genero) {
        this.id = id;
        this.titulo = titulo;
        this.diretor = diretor;
        this.ano = ano;
        this.sinopse = sinopse;
        this.nota = nota;
        this.qtd_avaliacoes = qtd_avaliacoes;
        this.genero = genero;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDiretor() { return diretor; }
    public String getAno() { return ano; }
    public List<String> getGenero() { return genero; }
    public String getNota() { return nota; }
    public String getQtd_avaliacoes() { return qtd_avaliacoes; }
    public String getSinopse() { return sinopse; }

    // --- Setters (Usados para atualizar o filme) ---
    public void setNota(String nota) { this.nota = nota; }
    public void setQtd_avaliacoes(String qtd_avaliacoes) { this.qtd_avaliacoes = qtd_avaliacoes; }
    
    // (Opcional) Permite que um admin edite os detalhes de um filme
    public void updateDetails(MovieClass movieDTO) {
        this.titulo = movieDTO.getTitulo();
        this.diretor = movieDTO.getDiretor();
        this.ano = movieDTO.getAno();
        this.genero = movieDTO.getGenero();
        this.sinopse = movieDTO.getSinopse();
    }


    // --- Lógica de Persistência (CSV) ---

    /**
     * Converte o objeto MovieDBModel em uma única linha de String para o .txt.
     * Formato: id;titulo;diretor;ano;sinopse;nota;qtd_avaliacoes;genero1|genero2
     */
    public String paraCsvString() {
        // Converte a lista de gêneros em uma string única separada por pipes
        // ex: ["Ação", "Aventura"] -> "Ação|Aventura"
        String generosString = String.join(GENRE_DELIMITER, this.genero);

        // Usar StringJoiner é mais seguro do que concatenar com "+"
        StringJoiner joiner = new StringJoiner(CSV_DELIMITER);
        joiner.add(id);
        joiner.add(titulo);
        joiner.add(diretor);
        joiner.add(ano);
        joiner.add(sinopse); // CUIDADO: Sinopses com ";" quebrarão este formato
        joiner.add(nota);
        joiner.add(qtd_avaliacoes);
        joiner.add(generosString);

        return joiner.toString();
    }

    /**
     * Cria um objeto MovieDBModel a partir de uma linha de String do .txt.
     * Formato: id;titulo;diretor;ano;sinopse;nota;qtd_avaliacoes;genero1|genero2
     */
    public static MovieDBModel deCsvString(String csvLine) {
        if (csvLine == null || csvLine.isEmpty()) {
            return null;
        }
        
        // Limita a 8 partes, pois o último campo (gêneros) pode estar vazio
        String[] parts = csvLine.split(CSV_DELIMITER, 8); 
        
        if (parts.length == 8) {
            try {
                String id = parts[0];
                String titulo = parts[1];
                String diretor = parts[2];
                String ano = parts[3];
                String sinopse = parts[4];
                String nota = parts[5];
                String qtd_avaliacoes = parts[6];
                
                // Converte a string "Ação|Aventura" de volta para uma Lista
                List<String> generos = Arrays.asList(parts[7].split("\\" + GENRE_DELIMITER)); // Precisa escapar o "|"
                
                return new MovieDBModel(id, titulo, diretor, ano, sinopse, nota, qtd_avaliacoes, generos);
            } catch (Exception e) {
                System.err.println("Erro ao parsear linha do filme: " + csvLine);
                return null;
            }
        }
        return null; // Linha mal formatada
    }
}