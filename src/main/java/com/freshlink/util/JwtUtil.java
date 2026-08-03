package com.freshlink.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.freshlink.enums.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
 

@Component
public class JwtUtil {
	
	 @Value("${jwt.secret}")
	    private String secret;

	    @Value("${jwt.expiration}")
	    private long expiration;

	    private SecretKey secretKey;

	    /** HS256 requires a key of at least 256 bits; jjwt rejects anything shorter. */
	    private static final int MIN_SECRET_BYTES = 32;

	    @PostConstruct
	    public void init() {
	        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
	        if (keyBytes.length < MIN_SECRET_BYTES) {
	            throw new IllegalStateException(
	                    "jwt.secret must be at least %d bytes (%d given). Set the JWT_SECRET environment variable to a long random value."
	                            .formatted(MIN_SECRET_BYTES, keyBytes.length));
	        }
	        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	    }

	    public String generateToken(String email, Role role) { 
	    	return Jwts.builder()
	                .setSubject(email)
	                .claim("role", role.name())
	                .setIssuedAt(new Date())
	                .setExpiration(new Date(System.currentTimeMillis() + expiration))
	                .signWith(secretKey)
	                .compact();
	    }

	    public String extractRole(String token) {
	    	 return Jwts.parserBuilder()
	    	            .setSigningKey(secretKey)
	    	            .build()
	    	            .parseClaimsJws(token)
	    	            .getBody()
	    	            .get("role", String.class); 
	    }

	    public String extractEmail(String token) {
	    	 return Jwts.parserBuilder()
	    	            .setSigningKey(secretKey)
	    	            .build()
	    	            .parseClaimsJws(token)
	    	            .getBody()
	    	            .getSubject();
	    }

	    public boolean validateToken(String token) {
	        try {
	        	Jwts.parserBuilder()
	            .setSigningKey(secretKey)
	            .build()
	            .parseClaimsJws(token);
	        	return true;
	        } catch (JwtException | IllegalArgumentException e) {
	            return false;
	        }
	    }
}
