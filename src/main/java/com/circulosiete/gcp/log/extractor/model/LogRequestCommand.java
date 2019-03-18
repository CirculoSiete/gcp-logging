package com.circulosiete.gcp.log.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequestCommand {
  private String logName;
  @Builder.Default
  private Integer items = 3000;
  private String notifyTo;
  @Builder.Default
  private String notificationMedia = "EMAIL";
  @Builder.Default
  private String startDate = "";
  @Builder.Default
  private String endDate = "";
  @Builder.Default
  private String filter = "";
}
