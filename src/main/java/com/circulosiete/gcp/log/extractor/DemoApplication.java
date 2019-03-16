package com.circulosiete.gcp.log.extractor;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DemoApplication {

  @Bean
  public LoggingOptions loggingOptions() {
    return LoggingOptions.getDefaultInstance();
  }

  @Bean
  public Logging logging(LoggingOptions loggingOptions) {
    return loggingOptions.getService();
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

}
