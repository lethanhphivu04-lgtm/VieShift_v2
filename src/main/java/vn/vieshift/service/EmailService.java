package vn.vieshift.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

/**
 * Gửi email HTML qua SMTP. Khi chưa cấu hình MAIL_HOST, nội dung email được in ra log
 * (chế độ phát triển) để vẫn kiểm thử được toàn bộ quy trình.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ITemplateEngine templateEngine;
    private final JavaMailSenderImpl mailSender;
    private final String from;
    private final String baseUrl;

    public EmailService(ITemplateEngine templateEngine,
                        @Value("${vieshift.mail.host:}") String host,
                        @Value("${vieshift.mail.port:587}") int port,
                        @Value("${vieshift.mail.username:}") String username,
                        @Value("${vieshift.mail.password:}") String password,
                        @Value("${vieshift.mail.from}") String from,
                        @Value("${vieshift.base-url}") String baseUrl) {
        this.templateEngine = templateEngine;
        this.from = from;
        this.baseUrl = baseUrl;
        if (host == null || host.isBlank()) {
            this.mailSender = null;
            log.warn("[EmailService] Chưa cấu hình MAIL_HOST - email sẽ chỉ được ghi ra log.");
        } else {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            sender.setDefaultEncoding("UTF-8");
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            this.mailSender = sender;
        }
    }

    public boolean isEnabled() {
        return mailSender != null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** Render template trong templates/email/{template}.html và gửi đi. */
    public void send(String to, String subject, String template, Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariables(vars);
        ctx.setVariable("baseUrl", baseUrl);
        String html = templateEngine.process("email/" + template, ctx);

        if (mailSender == null) {
            log.info("\n========== [EMAIL - DEV MODE] ==========\nTo: {}\nSubject: {}\nVars: {}\n========================================", to, subject, vars);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EmailService] Đã gửi email '{}' tới {}", subject, to);
        } catch (Exception ex) {
            log.error("[EmailService] Gửi email tới {} thất bại: {}", to, ex.getMessage());
        }
    }
}
