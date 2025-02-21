-- Add name column to user_info table
ALTER TABLE user_info
    ADD COLUMN name VARCHAR(100);

-- Create unique index for name column in user_info table
CREATE UNIQUE INDEX idx_user_name ON user_info(name);