package co.edu.uniquindio.servly.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de envío de correos electrónicos.
 * Todos los métodos son @Async para no bloquear el hilo HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── 2FA ──────────────────────────────────────────────────────────────────

    @Async
    public void sendTwoFactorCode(String toEmail, String userName, String code) {
        sendHtmlEmail(toEmail,
                "Servly – Tu código de verificación",
                buildTwoFactorBody(userName, code));
    }

    // ── Reset de contraseña ───────────────────────────────────────────────────

    @Async
    public void sendPasswordResetCode(String toEmail, String userName, String code) {
        sendHtmlEmail(toEmail,
                "Servly – Recupera tu contraseña",
                buildPasswordResetBody(userName, code));
    }

    // ── Bienvenida al nuevo usuario ───────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String userName,
                                 String tempPassword, String role) {
        sendHtmlEmail(toEmail,
                "Servly – Tu cuenta ha sido creada",
                buildWelcomeBody(userName, toEmail, tempPassword, role));
    }

    // ── Envío genérico ────────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email enviado a: {}", to);
        } catch (MessagingException e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage(), e);
        }
    }

    // ── Templates HTML ────────────────────────────────────────────────────────

    private String buildTwoFactorBody(String userName, String code) {
        return """
            <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:500px;margin:auto;background:white;border-radius:8px;padding:32px;">
                <h2 style="color:#2c3e50;">Hola, %s 👋</h2>
                <p style="color:#555;">Tu código de verificación para <strong>Servly</strong> es:</p>
                <div style="text-align:center;margin:32px 0;">
                  <span style="font-size:36px;font-weight:bold;letter-spacing:8px;
                    color:#e74c3c;background:#fdf2f2;padding:12px 24px;border-radius:8px;">%s</span>
                </div>
                <p style="color:#888;font-size:14px;">⏱ Expira en <strong>10 minutos</strong>.</p>
                <p style="color:#888;font-size:14px;">Si no solicitaste este código, ignora este correo.</p>
                <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                <p style="color:#bbb;font-size:12px;text-align:center;">Servly – Gestión de restaurantes</p>
              </div>
            </body></html>
            """.formatted(userName, code);
    }

    private String buildPasswordResetBody(String userName, String code) {
        return """
            <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:500px;margin:auto;background:white;border-radius:8px;padding:32px;">
                <h2 style="color:#2c3e50;">Hola, %s</h2>
                <p style="color:#555;">Usa este código para restablecer tu contraseña en <strong>Servly</strong>:</p>
                <div style="text-align:center;margin:32px 0;">
                  <span style="font-size:36px;font-weight:bold;letter-spacing:8px;
                    color:#2980b9;background:#eaf4fb;padding:12px 24px;border-radius:8px;">%s</span>
                </div>
                <p style="color:#888;font-size:14px;">⏱ Expira en <strong>15 minutos</strong>.</p>
                <p style="color:#888;font-size:14px;">Si no solicitaste este cambio, ignora este correo.</p>
                <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                <p style="color:#bbb;font-size:12px;text-align:center;">Servly – Gestión de restaurantes</p>
              </div>
            </body></html>
            """.formatted(userName, code);
    }

    private String buildWelcomeBody(String userName, String email,
                                    String tempPassword, String role) {
        String roleName = switch (role) {
            case "ADMIN"       -> "Administrador";
            case "CAJERO"      -> "Cajero";
            case "MESERO"      -> "Mesero";
            case "COCINA"      -> "Cocina";
            case "STOREKEEPER" -> "Bodeguero";
            default            -> role;
        };
        return """
            <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:500px;margin:auto;background:white;border-radius:8px;padding:32px;">
                <h2 style="color:#2c3e50;">¡Bienvenido/a a Servly, %s! 🎉</h2>
                <p style="color:#555;">El administrador creó tu cuenta. Tus credenciales de acceso son:</p>
                <table style="width:100%%;margin:24px 0;border-collapse:collapse;">
                  <tr>
                    <td style="padding:10px 12px;background:#f8f9fa;font-weight:bold;color:#2c3e50;font-size:13px;">Correo</td>
                    <td style="padding:10px 12px;background:#f8f9fa;color:#555;font-size:13px;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:10px 12px;background:#eaf4fb;font-weight:bold;color:#2c3e50;font-size:13px;">Contraseña temporal</td>
                    <td style="padding:10px 12px;background:#eaf4fb;font-family:monospace;color:#2980b9;font-size:15px;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:10px 12px;background:#f8f9fa;font-weight:bold;color:#2c3e50;font-size:13px;">Rol</td>
                    <td style="padding:10px 12px;background:#f8f9fa;color:#555;font-size:13px;">%s</td>
                  </tr>
                </table>
                <div style="background:#fff8e1;border-left:4px solid #f39c12;padding:12px 16px;border-radius:4px;margin-bottom:24px;">
                  <p style="margin:0;color:#856404;font-size:13px;">⚠️ <strong>Cambia tu contraseña</strong> después del primer login.</p>
                </div>
                <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                <p style="color:#bbb;font-size:12px;text-align:center;">Servly – Gestión de restaurantes</p>
              </div>
            </body></html>
            """.formatted(userName, email, tempPassword, roleName);
    }
}