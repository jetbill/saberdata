package com.saberdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CAPTCHA de Cloudflare Turnstile ({@code app.captcha.*}).
 *
 * @param enabled   si es false no se exige ni se verifica ningún token (útil en desarrollo y en tests)
 * @param secret    clave secreta del sitio; nunca debe llegar al frontend
 * @param verifyUrl endpoint de verificación de Turnstile (siteverify)
 */
@ConfigurationProperties(prefix = "app.captcha")
public record CaptchaProperties(boolean enabled, String secret, String verifyUrl) {

    public CaptchaProperties {
        if (enabled && (secret == null || secret.isBlank())) {
            throw new IllegalStateException("CAPTCHA_SECRET es obligatorio cuando CAPTCHA_ENABLED=true.");
        }
    }
}
