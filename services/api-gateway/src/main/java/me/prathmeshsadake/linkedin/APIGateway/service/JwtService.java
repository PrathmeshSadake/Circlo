package me.prathmeshsadake.linkedin.APIGateway.service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    public SecretKey getJwtSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
    }
    public String getUserIdFromToken(String token){
        Claims claims = Jwts.parser()
        .verifyWith(getJwtSecretKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();

        return claims.getSubject();
    }

}
