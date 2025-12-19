-- Alter seen_notification_bundle table to change media_reference_ids from JSONB to BIGINT and add media_count
ALTER TABLE seen_notification_bundle
DROP COLUMN media_reference_ids;

ALTER TABLE seen_notification_bundle
ADD COLUMN media_reference_id BIGINT,
ADD COLUMN media_count INT;
