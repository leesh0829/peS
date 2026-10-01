package com.pes.quality.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import com.pes.quality.application.QualityService;

@WebMvcTest(QualityController.class)
@Import(SecurityConfig.class)
class QualityControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean QualityService service;
    @MockitoBean PesUserDetailsService userDetailsService;
    @Test void workerCannotInspect() throws Exception {
        mvc.perform(post("/api/product-lots/{id}/inspect", UUID.randomUUID()).with(user("worker").roles("WORKER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"inspectedQuantity\":1,\"acceptedQuantity\":1,\"defects\":[]}"))
            .andExpect(status().isForbidden());
    }
    @Test void workerCannotCreateDefectCode() throws Exception {
        mvc.perform(post("/api/defect-codes").with(user("worker").roles("WORKER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"D-01\",\"name\":\"가상 불량\"}"))
            .andExpect(status().isForbidden());
    }
    @Test void managerMustProvideDefectList() throws Exception {
        mvc.perform(post("/api/product-lots/{id}/inspect", UUID.randomUUID()).with(user("manager").roles("MANAGER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"inspectedQuantity\":1,\"acceptedQuantity\":1}"))
            .andExpect(status().isBadRequest());
    }
    @Test void workerCannotReadCandidates() throws Exception {
        mvc.perform(get("/api/inspection-candidates").with(user("worker").roles("WORKER"))).andExpect(status().isForbidden());
    }
}
