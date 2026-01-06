package dev.iamrat.login.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class LoginControllerTest {
    
    @Test
    @DisplayName("로그인 실패 : 잘못된 요청 값에 대한 @Valid 테스트")
    public void 예외처리_테스트() throws Exception {
    
    }
    
    @Test
    @DisplayName("로그인 성공 : 올바른 요청 값에 대한 응답 테스트")
    public void 응답_테스트() {
    
    }
    
    @Test
    @DisplayName("로그아웃 실패 : 허락되지 않은 권한으로 접근")
    public void 권한_테스트() {
    
    }
}