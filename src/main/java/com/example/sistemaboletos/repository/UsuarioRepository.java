/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.repository;

/**
 *
 * @author straker
 */

import com.example.sistemaboletos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    
    @Modifying
    @Query(value = "ALTER TABLE usuarios AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}
