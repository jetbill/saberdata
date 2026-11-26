package com.saberdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param captchaToken token del widget Turnstile; es opcional aquí porque solo se exige
 *                     cuando {@code app.captcha.enabled=true} (lo comprueba {@code CaptchaService})
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password,
                           @Size(max = 4096) String captchaToken) {
}
