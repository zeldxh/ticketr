/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.model.servicio;

/**
 *
 * @author zeldxh
 */

import com.example.sistemaboletos.model.Evento;
import java.util.List;

public interface IEventoService {
    List<Evento> listarTodos();
    Evento guardar(Evento evento);
    Evento buscarPorId(Integer id);
    void eliminar(Integer id);
    // boolean comprarBoletos(Integer eventoId, Integer cantidad, Integer usuarioId);
    boolean comprarBoletos(Integer eventoId, Integer cantidad, String emailUsuario); // Cambio de Integer a String para el email para la auntetificación de compra en el controlador, temporal???
}