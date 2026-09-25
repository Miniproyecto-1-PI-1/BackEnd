package com.miniproyecto.backend.repository;

import com.miniproyecto.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUser_IdAndNameIgnoreCase(Long userId, String name);
}
