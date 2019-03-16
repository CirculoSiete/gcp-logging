package com.circulosiete.gcp.log.extractor.api;

import com.circulosiete.gcp.log.extractor.model.LogRequestCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
public class LogController {

  private final JdbcTemplate template;

  public LogController(JdbcTemplate template) {
    this.template = template;
  }

  @ResponseStatus(ACCEPTED)
  @PostMapping("/v1/_logs")
  public Map log(@RequestBody LogRequestCommand logRequest) throws Exception {

    String stackdriver_log_name = Optional.ofNullable(logRequest.getLogName()).orElse(System.getenv("STACKDRIVER_LOG_NAME"));
    logRequest.setLogName(stackdriver_log_name);

    if (!StringUtils.isNotBlank(stackdriver_log_name)) {
      throw new RuntimeException("El nombre del log es requerido.");
    }

    Optional.ofNullable(logRequest.getNotifyTo())
      .orElseThrow(() -> new RuntimeException("Es requerido el email para enviar la notificacion"));

    //TODO: validar multiples solicitudes del mismo log en un periodo corto de tiempo
    String insert = "insert into log_requests (log_name, items, notify_to, notification_media) values (?,?,?,?)";
    template.update(insert,
      stackdriver_log_name, logRequest.getItems(), logRequest.getNotifyTo(), logRequest.getNotificationMedia());

    return Collections.singletonMap("created", true);
  }
}
