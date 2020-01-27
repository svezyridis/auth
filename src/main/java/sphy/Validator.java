package sphy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

@Service
public class Validator {
    @Value("${publicKey}")
    private final  String publicKeyStr;

    @Autowired
    public  Validator(@Value("${publicKey}") String publicKeyStr){
        this.publicKeyStr=publicKeyStr;
    }
    /**
     * @param token
     * @return true if token is valid, false otherwise
     */
    public boolean simpleValidateToken(String token) {
        try {
            ECPublicKey publicKey = ECDSA.reconstructPublicKey(publicKeyStr);
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(Constants.IDENTIFIER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * @param token
     * @return true if token is valid admin token, false otherwise
     */
    public  boolean validateAdminToken(String token){
        try {
            ECPublicKey publicKey = ECDSA.reconstructPublicKey(publicKeyStr);
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(Constants.IDENTIFIER)
                    .withClaim("role",Constants.ADMIN)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * @param token
     * @return true if token is valid teacher token, false otherwise
     */
    public  boolean validateTeacherToken(String token){
        try {
            ECPublicKey publicKey = ECDSA.reconstructPublicKey(publicKeyStr);
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(Constants.IDENTIFIER)
                    .withClaim("role",Constants.TEACHER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public DecodedJWT decode(String token){
        System.out.println("TOKEN===="+token);
        String[] arrOfStr = token.split(" ");
        if(arrOfStr.length>1)
            token=arrOfStr[1].replace("\"", "");
        return JWT.decode(token);
    }
}
