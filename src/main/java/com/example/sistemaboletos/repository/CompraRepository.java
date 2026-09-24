/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.repository;

/**
 *
 * @author zeldxh
 */

import com.example.sistemaboletos.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
    List<Compra> findByUsuarioId(Integer usuarioId);
    List<Compra> findByUsuarioEmailOrderByFechaCompraDesc(String email);
    List<Compra> findByUsuarioEmailIgnoreCaseOrderByFechaCompraDesc(String email);
    List<Compra> findAllByOrderByFechaCompraDesc();
}
