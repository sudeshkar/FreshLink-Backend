package com.freshlink.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.freshlink.enums.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
 

@Component
public class JwtUtil {
	
	 @Value("${jwt.secret}")
	    private String secret;

	    @Value("${jwt.expiration}")
	    private long expiration;

	    private SecretKey secretKey;

	    @PostConstruct
	    public void init() {
	        this.secretKey = Keys.hmacShaKeyFor(
	                secret.getBytes(StandardCharsets.UTF_8)
	        );
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
