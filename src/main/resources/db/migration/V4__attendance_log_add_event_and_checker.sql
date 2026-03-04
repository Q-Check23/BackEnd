ALTER TABLE attendance_logs
    ADD COLUMN event_id BIGINT NULL,
    ADD COLUMN checked_in_by_user_id BIGINT NULL;

UPDATE attendance_logs al
    JOIN registrations r ON al.registration_id = r.id
SET al.event_id = r.event_id,
    al.checked_in_by_user_id = r.user_id
WHERE al.event_id IS NULL
   OR al.checked_in_by_user_id IS NULL;

ALTER TABLE attendance_logs
    MODIFY COLUMN event_id BIGINT NOT NULL,
    MODIFY COLUMN checked_in_by_user_id BIGINT NOT NULL,
    MODIFY COLUMN method enum ('QR', 'AR', 'MANUAL') NOT NULL;

ALTER TABLE attendance_logs
    ADD CONSTRAINT fk_attendance_logs_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    ADD CONSTRAINT fk_attendance_logs_checked_in_by_user
        FOREIGN KEY (checked_in_by_user_id) REFERENCES users (id);
