package com.example.demo;

import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingException;
import com.google.cloud.logging.LoggingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class LogExtractor {
  public static final int WAITING = 1000 * 60 * 2;
  private final JdbcTemplate template;

  public LogExtractor(JdbcTemplate template) {
    this.template = template;
  }

  public void extractLog(Long id) throws Exception {

    LogRequestCommand logRequest = template.queryForObject("select * from log_requests where ID = ? and status = ?", new Object[]{id, "NEW"},
      (rs, rowNum) -> LogRequestCommand.builder()
        .id(rs.getLong("ID"))
        .build());

    kdfsgjhfsdg(logRequest.getLogName());
    System.out.println("DONE log id [" + id + "]");
  }


  private byte[] kdfsgjhfsdg(String stackdriver_log_name) throws Exception {
    log.info("Getting log {}", stackdriver_log_name);
    LoggingOptions options = LoggingOptions.getDefaultInstance();

    try (Logging logging = options.getService();
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      String filter =
        "logName=projects/" + options.getProjectId() +
          "/logs/" + stackdriver_log_name;/* +
          "timestamp>=\\\"2019-03-14T02:15:58.979Z\\\"      timestamp<=\\\"2019-03-15T02:15:58.979Z\\\"";*/

      Optional<Page<LogEntry>> entries1 = getEntries(logging, filter);

      if (!entries1.isPresent()) {
        //TODO: limpiar recursos
        return null;
      }
      Page<LogEntry> entries = entries1.get();

      writeLogFile(entries, writer);

      while (entries.hasNextPage()) {
        boolean success = false;
        while (!success) {
          try {
            entries = entries.getNextPage();
            success = true;
          } catch (LoggingException ex) {

            if (ex.getCause() instanceof ResourceExhaustedException) {
              System.out.println("Entries. API rate limit detected. Waiting a little..");
              Thread.sleep(WAITING);
            } else {
              System.out.println("FAIL: Aborting this execution. Will be performed next time.");
              //TODO: limpiar recursos
              return null;
            }
          }
        }
        writeLogFile(entries, writer);
      }
      return out.toByteArray();
    }
  }

  private Optional<Page<LogEntry>> getEntries(Logging logging, String filter) throws InterruptedException {
    boolean success = false;
    while (!success) {
      try {
        Optional<Page<LogEntry>> logEntryPage = Optional.of(logging.listLogEntries(
          Logging.EntryListOption.filter(filter)));
        success = true;
        return logEntryPage;
      } catch (LoggingException ex) {

        if (ex.getCause() instanceof ResourceExhaustedException) {
          log.info("LOG. API rate limit detected. Waiting a little..", ex);
          Thread.sleep(WAITING);
        }
      }
    }
    return Optional.empty();
  }

  private void writeLogFile(Page<LogEntry> entries, BufferedWriter writer) throws Exception {
    for (LogEntry logEntry : entries.iterateAll()) {
      Timestamp ts = new Timestamp(logEntry.getTimestamp());
      Date date = ts;
      String s = String.format("%s [%s] %s", date, logEntry.getSeverity().toString(), logEntry.getPayload().getData().toString());

      writer.write(s);
      writer.newLine();
    }
  }
}
