package com.circulosiete.gcp.log.extractor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;

@Component
public class EmailService {

  private final JavaMailSender emailSender;
  private final String from;

  public EmailService(JavaMailSender emailSender, @Value("${email.from:no-reply@circulosiete.com}") String from) {
    this.emailSender = emailSender;
    this.from = from;
  }

  public void sendMessageWithAttachment(String to, String subject,
                                        String text, String pathToAttachment) {
    MimeMessage message = emailSender.createMimeMessage();

    MimeMessageHelper helper = null;
    try {
      int idx = pathToAttachment.replaceAll("\\\\", "/").lastIndexOf("/");
      String singleFileName = idx >= 0 ? pathToAttachment.substring(idx + 1) : pathToAttachment;

      helper = new MimeMessageHelper(message, true);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text);
      FileSystemResource file
        = new FileSystemResource(new File(pathToAttachment));
      helper.addAttachment(singleFileName, file);

      helper.setFrom(from);
      emailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
