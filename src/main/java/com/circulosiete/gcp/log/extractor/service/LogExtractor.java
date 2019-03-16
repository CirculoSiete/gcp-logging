package com.circulosiete.gcp.log.extractor.service;

import com.circulosiete.gcp.log.extractor.db.LogRequestRepository;
import com.circulosiete.gcp.log.extractor.model.LogRequest;
import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional
public class LogExtractor {
  public static final int WAITING = 1000 * 60 * 2;
  private final LoggingOptions loggingOptions;
  private final Logging logging;
  private final LogRequestRepository repository;

  public LogExtractor(LoggingOptions loggingOptions, Logging logging, LogRequestRepository repository) {
    this.loggingOptions = loggingOptions;
    this.logging = logging;
    this.repository = repository;
  }

  public void processPendingLogs() {
    repository.newLogRequests().forEach(this::extractLog);
  }

  public void extractLog(Long id) {
    LogRequest logRequest = repository.getLogRequestCommand(id);

    log.info("Extracting log for id {} and logname {}", id, logRequest.getLogName());

    processRequestedLog(logRequest);

    repository.update(id);
    log.info("DONE log id [{}]", id);
  }

  private void processRequestedLog(LogRequest logRequest) {
    log.info("Getting log {}", logRequest.getLogName());
    Assert.hasText(logRequest.getLogName(), "dd");

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      String filter =
        "logName=projects/" + loggingOptions.getProjectId() +
          "/logs/" + logRequest.getLogName();
      // "timestamp>=\\\"2019-03-14T02:15:58.979Z\\\"      timestamp<=\\\"2019-03-15T02:15:58.979Z\\\""

      Optional<Page<LogEntry>> entries1 = getEntries(logging, filter);

      if (!entries1.isPresent()) {
        //TODO: limpiar recursos
        return;
      }
      Page<LogEntry> entries = entries1.get();

      writeLogFile(entries, writer);

      try {
        while (hasMoreLogData(entries)) {
          boolean success = false;
          while (!success) {
            try {
              entries = entries.getNextPage();
              success = true;
            } catch (Throwable ex) {

              if (ex.getCause() instanceof ResourceExhaustedException) {
                log.info("Entries. API rate limit detected. Waiting a little.. {} millis", WAITING);
                Thread.sleep(WAITING);
              } else {
                log.error("FAIL: Aborting this execution. Will be performed next time.");
                //TODO: limpiar recursos
                return;
              }
            }
          }
          writeLogFile(entries, writer);
        }
      } catch (Throwable t) {
        waitIfResourceExhaustedException(t);
        /*if (t.getCause() instanceof ResourceExhaustedException) {
          log.info("Entries. API rate limit detected. Waiting a little.. {} millis", WAITING);
          Thread.sleep(WAITING);
          //TODO: limpiar recursos
          return;
        }*/
      }

      //return out.toByteArray();
    } catch (Throwable e) {
      log.warn(e.getMessage(), e);
    }
  }

  public <T> T retryWithWait(Supplier<T> f) {
    boolean success = false;
    T result = null;
    while (!success) {
      try {
        result = f.get();
        success = true;
      } catch (Throwable t) {
        waitIfResourceExhaustedException(t);
      }
    }

    return result;
  }

  private void waitIfResourceExhaustedException(Throwable t) {
    if (t.getCause() instanceof ResourceExhaustedException) {
      waitFor();
    }
  }

  private boolean hasMoreLogData(Page<LogEntry> entries) {
    return retryWithWait(entries::hasNextPage);
  }

  private void waitFor() {
    try {
      Thread.sleep(WAITING);
    } catch (InterruptedException e) {
      log.error("Very SAD...");
    }
  }

  private Optional<Page<LogEntry>> getEntries(Logging logging, String filter) {
    return retryWithWait(() ->
      Optional.of(logging.listLogEntries(
        Logging.EntryListOption
          .filter(filter))));
  }

  private void writeLogFile(Page<LogEntry> entries, BufferedWriter writer) throws Exception {
    for (LogEntry logEntry : entries.iterateAll()) {
      Timestamp ts = new Timestamp(logEntry.getTimestamp());
      Date date = ts;
      String s = String.format("%s [%s] %s", date, logEntry.getSeverity().toString(), logEntry.getPayload().getData().toString());

      writer.write(s);
      writer.newLine();
      System.out.println(s);
    }
  }
}
