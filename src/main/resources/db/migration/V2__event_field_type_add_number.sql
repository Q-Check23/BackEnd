ALTER TABLE form_fields
    MODIFY COLUMN field_type enum ('BOOLEAN', 'SELECT', 'TEXT', 'NUMBER') NOT NULL;
