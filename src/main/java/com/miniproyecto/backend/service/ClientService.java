package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.ClientRequest;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Client;
import com.miniproyecto.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client findOrCreate(AppUser user, ClientRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            return null;
        }

        String name = request.name().trim();
        return clientRepository.findByUser_IdAndNameIgnoreCase(user.getId(), name)
                .map(existing -> {
                    if (hasText(request.phone())) {
                        existing.setPhone(request.phone().trim());
                    }
                    if (hasText(request.email())) {
                        existing.setEmail(request.email().trim());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setUser(user);
                    client.setName(name);
                    if (hasText(request.phone())) {
                        client.setPhone(request.phone().trim());
                    }
                    if (hasText(request.email())) {
                        client.setEmail(request.email().trim());
                    }
                    return clientRepository.save(client);
                });
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
