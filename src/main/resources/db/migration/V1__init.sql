-- Initialize Database Settings
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

------------------------------------------
-- Core User Management Tables
------------------------------------------
CREATE TABLE user_info (
                           id VARCHAR(36) NOT NULL,
                           email VARCHAR(255) NOT NULL,
                           password VARCHAR(255),
                           first_name VARCHAR(100),
                           last_name VARCHAR(100),
                           picture VARCHAR(500),
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP(6) NOT NULL,
                           updated_at TIMESTAMP(6),
                           CONSTRAINT user_info_pkey PRIMARY KEY (id),
                           CONSTRAINT idx_email UNIQUE (email),
                           CONSTRAINT user_info_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE INDEX idx_user_status ON user_info(status);
CREATE INDEX idx_user_created_at ON user_info(created_at);

CREATE TABLE role (
                      id VARCHAR(36) NOT NULL,
                      name VARCHAR(50) NOT NULL,
                      CONSTRAINT role_pkey PRIMARY KEY (id),
                      CONSTRAINT idx_role_name UNIQUE (name)
);

INSERT INTO public.role (id, name)
VALUES
    ('9f1a3cb2-a5ad-473f-b0f0-d63cfa08245b', 'ROLE_USER'),
    ('db669142-a9be-4ce3-81aa-0bf3831e189d', 'ROLE_ADMIN');

CREATE TABLE user_role (
                           user_id VARCHAR(36) NOT NULL,
                           role_id VARCHAR(36) NOT NULL,
                           CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_user ON user_role(user_id);
CREATE INDEX idx_user_role_role ON user_role(role_id);

------------------------------------------
-- MFA Related Tables
------------------------------------------
CREATE TABLE mfa_info (
                          id VARCHAR(36) NOT NULL,
                          user_id VARCHAR(36) NOT NULL,
                          type VARCHAR(20),
                          secret_key VARCHAR(255),
                          backup_codes VARCHAR(1000),
                          recovery_email VARCHAR(255),
                          attempts_count INTEGER,
                          is_default BOOLEAN NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          last_verified_at TIMESTAMP(6),
                          created_at TIMESTAMP(6) NOT NULL,
                          updated_at TIMESTAMP(6),
                          CONSTRAINT mfa_info_pkey PRIMARY KEY (id),
                          CONSTRAINT idx_mfa_user_id UNIQUE (user_id),
                          CONSTRAINT mfa_info_status_check CHECK (status IN ('DISABLED', 'PENDING', 'ENABLED')),
                          CONSTRAINT mfa_info_type_check CHECK (type IN ('NONE', 'TOTP', 'EMAIL', 'SMS'))
);

CREATE INDEX idx_mfa_status ON mfa_info(status);

------------------------------------------
-- OAuth2 Related Tables
------------------------------------------
CREATE TABLE oauth2_client_registration (
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

CREATE INDEX idx_oauth2_client_registration_client_id ON oauth2_client_registration(client_id);
CREATE INDEX idx_oauth2_client_registration_provider ON oauth2_client_registration(provider_type, status);
CREATE INDEX idx_oauth2_client_registration_provider_status
    ON oauth2_client_registration(provider_type, status) WHERE status = 'ACTIVE';

CREATE TABLE oauth2_registered_client (
                                          id VARCHAR(255) NOT NULL,
                                          client_id VARCHAR(255) NOT NULL,
                                          client_secret VARCHAR(255) NOT NULL,
                                          client_name VARCHAR(255) NOT NULL,
                                          client_id_issued_at TIMESTAMP(6) WITH TIME ZONE,
                                          client_secret_expires_at TIMESTAMP(6) WITH TIME ZONE,
                                          client_authentication_methods VARCHAR(255),
                                          authorization_grant_types VARCHAR(255),
                                          redirect_uris VARCHAR(255),
                                          post_logout_redirect_uris VARCHAR(255),
                                          scopes VARCHAR(255),
                                          client_settings TEXT,
                                          token_settings TEXT,
                                          client_secret_raw VARCHAR(255),
                                          created_at TIMESTAMP(6) WITH TIME ZONE,
                                          last_modified_at TIMESTAMP(6) WITH TIME ZONE,
                                          CONSTRAINT oauth2_registered_client_pkey PRIMARY KEY (id),
                                          CONSTRAINT uk_oauth2_client_id UNIQUE (client_id)
);

CREATE TABLE oauth2_authorization (
                                      id VARCHAR(100) NOT NULL,
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
                                      device_code_metadata TEXT,

                                      CONSTRAINT oauth2_authorization_pkey PRIMARY KEY (id)
);

-- OAuth2 Authorization Indexes
CREATE INDEX idx_oauth2_authorization_client_principal ON oauth2_authorization(registered_client_id, principal_name);
CREATE INDEX idx_oauth2_authorization_access_token ON oauth2_authorization(access_token_value);
CREATE INDEX idx_oauth2_authorization_refresh_token ON oauth2_authorization(refresh_token_value);
CREATE INDEX idx_oauth2_authorization_code ON oauth2_authorization(authorization_code_value);
CREATE INDEX idx_oauth2_authorization_access_token_expires
    ON oauth2_authorization(access_token_expires_at) WHERE access_token_expires_at IS NOT NULL;
CREATE INDEX idx_oauth2_authorization_refresh_token_expires
    ON oauth2_authorization(refresh_token_expires_at) WHERE refresh_token_expires_at IS NOT NULL;
CREATE INDEX idx_oauth2_authorization_state ON oauth2_authorization(state) WHERE state IS NOT NULL;
CREATE INDEX idx_oauth2_authorization_principal ON oauth2_authorization(principal_name);

CREATE TABLE oauth2_authorization_consent (
                                              registered_client_id VARCHAR(100) NOT NULL,
                                              principal_name VARCHAR(200) NOT NULL,
                                              authorities VARCHAR(1000) NOT NULL,
                                              CONSTRAINT oauth2_authorization_consent_pkey PRIMARY KEY (registered_client_id, principal_name)
);

------------------------------------------
-- Social Connection Tables
------------------------------------------
CREATE TABLE user_social_connection (
                                        id VARCHAR(36) NOT NULL,
                                        user_id VARCHAR(36) NOT NULL,
                                        provider_id VARCHAR(50) NOT NULL,
                                        provider_user_id VARCHAR(100) NOT NULL,
                                        provider_username VARCHAR(255),
                                        access_token VARCHAR(2000),
                                        refresh_token VARCHAR(2000),
                                        token_expires_at TIMESTAMP(6),
                                        created_at TIMESTAMP(6) NOT NULL,
                                        updated_at TIMESTAMP(6),
                                        CONSTRAINT user_social_connection_pkey PRIMARY KEY (id),
                                        CONSTRAINT idx_social_provider UNIQUE (provider_id, provider_user_id)
);

CREATE INDEX idx_social_user ON user_social_connection(user_id);

------------------------------------------
-- JWT Related Tables
------------------------------------------
CREATE TABLE jwt_keys (
                          key_id VARCHAR(255) NOT NULL,
                          private_key VARCHAR(4096),
                          public_key VARCHAR(4096),
                          created_at TIMESTAMP(6),
                          CONSTRAINT jwt_keys_pkey PRIMARY KEY (key_id)
);

CREATE INDEX idx_jwt_keys_created_at ON jwt_keys(created_at);

------------------------------------------
-- Workspace Related Tables
------------------------------------------
CREATE TABLE workspace (
                           id VARCHAR(36) NOT NULL,
                           name VARCHAR(100) NOT NULL,
                           description VARCHAR(500),
                           logo_url VARCHAR(255),
                           owner_id VARCHAR(36) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP(6) NOT NULL,
                           updated_at TIMESTAMP(6),
                           CONSTRAINT workspace_pkey PRIMARY KEY (id),
                           CONSTRAINT workspace_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_workspace_owner ON workspace(owner_id);
CREATE INDEX idx_workspace_status ON workspace(status);
