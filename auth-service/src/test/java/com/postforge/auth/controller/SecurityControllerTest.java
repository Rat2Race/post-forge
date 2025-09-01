package com.postforge.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postforge.auth.dto.CommonLoginRequest;
import com.postforge.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommonAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityControllerTest {

    @Autowired
    MockMvc mvc;

    ObjectMapper om = new ObjectMapper();

    @Test
    void 컨트롤러_실행_테스트() throws Exception {
        mvc.perform(get("/api/auth/security"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void 잘못된_JSON_전송시_400() throws Exception {
        var badRequest = "{1,1}";

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void JSON_전송시_200_1() throws Exception {
        var request = new CommonLoginRequest("i1234", "p1234");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.id").value("i1234"))
            .andExpect(jsonPath("$.pw").value("p1234"));
    }

    @Test
    void JSON_전송시_200_2() throws Exception {
        var request = new RegisterRequest("test", "i1234", "p1234");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.name").value("test"))
            .andExpect(jsonPath("$.id").value("i1234"))
            .andExpect(jsonPath("$.pw").value("p1234"));
    }
}