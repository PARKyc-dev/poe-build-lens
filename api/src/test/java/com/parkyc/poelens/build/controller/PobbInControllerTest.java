package com.parkyc.poelens.build.controller;

import com.parkyc.poelens.build.service.PobbInClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PobbInControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PobbInClient pobbInClient;

    @Test
    void returnsRawPobCodeForShareId() throws Exception {
        when(pobbInClient.fetch("AbC_123-xy")).thenReturn("eNrawPobCode");

        mockMvc.perform(get("/api/pobb-in/AbC_123-xy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject").value("eNrawPobCode"));
    }
}
