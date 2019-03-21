package com.circulosiete.gcp.log.extractor.service;

import com.circulosiete.gcp.log.extractor.db.LogRequestRepository;
import com.circulosiete.gcp.log.extractor.model.LogRequest;
import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Transactional
public class LogExtractor {
  public static final int WAITING = 1000 * 60 * 2;
  private final EmailService emailService;
  private final String baseDir;
  private final LoggingOptions loggingOptions;
  private final Logging logging;
  private final LogRequestRepository repository;

  public LogExtractor(EmailService emailService, @Value("${files.baseDir}") String baseDir, LoggingOptions loggingOptions, Logging logging, LogRequestRepository repository) throws IOException {
    this.emailService = emailService;
    this.baseDir = baseDir;
    this.loggingOptions = loggingOptions;
    this.logging = logging;
    this.repository = repository;

    File dir = new File(baseDir);
    if (!dir.exists()) dir.mkdirs();
  }

  public void processPendingLogs() {
    repository.newLogRequests().forEach(this::extractLog);
  }

  public void extractLog(Long id) {
    LogRequest logRequest = repository.getLogRequestCommand(id);

    log.info("Extracting log for id {} and logname {}", id, logRequest.getLogName());
    log.info("Using filter {}", logRequest.getFilter());

    processRequestedLog(logRequest);

    repository.update(id);
    log.info("DONE log id [{}]", id);
  }

  private void processRequestedLog(LogRequest logRequest) {
    log.info("Getting log {}", logRequest.getLogName());
    Assert.hasText(logRequest.getLogName(), "dd");

    String filename =
      String.format("%s/%s_%d.log",
        baseDir,
        logRequest.getLogName(),
        logRequest.getId());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(filename), "utf-8"))) {

      String filter =
        String.format("logName=projects/%s/logs/%s %s",
          loggingOptions.getProjectId(),
          logRequest.getLogName(),
          logRequest.getFilter());

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

    String sourceFile = filename;

    int index = filename.lastIndexOf(".log");
    String zipbase = filename.substring(0, index);

    String zipFile = zipbase + ".txtzip";

    log.warn("Writing zipfile {}", zipFile);

    try (FileOutputStream fos = new FileOutputStream(zipFile);
         ZipOutputStream zipOut = new ZipOutputStream(fos);
    ) {
      File fileToZip = new File(sourceFile);
      FileInputStream fis = new FileInputStream(fileToZip);
      ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
      zipOut.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zipOut.write(bytes, 0, length);
      }
    } catch (Throwable ff) {
      log.error(ff.getMessage(), ff);
    }

    String subject = String
      .format("Envio de log %s %s",
        logRequest.getLogName(),
        new Date());

    try {
      emailService.sendMessageWithAttachment(logRequest.getNotifyTo(), subject, "Aqui va el log", zipFile);
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
    }

    deleteFile(filename);
    deleteFile(zipFile);

  }

  private void deleteFile(String filename) {
    try {

      File file = new File(filename);

      if (file.delete()) {
        log.info(file.getName() + " is deleted!");
      } else {
        log.warn("Delete operation is failed. {}", file.getName());
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
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
      log.info("API quota reached. Waiting...");
      waitFor();
    } else {
      throw new RuntimeException(t.getMessage(), t);
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

  private void writeLogFile(Page<LogEntry> entries, Writer writer) throws Exception {
    for (LogEntry logEntry : entries.iterateAll()) {
      Timestamp ts = new Timestamp(logEntry.getTimestamp());
      Date date = ts;
      String s = String.format("%s [%s] %s", date, logEntry.getSeverity().toString(), logEntry.getPayload().getData().toString());

      writer.write(s);
      writer.write("\n");
      //System.out.println(s);
    }
  }
}
