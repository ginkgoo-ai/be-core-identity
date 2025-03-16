-- V1__User_Auth_Schema.sql

CREATE TABLE identity.user_info (
                           id VARCHAR(36) PRIMARY KEY,
                           email VARCHAR(255) NOT NULL,
                           password VARCHAR(255),
                           name VARCHAR(100),
                           first_name VARCHAR(100),
                           last_name VARCHAR(100),
                           picture VARCHAR(500),
                           status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
                           created_at TIMESTAMP(6) NOT NULL,
                           updated_at TIMESTAMP(6)
);

CREATE UNIQUE INDEX idx_user_email ON identity.user_info (email);
CREATE INDEX idx_user_status ON identity.user_info (status);
CREATE INDEX idx_user_created_at ON identity.user_info (created_at);

CREATE TABLE identity.role (
                      id VARCHAR(36) PRIMARY KEY,
                      name VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX idx_role_name ON identity.role (name);

CREATE TABLE identity.user_role (
                           user_id VARCHAR(36) NOT NULL,
                           role_id VARCHAR(36) NOT NULL,
                           PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_user ON identity.user_role (user_id);
CREATE INDEX idx_user_role_role ON identity.user_role (role_id);

CREATE TABLE identity.mfa_info (
                          id VARCHAR(36) PRIMARY KEY,
                          user_id VARCHAR(36) NOT NULL,
                          type VARCHAR(20) CHECK (type IN ('NONE', 'TOTP', 'EMAIL', 'SMS')),
                          secret_key VARCHAR(255),
                          backup_codes VARCHAR(1000),
                          recovery_email VARCHAR(255),
                          attempts_count INTEGER,
                          is_default BOOLEAN NOT NULL,
                          status VARCHAR(20) NOT NULL CHECK (status IN ('DISABLED', 'PENDING', 'ENABLED')),
                          last_verified_at TIMESTAMP(6),
                          created_at TIMESTAMP(6) NOT NULL,
                          updated_at TIMESTAMP(6)
);

CREATE UNIQUE INDEX idx_mfa_user_id ON identity.mfa_info (user_id);
CREATE INDEX idx_mfa_status ON identity.mfa_info (status);
CREATE INDEX idx_mfa_type ON identity.mfa_info (type);

CREATE TABLE identity.oauth2_client_registration (
                                            registration_id VARCHAR(255) PRIMARY KEY,
                                            client_id VARCHAR(255) NOT NULL,
                                            client_secret VARCHAR(255),
                                            client_name VARCHAR(255),
                                            provider_type VARCHAR(50),
                                            provider_display_name VARCHAR(255),
                                            authorization_grant_type VARCHAR(50) NOT NULL,
                                            client_authentication_method VARCHAR(50),
                                            authorization_uri VARCHAR(1000),
                                            token_uri VARCHAR(1000),
                                            user_info_uri VARCHAR(1000),
                                            user_info_authentication_method VARCHAR(50),
                                            redirect_uri VARCHAR(1000),
                                            scopes VARCHAR(1000),
                                            issuer_uri VARCHAR(1000),
                                            jwk_set_uri VARCHAR(1000),
                                            user_name_attribute_name VARCHAR(100),
                                            status VARCHAR(20) DEFAULT 'ACTIVE',
                                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth2_client_registration_client_id ON identity.oauth2_client_registration (client_id);
CREATE INDEX idx_oauth2_client_registration_provider ON identity.oauth2_client_registration (provider_type, status);
CREATE INDEX idx_oauth2_client_registration_provider_active ON identity.oauth2_client_registration (provider_type) WHERE status = 'ACTIVE';

CREATE TABLE identity.oauth2_registered_client (
                                          id VARCHAR(255) PRIMARY KEY,
                                          client_id VARCHAR(255) NOT NULL,
                                          client_secret VARCHAR(255) NOT NULL,
                                          client_name VARCHAR(255) NOT NULL,
                                          client_id_issued_at TIMESTAMP WITH TIME ZONE,
                                          client_secret_expires_at TIMESTAMP WITH TIME ZONE,
                                          client_authentication_methods VARCHAR(255),
                                          authorization_grant_types VARCHAR(255),
                                          redirect_uris VARCHAR(255),
                                          post_logout_redirect_uris VARCHAR(255),
                                          scopes VARCHAR(255),
                                          client_settings TEXT,
                                          token_settings TEXT,
                                          client_secret_raw VARCHAR(255),
                                          created_at TIMESTAMP WITH TIME ZONE,
                                          last_modified_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_oauth2_registered_client_client_id ON identity.oauth2_registered_client (client_id);

CREATE TABLE identity.oauth2_authorization (
                                      id VARCHAR(100) PRIMARY KEY,
                                      registered_client_id VARCHAR(100) NOT NULL,
                                      principal_name VARCHAR(200) NOT NULL,
                                      authorization_grant_type VARCHAR(100) NOT NULL,
                                      authorized_scopes VARCHAR(1000),
                                      attributes TEXT,
                                      state VARCHAR(500),

    -- Authorization Code
                                      authorization_code_value TEXT,
                                      authorization_code_issued_at TIMESTAMP,
                                      authorization_code_expires_at TIMESTAMP,
                                      authorization_code_metadata TEXT,

    -- Access Token
                                      access_token_value TEXT,
                                      access_token_issued_at TIMESTAMP,
                                      access_token_expires_at TIMESTAMP,
                                      access_token_metadata TEXT,
                                      access_token_type VARCHAR(100),
                                      access_token_scopes VARCHAR(1000),

    -- Refresh Token
                                      refresh_token_value TEXT,
                                      refresh_token_issued_at TIMESTAMP,
                                      refresh_token_expires_at TIMESTAMP,
                                      refresh_token_metadata TEXT,

    -- OIDC Token
                                      oidc_id_token_value TEXT,
                                      oidc_id_token_issued_at TIMESTAMP,
                                      oidc_id_token_expires_at TIMESTAMP,
                                      oidc_id_token_metadata TEXT,

    -- Device Flow
                                      user_code_value TEXT,
                                      user_code_issued_at TIMESTAMP,
                                      user_code_expires_at TIMESTAMP,
                                      user_code_metadata TEXT,
                                      device_code_value TEXT,
                                      device_code_issued_at TIMESTAMP,
                                      device_code_expires_at TIMESTAMP,
                                      device_code_metadata TEXT
);

CREATE INDEX idx_oauth2_authorization_client_principal ON identity.oauth2_authorization (registered_client_id, principal_name);
CREATE INDEX idx_oauth2_authorization_principal ON identity.oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_authorization_state ON identity.oauth2_authorization (state) WHERE state IS NOT NULL;
CREATE INDEX idx_oauth2_authorization_access_token_expires ON identity.oauth2_authorization (access_token_expires_at) WHERE access_token_expires_at IS NOT NULL;
CREATE INDEX idx_oauth2_authorization_refresh_token_expires ON identity.oauth2_authorization (refresh_token_expires_at) WHERE refresh_token_expires_at IS NOT NULL;

CREATE TABLE identity.oauth2_authorization_consent (
                                              registered_client_id VARCHAR(100) NOT NULL,
                                              principal_name VARCHAR(200) NOT NULL,
                                              authorities VARCHAR(1000) NOT NULL,
                                              PRIMARY KEY (registered_client_id, principal_name)
);

CREATE TABLE identity.user_social_connection (
                                        id VARCHAR(36) PRIMARY KEY,
                                        user_id VARCHAR(36) NOT NULL,
                                        provider_id VARCHAR(50) NOT NULL,
                                        provider_user_id VARCHAR(100) NOT NULL,
                                        provider_username VARCHAR(255),
                                        access_token VARCHAR(2000),
                                        refresh_token VARCHAR(2000),
                                        token_expires_at TIMESTAMP(6),
                                        created_at TIMESTAMP(6) NOT NULL,
                                        updated_at TIMESTAMP(6)
);

CREATE UNIQUE INDEX idx_social_provider ON identity.user_social_connection (provider_id, provider_user_id);
CREATE INDEX idx_social_user ON identity.user_social_connection (user_id);
CREATE INDEX idx_social_token_expires ON identity.user_social_connection (token_expires_at);

CREATE TABLE identity.jwt_keys (
                          key_id VARCHAR(255) PRIMARY KEY,
                          private_key VARCHAR(4096),
                          public_key VARCHAR(4096),
                          created_at TIMESTAMP(6)
);

CREATE INDEX idx_jwt_keys_created_at ON identity.jwt_keys (created_at);
