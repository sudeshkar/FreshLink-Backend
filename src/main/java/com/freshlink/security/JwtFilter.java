package com.freshlink.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.freshlink.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{
	
 
	 private final JwtUtil jwtUtil;
	 private final AccountStatusService accountStatusService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, 
									HttpServletResponse response, 
									FilterChain filterChain)
			throws ServletException, IOException {
		String authHeaders =request.getHeader("Authorization");
        if (authHeaders!=null && authHeaders.startsWith("Bearer ")){
            String token = authHeaders.substring(7);
            if (jwtUtil.validateToken(token)){
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                // A signed token stays valid until it expires, so without this a
                // suspended or removed account would keep working for the rest of
                // its lifetime. The lookup is cached, so this costs roughly one
                // query per user per minute rather than one per request.
                if (!accountStatusService.isUsable(email)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request,response);
		
	}

}
