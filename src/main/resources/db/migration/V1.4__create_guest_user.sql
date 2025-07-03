-- V1.4__Create_Guest_User.sql
-- Insert guest user for testing login functionality

INSERT INTO identity.user_info (id,
                                email,
                                password,
                                first_name,
                                last_name,
                                name,
                                status,
                                roles,
                                login_methods,
                                created_at,
                                updated_at)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479', -- Fixed UUID for guest user
        'guest@test.com', -- Email address
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- BCrypt hash for "password" (temporary)
        'Guest', -- First name
        'User', -- Last name
        'Guest User', -- Full name
        'ACTIVE', -- Status (must be ACTIVE to login)
        ARRAY['ROLE_USER'], -- Roles array
        ARRAY['PASSWORD'], -- Login methods array
        NOW(), -- Created at
        NOW() -- Updated at
       ); 