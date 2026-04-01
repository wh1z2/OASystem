package com.oasystem.util;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenTest {

    @Test
    public void generatePasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("admin123   -> " + encoder.encode("zhangsan123"));
        System.out.println("manager123 -> " + encoder.encode("wangwu123"));
        System.out.println("user123    -> " + encoder.encode("user123"));
    }
}
