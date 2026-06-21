package org.example.bookshop.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPath {
    public static final String AUTH_REGISTER_URL = "/register";
    public static final String AUTH_LOGIN_URL = "/login";
}
