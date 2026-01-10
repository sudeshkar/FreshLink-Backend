package com.freshlink.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.freshlink.model.User;

public class UserPrincipal implements UserDetails{
	
	private final User user;
	
	 public UserPrincipal(User user) {
	        this.user = user;
	    }
	 
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		 return List.of(
		            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
		        ); 
	}

	@Override
	public @Nullable String getPassword() {
		 return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		 return user.getEmail();
	}
	
	
 @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getEmailVerified();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public User getUser() {
        return user;
    }

}
