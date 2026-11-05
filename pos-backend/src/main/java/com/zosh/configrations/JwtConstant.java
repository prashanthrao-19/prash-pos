package com.zosh.configrations;

public class JwtConstant {
    private static String secretKey = "asdfghjklpoiuytrewqzxcvbnmlkjhglpouhggfdsawqwertyyuiioplmnbvcxzasdfgh";
    public static final String JWT_HEADER = "Authorization";

    public static synchronized String getSecretKey() {
        return secretKey;
    }

    public static synchronized void setSecretKey(String key) {
        if (key != null && !key.isBlank()) {
            secretKey = key;
        }
    }
}
