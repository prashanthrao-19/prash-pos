package com.zosh.configrations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {

    @Value("${app.jwt.secret:asdfghjklpoiuytrewqzxcvbnmlkjhglpouhggfdsawqwertyyuiioplmnbvcxzasdfgh}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            JwtConstant.setSecretKey(jwtSecret);
        }
        this.key = Keys.hmacShaKeyFor(JwtConstant.getSecretKey().getBytes());
    }

    public String generateToken(Authentication auth) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .signWith(getKey())
                .compact();
    }

    public String getEmailFromJwtToken(String jwt) {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }
        Claims claims = Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(jwt).getPayload();
        return String.valueOf(claims.get("email"));
    }

    public SecretKey getKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(JwtConstant.getSecretKey().getBytes());
        }
        return key;
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }
}
