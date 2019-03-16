package com.circulosiete.gcp.log.extractor.util;

import com.circulosiete.gcp.log.extractor.service.LogExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Runner {
  private final LogExtractor logExtractor;

  public Runner(LogExtractor logExtractor) {
    this.logExtractor = logExtractor;
  }

  @Scheduled(fixedDelay = 1000)
  public void logs() {
    log.info("Getting all log requests.");
    logExtractor.procd();
  }
}
