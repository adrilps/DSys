package testes_SD;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public class MovieDBModel {


    private static final String CSV_DELIMITER = ";";
    private static final String GENRE_DELIMITER = "|";

    private String id;
    private String titulo;
    private String diretor;
    private String ano;
    private List<String> genero;
    private String nota;            
    private String qtd_avaliacoes;  
    private String sinopse;


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


    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDiretor() { return diretor; }
    public String getAno() { return ano; }
    public List<String> getGenero() { return genero; }
    public String getNota() { return nota; }
    public String getQtd_avaliacoes() { return qtd_avaliacoes; }
    public String getSinopse() { return sinopse; }


    public void setNota(String nota) { this.nota = nota; }
    public void setQtd_avaliacoes(String qtd_avaliacoes) { this.qtd_avaliacoes = qtd_avaliacoes; }
    

    public void updateDetails(MovieClass movieDTO) {
        this.titulo = movieDTO.getTitulo();
        this.diretor = movieDTO.getDiretor();
        this.ano = movieDTO.getAno();
        this.genero = movieDTO.getGenero();
        this.sinopse = movieDTO.getSinopse();
    }



    public String paraCsvString() {

        String generosString = String.join(GENRE_DELIMITER, this.genero);


        StringJoiner joiner = new StringJoiner(CSV_DELIMITER);
        joiner.add(id);
        joiner.add(titulo);
        joiner.add(diretor);
        joiner.add(ano);
        joiner.add(sinopse); 
        joiner.add(nota);
        joiner.add(qtd_avaliacoes);
        joiner.add(generosString);

        return joiner.toString();
    }


    public static MovieDBModel deCsvString(String csvLine) {
        if (csvLine == null || csvLine.isEmpty()) {
            return null;
        }
        

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
                

                List<String> generos = Arrays.asList(parts[7].split("\\" + GENRE_DELIMITER));
                
                return new MovieDBModel(id, titulo, diretor, ano, sinopse, nota, qtd_avaliacoes, generos);
            } catch (Exception e) {
                System.err.println("Erro ao parsear linha do filme: " + csvLine);
                return null;
            }
        }
        return null;
    }
}