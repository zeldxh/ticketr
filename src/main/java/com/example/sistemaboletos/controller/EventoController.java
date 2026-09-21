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
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.Model;
 import org.springframework.web.bind.annotation.*;
 
 @Controller
 @RequestMapping("/eventos")
 public class EventoController {
 
     @Autowired
     private IEventoService eventoService;
 
     @GetMapping
     public String listarEventos(Model model) {
         model.addAttribute("eventos", eventoService.listarTodos());
         return "eventos/lista";
     }
 
     @GetMapping("/{id}")
     public String verEvento(@PathVariable Integer id, Model model) {
         Evento evento = eventoService.buscarPorId(id);
         if(evento == null) {
             return "redirect:/eventos";
         }
         model.addAttribute("evento", evento);
         return "eventos/detalle";
     }
 
     @PostMapping("/comprar/{id}")
     public String comprarBoletos(@PathVariable Integer id, @RequestParam Integer cantidad) {
         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         String email = auth.getName(); // Obtener el email del usuario autenticado
         
         boolean exito = eventoService.comprarBoletos(id, cantidad, email); // Pasar el email en lugar del ID
         
         if(exito) {
             return "redirect:/eventos?compra_exitosa";
         }
         return "redirect:/eventos/" + id + "?error=sin_disponibilidad";
     }
 }