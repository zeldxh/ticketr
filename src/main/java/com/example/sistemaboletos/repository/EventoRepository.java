/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.sistemaboletos.repository;

/**
 *
 * @author zeldxh
 */

import com.example.sistemaboletos.model.Evento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    @Modifying
    @Query(value = "ALTER TABLE eventos AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();

    // Bloqueo pesimista a nivel de fila (SELECT ... FOR UPDATE) para que la
    // segunda transacción concurrente espere el commit de la primera antes
    // de leer boletosDisponibles, cerrando la ventana de carrera entre el
    // release de synchronized y el commit real de la transacción.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> findByIdForUpdate(@Param("id") Integer id);
}
