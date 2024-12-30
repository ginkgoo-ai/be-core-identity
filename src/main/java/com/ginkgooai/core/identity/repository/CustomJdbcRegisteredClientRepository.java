package com.ginkgooai.core.identity.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
@RequiredArgsConstructor
public class CustomJdbcRegisteredClientRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RegisteredClientRepository delegate;

    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    public RegisteredClient findById(String id) {
        return delegate.findById(id);
    }

    public RegisteredClient findByClientId(String clientId) {
        return delegate.findByClientId(clientId);
    }

    @Transactional
    public void deleteByClientId(String clientId) {
        if (findByClientId(clientId) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Client not found"
            );
        }

        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE registered_client_id = (" +
                        "SELECT id FROM oauth2_registered_client WHERE client_id = ?)",
                clientId
        );

        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = (" +
                        "SELECT id FROM oauth2_registered_client WHERE client_id = ?)",
                clientId
        );

        int deleted = jdbcTemplate.update(
                "DELETE FROM oauth2_registered_client WHERE client_id = ?",
                clientId
        );

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete client"
            );
        }
    }
}
