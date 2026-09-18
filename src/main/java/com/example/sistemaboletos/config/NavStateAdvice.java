package com.example.sistemaboletos.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavStateAdvice {

    @ModelAttribute("activeKey")
    public String activeKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean isAdmin = isAdmin();

        if (uri == null) {
            return isAdmin ? "admin-home" : "eventos";
        }

        if (uri.startsWith("/admin/compras")) {
            return "admin-compras";
        }
        if (uri.startsWith("/admin/usuarios")) {
            return "admin-usuarios";
        }
        if (uri.startsWith("/admin/eventos")) {
            return "admin-eventos";
        }
        if (uri.equals("/admin") || uri.startsWith("/admin/")) {
            return "admin-home";
        }
        if (uri.startsWith("/mis-compras")) {
            return "mis-compras";
        }
        if (uri.startsWith("/eventos")) {
            return isAdmin ? "eventos-publicos" : "eventos";
        }
        if (uri.startsWith("/perfil")) {
            return isAdmin ? "admin-home" : "eventos";
        }

        return isAdmin ? "admin-home" : "eventos";
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
