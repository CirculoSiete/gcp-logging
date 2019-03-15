package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class Runner {
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
  private final LogExtractor logExtractor;
  private final JdbcTemplate jdbcTemplate;

  public Runner(LogExtractor logExtractor, JdbcTemplate jdbcTemplate) {
    this.logExtractor = logExtractor;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Scheduled(fixedDelay = 5000)
  public void reportCurrentTime() {

    System.out.println("Getting all log requests. \n\tThe time is now " + dateFormat.format(new Date()));
    jdbcTemplate
      .queryForList("select id from log_requests where status='NEW'")
      .forEach(stringObjectMap -> {
        Long id = (Long) stringObjectMap.get("ID");
        try {
          logExtractor.extractLog(id);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
  }
}
