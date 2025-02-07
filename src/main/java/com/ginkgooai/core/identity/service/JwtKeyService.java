package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.identity.domain.JwtKey;
import com.ginkgooai.core.identity.repository.JwtKeyRepository;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtKeyService {
    @Autowired
    private JwtKeyRepository jwtKeyRepository;
    
    public RSAKey loadOrCreateKey() {
        return jwtKeyRepository.findFirstByOrderByCreatedAtDesc()
                .map(this::convertToRsaKey)
                .orElseGet(this::generateAndSaveNewKey);
    }
    
    private RSAKey convertToRsaKey(JwtKey jwtKey) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(jwtKey.getPublicKey()))
            );
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(jwtKey.getPrivateKey()))
            );
            
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwtKey.getKeyId())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert stored key", e);
        }
    }
    
    private RSAKey generateAndSaveNewKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            String keyId = UUID.randomUUID().toString();
            
            JwtKey jwtKey = new JwtKey();
            jwtKey.setKeyId(keyId);
            jwtKey.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            jwtKey.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            jwtKey.setCreatedAt(LocalDateTime.now());
            
            jwtKeyRepository.save(jwtKey);
            
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate new key pair", e);
        }
    }
}