package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LogRequestRepository {
  public static final String NEW_LOG_REQUESTS = "select id from log_requests where status='NEW'";
  public static final String SINGLE_NEW_LOG_REQUEST = "select * from log_requests where ID = ? and status = ?";
  public static final String UPDATE_TO_DONE_NEW_LOG_REQUEST = "update log_requests set status = ? where id = ? and status = ?";
  private final JdbcTemplate template;

  public LogRequestRepository(JdbcTemplate template) {
    this.template = template;
  }

  public List<Long> newLogRequests() {
    return template.queryForList(NEW_LOG_REQUESTS, Long.class);
  }

  public LogRequestCommand getLogRequestCommand(Long id) {
    return template
      .queryForObject(SINGLE_NEW_LOG_REQUEST,
        new Object[]{id, "NEW"},
        (rs, rowNum) -> LogRequestCommand.builder()
          .id(rs.getLong("ID"))
          .items(rs.getInt("items"))
          .logName(rs.getString("log_name"))
          .notificationMedia(rs.getString("notification_media"))
          .notifyTo(rs.getString("notify_to"))
          .status(rs.getString("status"))
          .build());
  }

  public void update(Long id) {
    template.update(UPDATE_TO_DONE_NEW_LOG_REQUEST, "DONE", id, "NEW");
  }
}
