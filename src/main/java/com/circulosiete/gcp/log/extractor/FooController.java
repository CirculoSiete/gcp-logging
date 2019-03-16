package com.circulosiete.gcp.log.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
public class FooController {
  @GetMapping("/")
  public Map greeting() {
    log.error("Este es un mensaje de error");
    log.warn("Este es un mensaje de warn");
    log.info("Este es un mensaje de info");
    log.debug("Este es un mensaje de debug");
    log.trace("Este es un mensaje de trace");
    return Collections.singletonMap("greeting", "hello");
  }
}
