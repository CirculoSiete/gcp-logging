package com.circulosiete.gcp.log.extractor.db;

import com.circulosiete.gcp.log.extractor.model.LogRequest;
import com.circulosiete.gcp.log.extractor.model.LogRequestCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LogRequestRepository {
  public static final String INSERT_LOG_REQUEST = "insert into log_requests (log_name, items, notify_to, notification_media) values (?,?,?,?)";
  public static final String NEW_LOG_REQUESTS = "select id from log_requests where status='NEW'";
  public static final String SINGLE_NEW_LOG_REQUEST = "select * from log_requests where ID = ? and status = ?";
  public static final String UPDATE_TO_DONE_NEW_LOG_REQUEST = "update log_requests set status = ? where id = ? and status = ?";
  private final JdbcTemplate template;

  public LogRequestRepository(JdbcTemplate template) {
    this.template = template;
  }

  public void insert(LogRequestCommand logRequest) {
    if (!StringUtils.isNotBlank(logRequest.getLogName())) {
      throw new RuntimeException("El nombre del log es requerido.");
    }

    Optional.ofNullable(logRequest.getNotifyTo())
      .orElseThrow(() -> new RuntimeException("Es requerido el email para enviar la notificacion"));
    //TODO: validar multiples solicitudes del mismo log en un periodo corto de tiempo

    template.update(
      INSERT_LOG_REQUEST,
      logRequest.getLogName(),
      logRequest.getItems(),
      logRequest.getNotifyTo(),
      logRequest.getNotificationMedia());
  }

  public List<Long> newLogRequests() {
    return template.queryForList(NEW_LOG_REQUESTS, Long.class);
  }

  public LogRequest getLogRequestCommand(Long id) {
    return template
      .queryForObject(SINGLE_NEW_LOG_REQUEST,
        new Object[]{id, "NEW"},
        (rs, rowNum) -> LogRequest.builder()
          .id(rs.getLong("ID"))
          .items(rs.getInt("items"))
          .logName(rs.getString("log_name"))
          .notificationMedia(rs.getString("notification_media"))
          .notifyTo(rs.getString("notify_to"))
          .status(rs.getString("status"))
          .build());
  }

  public void update(Long id) {
    template.update(
      UPDATE_TO_DONE_NEW_LOG_REQUEST,
      "DONE", id, "NEW");
  }
}
