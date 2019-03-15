CREATE TABLE log_requests
(
  ID                 BIGINT IDENTITY,
  created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
  log_name           VARCHAR(2000),
  items              INTEGER,
  notify_to          VARCHAR(500),
  notification_media VARCHAR(500),
  status             VARCHAR(500) DEFAULT 'NEW'
);