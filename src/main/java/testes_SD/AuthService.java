package testes_SD;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AuthService {

    private static final String SECRET_KEY = "seu-segredo-super-secreto-de-32-bytes";
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
    private final JWTVerifier verifier;

    public AuthService() {
        this.verifier = JWT.require(algorithm)
                .withIssuer("VoteFlix")
                .build();
    }


    public String gerarToken(String id, String nome, String role) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(2, ChronoUnit.HOURS);


        String token = JWT.create()
                .withIssuer("VoteFlix")
                .withSubject(id)
                .withClaim("nome", nome)
                .withClaim("funcao", role)
                .withIssuedAt(agora)
                .withExpiresAt(expira)
                .sign(algorithm);

        return token;
    }


    public DecodedJWT validarToken(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT;
        } catch (JWTVerificationException exception) {
            System.err.println("Falha na verificação do JWT: " + exception.getMessage());
            return null;
        }
    }
}