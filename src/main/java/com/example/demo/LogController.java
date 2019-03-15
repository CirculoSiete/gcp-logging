package com.example.demo;

import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.EntryListOption;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
public class LogController {

  @GetMapping(path = {"/v1/_logs/{log}", "/v1/_logs"}, produces = APPLICATION_OCTET_STREAM_VALUE)
  public @ResponseBody
  byte[] log(HttpServletResponse response,
             @PathVariable(name = "log", required = false) String log,
             @RequestParam(name="items",required = false, defaultValue = "3000") Integer ps) throws Exception {

    String stackdriver_log_name = Optional.ofNullable(log).orElse(System.getenv("STACKDRIVER_LOG_NAME"));

    //Integer


    LoggingOptions options = LoggingOptions.getDefaultInstance();

    try (Logging logging = options.getService();
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      Page<LogEntry> entries =
        logging.listLogEntries(
          EntryListOption.filter(
            "logName=projects/" + options.getProjectId() + "/logs/" + stackdriver_log_name));

      writeLogFile(entries, writer);

      while (entries.hasNextPage()) {
        entries = entries.getNextPage();
        writeLogFile(entries, writer);
      }

      response.addHeader(CONTENT_DISPOSITION, "attachment;filename=\"logs_" + stackdriver_log_name + ".log\"");
      return out.toByteArray();
    }
  }

  private void writeLogFile(Page<LogEntry> entries, BufferedWriter writer) throws Exception {
    for (LogEntry logEntry : entries.iterateAll()) {
      Timestamp ts=new Timestamp(logEntry.getTimestamp());
      Date date=ts;
      System.out.println(date);
      //
      String s = date + " [" + logEntry.getSeverity().toString() + "] " + logEntry.getPayload().getData().toString();
      System.out.println(s);

      //Payload.
      //System.out.println(logEntry.getPayload().getType());

      writer.write(s);
      writer.newLine();
    }
  }
}
