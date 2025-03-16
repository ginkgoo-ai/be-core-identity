-- V1.1__Initial_Data.sql

INSERT INTO identity.oauth2_registered_client (id, authorization_grant_types, client_authentication_methods, client_id,
                                             client_id_issued_at, client_name, client_secret, client_secret_expires_at,
                                             client_secret_raw, client_settings, created_at, last_modified_at,
                                             post_logout_redirect_uris, redirect_uris, scopes, token_settings)
VALUES ('31ba3a02-ec9a-4ef7-9a52-113e54d4fa56', 'authorization_code,refresh_token', 'client_secret_basic',
        'ginkgoo-web-client', '2024-12-30 07:57:26.686486 +00:00', 'ginkgoo-web-client',
        '$2a$10$T6sz1BNbSISw0S7K7VB4DuvaNFRSmB9feUn7eIxsrQburCcP0Onjq', null,
        'bJkgT1OgzUjViJDiOv0msKVBwqVy5IXCmtA1oGN0yB9bcR6CbRbpDEg5pLHHGkQFtZe7AqJ+1bWv+dDF3A96mg==',
        '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
        null, null, 'https://api-test.slatecast.dev',
        'https://api-test.slatecast.dev/login/oauth2/code/ginkgoo-web-client', 'openid,profile,email',
        '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",2592000.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}');


INSERT INTO identity.jwt_keys (key_id,
                      created_at,
                      private_key,
                      public_key)
VALUES ('c6efc41d-1256-4702-9262-9255e78195de',
        '2025-02-07 09:43:26.593308',
        'MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQD3f1e+Aqjr6hnGUi2JGqXIhVN/hz+/H/CVOY5/5QEG7KcRk8IlKceC+Y0vT4FBmWJ4avXE3lDSH+1z9BjVddLt0OabGJyDTSLPj5vbnN40YdvpMvUJSM4TCIQXMQZWJYlZobCWjZdbzIx3A30AOzkrEL0v+Q0I3cKRTIX75haaBQ0HyaBxDzbwRcD1zJJeUTS1E2UWU3ODyFmAr/d5lcHWu3w4waZPJtRnHUSJiH4GoXgsGFCTHuaiRqGs7LbKNpCHnwiFh9W75EJtSAKZKpgfDllolP8loTxIy09gs6ODY24PVdltxzJHhEnMVuL1S9IphbL8LoXb0jAZcshNxf8lAgMBAAECggEAZ0uiOr8Qcnn/rOqSb7UL+S9QJJs1q1awyg8+Hrnc3l958ReawXj4+HygBN1pZJAYbYqyrapdz/0cVTdNABZPZQs5nAS/GfvhKgDVe2iywukO5/rpEylsPLxiqO1AqEa9VwfpVESfJeSuwWNW0fhlyUgQtct7ubYYZR0rlHE5AKmF5Eh7S94Y5LOZqk5Rz+S+HjRn5Uu5aGBHR32cTRtPw+UppQBYd3iEeVVvSJqmS/ob7J+qlE5QVPxdtlgjwVi3Ajlm1MrX6ghOL5DLG/vCe9B/cydZPBfF9ZBTMOn9qmGELsIcU1qexvHERYqTqYX+1tcx76lrVfheRlocrxyWJwKBgQD8ICsfpspUyqtctmBlNsfHnZWA81SKB5+mHo+SZbS9JiGwQqEGftJTuIcrUPA6AiEueOP8Q6bpseSE5VIWvnXz4Hxn8NbQC+TQOTL6b+mDiibSci5ycNfz7lqSIQ6jyopCndz89RPQhrRD1fAsT7DUvXFhcW6N+wwHvasWvSIhawKBgQD7TPeosK+7NZ3tDHdO41SaKXO3d7SgHdf65Scoq7a0xz3XYNQAA9WcI4upbIobI8MU1ikXc697kA46WTGfU0uK507ePO62SNB0q7tFHGaEkwEwGijeRxOasXN4Q4NMz6G+ToH7Js2AnWWJM0VSgZgVzMxHCGi4kqQKbhx9CxI1rwKBgACej5yPS1Xz/YHHzOrSvKDn6TMc5etE44aD8rRYXZ5NVq3ZLA4ZB6k+/c4Eb+KkmLJFh8HZ5LIURsxwx8fYhe+ocIXM2KpALTq5uT9WQQuarke8EhMj5dPqlPxg6mlKmRZazu0sKPVNL2ovP1zMgVG0eW/U2IGuyTPHqoDuzAOrAoGAO3EzS7Wve6rtFjtSpVhklKqlcopgMVed1P0LTDWB3DUczatbuj0t8+b8jDDeYhkNdgpKYf+9TI8ArpCWikdboRmhsDPdi0Sh+lb8zxYuscFMEKK/dASAAKFk3NerYSnj6m4LlOrSxU91ywRTq3Z4EQAGee7lRnN5VfZBytopjoUCgYADcX8GkQimCqqP9TWlpdqp1mAuAGCm3Ew5cQ/PVl1b3GODjiTVTl6AZuvjXlnu57Uf6URiRlgD5dZtdR7ArycZPagGN4MHdWf2rBNiKXakINEn5YmIeDHJVEJHX8q5WdODUgHgNjruj2u9UFmpMMVSVeUnyrKX59XOTHEb8knumg==',
        'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA939XvgKo6+oZxlItiRqlyIVTf4c/vx/wlTmOf+UBBuynEZPCJSnHgvmNL0+BQZlieGr1xN5Q0h/tc/QY1XXS7dDmmxicg00iz4+b25zeNGHb6TL1CUjOEwiEFzEGViWJWaGwlo2XW8yMdwN9ADs5KxC9L/kNCN3CkUyF++YWmgUNB8mgcQ828EXA9cySXlE0tRNlFlNzg8hZgK/3eZXB1rt8OMGmTybUZx1EiYh+BqF4LBhQkx7mokahrOy2yjaQh58IhYfVu+RCbUgCmSqYHw5ZaJT/JaE8SMtPYLOjg2NuD1XZbccyR4RJzFbi9UvSKYWy/C6F29IwGXLITcX/JQIDAQAB');

INSERT INTO identity.role (id, name)
VALUES ('9f1a3cb2-a5ad-473f-b0f0-d63cfa08245b', 'ROLE_USER');
INSERT INTO identity.role (id, name)
VALUES ('db669142-a9be-4ce3-81aa-0bf3831e189d', 'ROLE_ADMIN');

INSERT INTO identity.oauth2_client_registration (registration_id, authorization_grant_type, authorization_uri,
                                        client_authentication_method, client_id, client_name, client_secret, created_at,
                                        issuer_uri, jwk_set_uri, provider_display_name, provider_type, redirect_uri,
                                        scopes, status, token_uri, updated_at, user_info_authentication_method,
                                        user_info_uri, user_name_attribute_name)
VALUES ('google', 'authorization_code', 'https://accounts.google.com/o/oauth2/v2/auth', 'client_secret_basic',
        '504510712661-dlliasc5r38u1dgd1a5jl87bci7u5ire.apps.googleusercontent.com', 'Google',
        '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', '2024-12-30 06:00:04.132949 +00:00', 'https://accounts.google.com',
        'https://www.googleapis.com/oauth2/v3/certs', 'Google', 'GOOGLE',
        'https://auth-test.slatecast.dev/login/oauth2/code/google', 'openid,profile,email', 'ACTIVE',
        'https://www.googleapis.com/oauth2/v4/token', '2024-12-30 06:00:04.132949 +00:00', null,
        'https://www.googleapis.com/oauth2/v3/userinfo', 'sub');
