package com.ginkgooai.core.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "jwt_keys")
@Data
public class JwtKey {
    @Id
    private String keyId;
    
    @Column(length = 4096)
    private String publicKey;
    
    @Column(length = 4096)
    private String privateKey;
    
    private LocalDateTime createdAt;
}