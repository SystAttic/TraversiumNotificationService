CREATE TABLE notification_table (
    notification_id BIGSERIAL PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    collection_reference_id BIGINT,
    node_reference_id BIGINT,
    comment_reference_id BIGINT,
    action VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    seen BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_notification_receiver_timestamp ON notification_table(receiver_id, timestamp DESC);
