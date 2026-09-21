package com.example.sistemaboletos.controller;

import com.example.sistemaboletos.repository.CompraRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CompraController {

    private final CompraRepository compraRepository;

    public CompraController(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    @GetMapping("/mis-compras")
    public String misCompras(Model model) {
        String emailUsuario = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        model.addAttribute("compras", compraRepository.findByUsuarioEmailIgnoreCaseOrderByFechaCompraDesc(emailUsuario));
        return "compras/mis-compras";
    }

    @GetMapping("/compras")
    public String comprasAlias() {
        return "redirect:/mis-compras";
    }

    @GetMapping("/admin/compras")
    public String comprasAdmin(Model model) {
        model.addAttribute("compras", compraRepository.findAllByOrderByFechaCompraDesc());
        return "admin/compras/lista";
    }
}
