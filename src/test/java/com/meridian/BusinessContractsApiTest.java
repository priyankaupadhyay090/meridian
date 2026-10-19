package com.meridian;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BusinessContractsApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String upload(String user, String text) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "terms.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));
        String body = mvc.perform(multipart("/api/business/contracts").file(file).param("carrierName", "DPD")
                        .with(httpBasic(user, "demo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(user))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asText();
    }

    @Test
    void aBusinessSeesOnlyItsOwnContracts() throws Exception {
        String nordlicht = upload("nordlicht", "5.1 Liability is limited to 1 SDR per kilogram.");
        upload("weserkontor", "6.1 Liability is limited to 8.33 SDR per kilogram.");

        mvc.perform(get("/api/business/contracts").with(httpBasic("nordlicht", "demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(nordlicht));
        mvc.perform(get("/api/business/contracts/{id}/text", nordlicht).with(httpBasic("nordlicht", "demo")))
                .andExpect(jsonPath("$.text").value("5.1 Liability is limited to 1 SDR per kilogram."));
        mvc.perform(get("/api/business/contracts/{id}/file", nordlicht).with(httpBasic("nordlicht", "demo")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void anotherBusinessGetsNotFoundAndCannotDelete() throws Exception {
        String nordlicht = upload("nordlicht", "5.1 Liability is limited to 1 SDR per kilogram.");

        mvc.perform(get("/api/business/contracts/{id}/file", nordlicht).with(httpBasic("weserkontor", "demo")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/business/contracts/{id}/text", nordlicht).with(httpBasic("weserkontor", "demo")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/business/contracts/{id}", nordlicht).with(httpBasic("weserkontor", "demo")))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/business/contracts/{id}", nordlicht).with(httpBasic("nordlicht", "demo")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/business/contracts").with(httpBasic("nordlicht", "demo")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theSameFileUploadedByTwoBusinessesStaysTwoSeparateFiles() throws Exception {
        String first = upload("nordlicht", "same text");
        String second = upload("weserkontor", "same text");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void loginsStayInTheirOwnArea() throws Exception {
        mvc.perform(get("/api/business/contracts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/business/contracts").with(httpBasic("anna", "demo"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/console/rules").with(httpBasic("nordlicht", "demo"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/console/contract-files").with(httpBasic("nordlicht", "demo")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theInternalTeamSeesEveryFileAndEveryLookIsAudited() throws Exception {
        String nordlicht = upload("nordlicht", "5.1 Liability is limited to 1 SDR per kilogram.");
        upload("weserkontor", "6.1 Liability is limited to 8.33 SDR per kilogram.");

        mvc.perform(get("/api/console/contract-files").with(httpBasic("victor", "demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/console/contract-files/{id}/file", nordlicht).with(httpBasic("victor", "demo")))
                .andExpect(status().isOk())
                .andExpect(content().string("5.1 Liability is limited to 1 SDR per kilogram."));

        mvc.perform(get("/api/console/audit-log").with(httpBasic("anna", "demo")))
                .andExpect(content().string(containsString("contract-file.opened-by-staff")))
                .andExpect(content().string(containsString("contract-file.list-viewed")));
    }
}
