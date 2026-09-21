/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.controller;

/**
 *
 * @author straker
 */

import com.example.sistemaboletos.model.Evento;
import com.example.sistemaboletos.model.servicio.IEventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IEventoService eventoService;

    @GetMapping
    public String panelAdmin() {
        return "admin/dashboard";
    }
    
    @GetMapping("/eventos")
    public String listarEventos(Model model) {
        model.addAttribute("eventos", eventoService.listarTodos());
        return "admin/eventos/lista";
    }

    @GetMapping("/eventos/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("evento", new Evento());
        return "admin/eventos/formulario";
    }

    @GetMapping("/eventos/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Evento evento = eventoService.buscarPorId(id);
        model.addAttribute("evento", evento);
        return "admin/eventos/formulario";
    }

    @PostMapping("/eventos/guardar")
    public String guardarEvento(@ModelAttribute Evento evento) {
        if(evento.getBoletosDisponibles() == null) {
            evento.setBoletosDisponibles(evento.getCapacidad());
        }
        eventoService.guardar(evento);
        return "redirect:/admin/eventos";
    }

    @GetMapping("/eventos/eliminar/{id}")
    public String eliminarEvento(@PathVariable Integer id) {
        eventoService.eliminar(id);
        return "redirect:/admin/eventos";
    }
}
