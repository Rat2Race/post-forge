package dev.iamrat.security;

import dev.iamrat.security.config.ActuatorSecurityConfig;
import dev.iamrat.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@ExtendWith(SpringExtension.class)
//@ContextConfiguration(classes = {ActuatorSecurityConfig.class, SecurityConfig.class})
//@WebAppConfiguration
//@AutoConfigureMockMvc
//class ActuatorSecurityTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    void actuator_인증없이_접근하면_401() throws Exception {
//        mockMvc.perform(get("/actuator/health"))
//            .andExpect(status().isUnauthorized());
//    }
//}