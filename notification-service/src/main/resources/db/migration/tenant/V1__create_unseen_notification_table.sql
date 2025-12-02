-- Create unseen notification table
CREATE TABLE unseen_notification
(
    notification_id         BIGSERIAL PRIMARY KEY,
    sender_id               VARCHAR(255)             NOT NULL,
    receiver_id             VARCHAR(255)             NOT NULL,
    collection_reference_id BIGINT,
    node_reference_id       BIGINT,
    media_reference_id      BIGINT,
    comment_reference_id    BIGINT,
    action                  BIGINT                   NOT NULL,
    timestamp               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_unseen_notification_receiver_timestamp ON unseen_notification (receiver_id, timestamp DESC);
CREATE INDEX idx_unseen_notification_bundle_lookup ON unseen_notification (receiver_id, action, collection_reference_id, node_reference_id, media_reference_id,
                                                                           comment_reference_id);
-- create seen notification bundle table
CREATE TABLE seen_notification_bundle
(
    uuid                    BIGSERIAL PRIMARY KEY,
    bundle_id               VARCHAR(500),
    receiver_id             VARCHAR(255)             NOT NULL,
    sender_ids              JSONB NOT NULL,
    action                  BIGINT                   NOT NULL,
    collection_reference_id BIGINT,
    node_reference_id       BIGINT,
    media_reference_ids     JSONB,
    comment_reference_id    BIGINT,
    notification_count      INT                      NOT NULL DEFAULT 1,
    first_timestamp         TIMESTAMP WITH TIME ZONE NOT NULL,
    last_timestamp          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seen_bundle_receiver_created ON seen_notification_bundle (receiver_id, created_at DESC);
CREATE INDEX idx_seen_bundle_receiver_last_timestamp ON seen_notification_bundle (receiver_id, last_timestamp DESC);