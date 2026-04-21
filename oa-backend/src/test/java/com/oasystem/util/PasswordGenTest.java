package com.oasystem.util;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenTest {

    @Test
    public void generatePasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("caiwujl123   -> " + encoder.encode("caiwujl123"));
        System.out.println("renshijl123 -> " + encoder.encode("renshijl123"));
        System.out.println("sysjl123    -> " + encoder.encode("sysjl123"));
        System.out.println("yunwei123    -> " + encoder.encode("yunwei123"));
        System.out.println("zhougong123    -> " + encoder.encode("zhougong123"));
        System.out.println("lili123    -> " + encoder.encode("lili123"));
        System.out.println("wangcai123    -> " + encoder.encode("wangcai123"));
    }
}
