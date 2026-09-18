/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.model.servicio;

/**
 *
 * @author straker
 */

import com.example.sistemaboletos.model.Compra;
import com.example.sistemaboletos.model.Evento;
import com.example.sistemaboletos.model.Usuario;
import com.example.sistemaboletos.repository.CompraRepository;
import com.example.sistemaboletos.repository.EventoRepository;
import com.example.sistemaboletos.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoServiceImpl implements IEventoService {

    private final EventoRepository eventoRepo;
    private final UsuarioRepository usuarioRepo;
    private final CompraRepository compraRepo;

    @Autowired
    public EventoServiceImpl(EventoRepository eventoRepo, 
                           UsuarioRepository usuarioRepo, 
                           CompraRepository compraRepo) {
        this.eventoRepo = eventoRepo;
        this.usuarioRepo = usuarioRepo;
        this.compraRepo = compraRepo;
    }

    @Override
    public List<Evento> listarTodos() {
        return eventoRepo.findAll();
    }

    @Override
    public Evento guardar(Evento evento) {
        return eventoRepo.save(evento);
    }

    @Override
    public Evento buscarPorId(Integer id) {
        return eventoRepo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        try {
            Evento evento = eventoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));
            eventoRepo.delete(evento);
            
            // Resetear el contador de auto-incremento
            eventoRepo.resetAutoIncrement();
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el evento: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    // public synchronized boolean comprarBoletos(Integer eventoId, Integer cantidad, Integer usuarioId) {
        public synchronized boolean comprarBoletos(Integer eventoId, Integer cantidad, String emailUsuario) {
        Evento evento = eventoRepo.findByIdForUpdate(eventoId).orElse(null);
        Usuario usuario = usuarioRepo.findByEmail(emailUsuario).orElse(null);
        
        if(evento == null || usuario == null || cantidad <= 0) {
            return false;
        }
        
        if(evento.getBoletosDisponibles() >= cantidad) {
            // Actualizar disponibilidad
            evento.setBoletosDisponibles(evento.getBoletosDisponibles() - cantidad);
            eventoRepo.save(evento);
            
            // Registrar compra
            Compra compra = new Compra();
            compra.setEvento(evento);
            compra.setUsuario(usuario);
            compra.setCantidad(cantidad);
            compra.setFechaCompra(LocalDateTime.now());
            compra.setTotal(cantidad * evento.getPrecio());
            compraRepo.save(compra);
            
            return true;
        }
        return false;
    }
}