package com.meridian;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MeridianApiTest {

    private static final String EXPRESS_RULES = """
            {"rules": [
              {"id": "r-1-standard", "match": {"destinationPrefix": "1"}, "priority": 500, "carrierService": "DHL_PAKET"},
              {"id": "r-10-express", "match": {"destinationPrefix": "10"}, "priority": 100, "carrierService": "GLS_EXPRESS"}
            ]}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static MockHttpServletRequestBuilder as(String user, MockHttpServletRequestBuilder request) {
        return request.with(httpBasic(user, "demo")).contentType(MediaType.APPLICATION_JSON);
    }

    private String draftAndSubmit() throws Exception {
        String body = mvc.perform(as("olivia", post("/api/console/changes")).content(EXPRESS_RULES))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        mvc.perform(as("olivia", post("/api/console/changes/{id}/dry-run", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));
        mvc.perform(as("olivia", post("/api/console/changes/{id}/submit", id))).andExpect(status().isOk());
        return id;
    }

    @Test
    void requestsWithoutLoginAreRejected() throws Exception {
        mvc.perform(get("/api/console/rules")).andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCanReadButNotDraft() throws Exception {
        mvc.perform(as("victor", get("/api/console/rules"))).andExpect(status().isOk());
        mvc.perform(as("victor", post("/api/console/changes")).content(EXPRESS_RULES)).andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotApprove() throws Exception {
        String id = draftAndSubmit();

        mvc.perform(as("omar", post("/api/console/changes/{id}/approve", id)).content("{\"comment\": \"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvedChangeRoutesNewParcelsByTheNewRules() throws Exception {
        String id = draftAndSubmit();

        mvc.perform(as("anna", post("/api/console/changes/{id}/approve", id)).content("{\"comment\": \"matches the contract\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activatedVersion").value(2));

        mvc.perform(as("olivia", post("/api/console/parcels")).content("""
                        {"id": "P-1", "profile": {"destinationPostcode": "10115", "serviceLevel": "STANDARD",
                         "weightGrams": 800, "customerTier": "BASIC"}}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carrierService").value("GLS_EXPRESS"));

        String audit = mvc.perform(as("victor", get("/api/console/audit-log"))).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(audit).findValuesAsText("action")).contains("change.approved", "parcel.registered");
    }

    @Test
    void impossibleEventGoesToTheReviewQueueAndDuplicatesAreIgnored() throws Exception {
        mvc.perform(as("olivia", post("/api/console/parcels")).content("""
                        {"id": "P-2", "profile": {"destinationPostcode": "50667", "serviceLevel": "STANDARD",
                         "weightGrams": 800, "customerTier": "BASIC"}}
                        """))
                .andExpect(status().isCreated());
        String delivered = """
                {"eventId": "e-1", "parcelId": "P-2", "type": "DELIVERED"}
                """;

        mvc.perform(as("olivia", post("/api/console/events")).content(delivered))
                .andExpect(jsonPath("$.outcome").value("SENT_TO_REVIEW"));
        mvc.perform(as("olivia", post("/api/console/events")).content(delivered))
                .andExpect(jsonPath("$.outcome").value("DUPLICATE_IGNORED"));
        mvc.perform(as("victor", get("/api/console/review-queue")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void contractImportReadsClausesAndChecksGermanLaw() throws Exception {
        mvc.perform(as("olivia", post("/api/console/contracts/imports")).content("""
                        {"carrierName": "DPD", "text": "1. Express parcels to postcodes starting with 10 are handled by DPD Express.\\n2. Hidden damage must be reported within 24 hours."}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted.length()").value(1))
                .andExpect(jsonPath("$.findings[0].lawReference").value(containsString("§ 438")))
                .andExpect(jsonPath("$.changeId").doesNotExist());
    }

    @Test
    void invalidRulesComeBackAsAListOfProblems() throws Exception {
        mvc.perform(as("olivia", post("/api/console/changes")).content("""
                        {"rules": [{"id": "x", "match": {"destinationPrefix": "ten"}, "priority": 0, "carrierService": "DHL_PAKET"}]}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.problems.length()").value(2));
    }
}
