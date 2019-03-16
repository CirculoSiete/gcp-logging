package com.circulosiete.gcp.log.extractor.config;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpLoggingConfig {
  @Bean
  public LoggingOptions loggingOptions() {
    return LoggingOptions.getDefaultInstance();
  }

  @Bean
  public Logging logging(LoggingOptions loggingOptions) {
    return loggingOptions.getService();
  }

}
