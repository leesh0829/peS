package com.pes.lot.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.pes.auth.PesUserDetailsService;
import com.pes.config.SecurityConfig;
import com.pes.lot.application.LotService;

@WebMvcTest(LotController.class)
@Import(SecurityConfig.class)
class LotControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean LotService service;
    @MockitoBean PesUserDetailsService userDetailsService;
    @Test void workerCannotRegisterMaterialLot() throws Exception {
        mvc.perform(post("/api/material-lots").with(user("worker").roles("WORKER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"materialCode\":\"RAW-01\",\"materialName\":\"가상 자재\",\"receivedQuantity\":10}"))
            .andExpect(status().isForbidden());
    }
    @Test void managerCannotInputMaterials() throws Exception {
        mvc.perform(post("/api/work-orders/{id}/materials", UUID.randomUUID()).with(user("manager").roles("MANAGER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"materialLotId\":\"" + UUID.randomUUID() + "\",\"inputQuantity\":1}"))
            .andExpect(status().isForbidden());
    }
}
