package com.benwk.ginkgoocoreidentity.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "role",
        indexes = {
                @Index(name = "idx_name", columnList = "name", unique = true)
        }
)
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}