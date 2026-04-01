package com.oasystem.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    public void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "mySecretKeymySecretKeymySecretKey12");
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 1800000L); // 30分钟
    }

    @Test
    public void testGenerateAndParseToken() {
        String token = jwtTokenUtil.generateToken(1L, "admin");
        assertNotNull(token);

        assertEquals(1L, jwtTokenUtil.getUserIdFromToken(token));
        assertEquals("admin", jwtTokenUtil.getUsernameFromToken(token));
        assertTrue(jwtTokenUtil.validateToken(token));
    }

    @Test
    public void testGenerateExpiredToken() throws InterruptedException {
        // 设置30秒过期，方便测试过期场景
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 30000L);

        String expiredToken = jwtTokenUtil.generateToken(3L, "user");
        assertNotNull(expiredToken);

        // 立刻校验应该是有效的
        assertTrue(jwtTokenUtil.validateToken(expiredToken));
        assertEquals(3L, jwtTokenUtil.getUserIdFromToken(expiredToken));
        assertEquals("user", jwtTokenUtil.getUsernameFromToken(expiredToken));
        System.out.println(expiredToken);

        // 等待32秒后，token已过期
        Thread.sleep(32000);
        assertFalse(jwtTokenUtil.validateToken(expiredToken));
    }

    @Test
    public void testGetExpirationDate() {
        String token = jwtTokenUtil.generateToken(1L, "admin");
        Long remain = jwtTokenUtil.getExpirationDate(token);
        assertNotNull(remain);
        assertTrue(remain > 0);
        assertTrue(remain <= 1800000L);
    }

    @Test
    public void testValidateInvalidToken() {
        assertFalse(jwtTokenUtil.validateToken("invalid_token"));
        assertFalse(jwtTokenUtil.validateToken(""));
    }
}
