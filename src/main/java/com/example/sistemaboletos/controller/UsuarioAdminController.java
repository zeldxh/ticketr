package com.example.sistemaboletos.controller;

import com.example.sistemaboletos.model.Usuario;
import com.example.sistemaboletos.model.servicio.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listaUsuarios(Model model) {
        System.out.println("Accediendo a listaUsuarios");
        model.addAttribute("usuarios", usuarioService.findAll());
        return "admin/usuarios/lista";
    }

    @GetMapping("/formulario")
    public String formularioUsuario(@RequestParam(required = false) Integer id, Model model) {
        Usuario usuario = id != null ? 
            usuarioService.findById(id).orElse(new Usuario()) : 
            new Usuario();
        model.addAttribute("usuario", usuario);
        return "admin/usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        usuarioService.save(usuario);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id) {
        usuarioService.deleteById(id);
        return "redirect:/admin/usuarios";
    }
} 