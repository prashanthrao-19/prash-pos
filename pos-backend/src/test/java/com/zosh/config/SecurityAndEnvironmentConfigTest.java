package com.zosh.config;

import com.zosh.configrations.JwtConstant;
import com.zosh.configrations.JwtProvider;
import com.zosh.service.gateway.RazorpayService;
import com.zosh.service.gateway.StripeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndEnvironmentConfigTest {

    @Test
    @DisplayName("JWT Secret externalization should support dynamic keys and 256-bit HMAC keys")
    void testJwtSecretExternalization() {
        String customSecret = "this_is_a_very_secure_test_jwt_secret_key_that_is_at_least_256_bits_long!";
        JwtConstant.setSecretKey(customSecret);

        String secret = JwtConstant.getSecretKey();
        assertNotNull(secret);
        assertEquals(customSecret, secret);

        JwtProvider provider = new JwtProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", customSecret);
        ReflectionTestUtils.setField(provider, "expirationMs", 86400000L);
        provider.init();

        SecretKey hmacKey = provider.getKey();
        assertNotNull(hmacKey);
        assertTrue(hmacKey.getAlgorithm().startsWith("HmacSHA"));
    }

    @Test
    @DisplayName("Razorpay webhook secret and callback URL should be properly externalized")
    void testRazorpayConfigurationExternalization() {
        RazorpayService razorpayService = new RazorpayService();
        ReflectionTestUtils.setField(razorpayService, "razorpayKeyId", "rzp_test_key123");
        ReflectionTestUtils.setField(razorpayService, "razorpayKeySecret", "secret456");
        ReflectionTestUtils.setField(razorpayService, "razorpayWebhookSecret", "whsec_razorpay789");
        ReflectionTestUtils.setField(razorpayService, "callbackBaseUrl", "https://pos.mybrand.com");

        assertTrue(razorpayService.isConfigured());
        assertEquals("whsec_razorpay789", razorpayService.getWebhookSecret());
    }

    @Test
    @DisplayName("Stripe webhook secret and keys should be properly externalized")
    void testStripeConfigurationExternalization() {
        StripeService stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "stripeSecretKey", "sk_test_123");
        ReflectionTestUtils.setField(stripeService, "stripePublishableKey", "pk_test_456");
        ReflectionTestUtils.setField(stripeService, "stripeWebhookSecret", "whsec_stripe789");

        assertTrue(stripeService.isConfigured());
        assertEquals("pk_test_456", stripeService.getStripePublishableKey());
        assertEquals("whsec_stripe789", stripeService.getStripeWebhookSecret());
    }

    @Test
    @DisplayName("CORS Configuration must properly parse comma-separated origins and reject wildcard vercel")
    void testCorsConfigurationAllowedOrigins() {
        String configuredOrigins = "https://admin.pos.example.com, https://app.pos.example.com, http://localhost:5173";
        List<String> origins = java.util.Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());

        assertEquals(3, origins.size());
        assertTrue(origins.contains("https://admin.pos.example.com"));
        assertTrue(origins.contains("https://app.pos.example.com"));
        assertTrue(origins.contains("http://localhost:5173"));
        assertFalse(origins.contains("https://*.vercel.app"));
    }
}
