package com.circulosiete.gcp.log.extractor.api;

import com.circulosiete.gcp.log.extractor.db.LogRequestRepository;
import com.circulosiete.gcp.log.extractor.model.LogRequestCommand;
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

  private final LogRequestRepository repository;

  public LogController(LogRequestRepository repository) {
    this.repository = repository;
  }

  @ResponseStatus(ACCEPTED)
  @PostMapping("/v1/_logs")
  public Map log(@RequestBody LogRequestCommand logRequest) {

    String logName = Optional.ofNullable(logRequest.getLogName()).orElse(System.getenv("STACKDRIVER_LOG_NAME"));
    logRequest.setLogName(logName);

    repository.insert(logRequest);

    return Collections.singletonMap("created", true);
  }
}
