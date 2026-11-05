package com.zosh.configrations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtValidator extends OncePerRequestFilter {

    private final String secretKey;

    public JwtValidator() {
        this.secretKey = null;
    }

    public JwtValidator(String secretKey) {
        this.secretKey = secretKey;
    }

    private SecretKey getSigningKey() {
        String keyToUse = (secretKey != null && !secretKey.isBlank()) ? secretKey : JwtConstant.getSecretKey();
        return Keys.hmacShaKeyFor(keyToUse.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7).trim();
            try {
                SecretKey key = getSigningKey();
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(jwt).getPayload();

                String email = String.valueOf(claims.get("email"));
                String authorities = String.valueOf(claims.get("authorities"));

                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Don't throw directly if route might be public; clear security context
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
