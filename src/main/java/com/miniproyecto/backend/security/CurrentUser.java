package com.miniproyecto.backend.security;

import com.miniproyecto.backend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** Id del usuario autenticado, tomado del subject del JWT. */
    public static long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return Long.parseLong(jwt.getSubject());
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Sesión no válida");
            }
        }
        throw new UnauthorizedException("Sesión no válida");
    }
}
