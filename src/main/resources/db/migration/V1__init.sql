CREATE TABLE log_requests
(
  ID                 BIGINT IDENTITY,
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  log_name           VARCHAR(2000) NOT NULL,
  items              INTEGER       NOT NULL DEFAULT 2000,
  notify_to          VARCHAR(500)  NOT NULL,
  notification_media VARCHAR(500)  NOT NULL DEFAULT 'EMAIL',
  status             VARCHAR(500)  NOT NULL DEFAULT 'NEW',
  extra_filter       varchar(1000) NOT NULL DEFAULT ''
);