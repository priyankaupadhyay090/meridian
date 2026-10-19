package com.meridian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PublicApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    @Test
    void publishedRatesCarryTheirSource() throws Exception {
        mvc.perform(get("/api/public/couriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").isNotEmpty())
                .andExpect(jsonPath("$[0].checkedOn").isNotEmpty());
    }

    @Test
    void quoteComparesEveryCourierThatTakesTheParcel() throws Exception {
        String parcel = json(Map.of("weightGrams", 900, "lengthCm", 30, "widthCm", 20, "heightCm", 10));

        mvc.perform(post("/api/public/quotes").contentType(MediaType.APPLICATION_JSON).content(parcel))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courier").value("DHL"))
                .andExpect(jsonPath("$[0].priceCents").value(419))
                .andExpect(jsonPath("$[0].labels", hasItems("CHEAPEST")))
                .andExpect(jsonPath("$[*].courier", hasItems("DHL", "HERMES", "DPD", "GLS")));
    }

    @Test
    void impossibleParcelIsRejected() throws Exception {
        String parcel = json(Map.of("weightGrams", 0, "lengthCm", 30, "widthCm", 20, "heightCm", 10));

        mvc.perform(post("/api/public/quotes").contentType(MediaType.APPLICATION_JSON).content(parcel))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutShowsOnlyTheShopsCouriersAndItsOwnPrice() throws Exception {
        String request = json(Map.of(
                "parcel", Map.of("weightGrams", 900, "lengthCm", 30, "widthCm", 20, "heightCm", 10),
                "shopDefault", "HERMES", "shopShippingCents", 0, "couriers", new String[]{"HERMES", "GLS"}));

        mvc.perform(post("/api/public/checkout-options").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].courier").value("HERMES"))
                .andExpect(jsonPath("$[0].priceCents").value(0))
                .andExpect(jsonPath("$[0].labels", hasItems("SHOP_DEFAULT", "CHEAPEST")));
    }

    @Test
    void contractWithTermsAgainstTheLawCannotBecomeADraft() throws Exception {
        JsonNode example = mapper.readTree(mvc.perform(get("/api/public/contract-checks/examples"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get(0);

        mvc.perform(post("/api/public/contract-checks").contentType(MediaType.APPLICATION_JSON).content(json(example)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDraft").value(false))
                .andExpect(jsonPath("$.accepted.length()").value(2))
                .andExpect(jsonPath("$.findings[*].ruleId",
                        hasItems("HGB-449-2-LIABILITY", "HGB-438-2-HIDDEN", "HGB-439-4-LIMITATION")));
    }

    @Test
    void instructionHiddenInAGermanContractIsLeftForAPerson() throws Exception {
        JsonNode example = mapper.readTree(mvc.perform(get("/api/public/contract-checks/examples"))
                .andReturn().getResponse().getContentAsString()).get(2);

        String body = mvc.perform(post("/api/public/contract-checks").contentType(MediaType.APPLICATION_JSON)
                        .content(json(example)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDraft").value(true))
                .andExpect(jsonPath("$.accepted[0].carrierService").value("GLS_EXPRESS"))
                .andExpect(jsonPath("$.findings[*].ruleId", hasItems("HGB-449-2-NOTICE", "POSTG-73-1-LABEL")))
                .andReturn().getResponse().getContentAsString();

        assertThat(mapper.readTree(body).get("unresolved").get(0).get("reason").asText()).contains("instruction");
    }

    @Test
    void everySampleContractGivesTheResultItIsMeantToShow() throws Exception {
        JsonNode examples = mapper.readTree(mvc.perform(get("/api/public/contract-checks/examples"))
                .andReturn().getResponse().getContentAsString());
        Map<String, Boolean> canDraft = Map.of("liability-cut", false, "negotiated", true, "heavy-parcels-de", true,
                "consumer-returns", false, "delay-excluded", false, "clean", true, "unclear", false);

        assertThat(examples).hasSize(canDraft.size());
        for (JsonNode example : examples) {
            String body = mvc.perform(post("/api/public/contract-checks").contentType(MediaType.APPLICATION_JSON)
                            .content(json(example)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(mapper.readTree(body).get("canDraft").asBoolean())
                    .as(example.get("id").asText())
                    .isEqualTo(canDraft.get(example.get("id").asText()));
        }
    }

    @Test
    void sampleRuleChangesRunAgainstTheLiveRulesWithoutChangingThem() throws Exception {
        JsonNode examples = mapper.readTree(mvc.perform(get("/api/public/rule-changes/examples"))
                .andReturn().getResponse().getContentAsString());
        int version = mapper.readTree(mvc.perform(get("/api/public/rules"))
                .andReturn().getResponse().getContentAsString()).get("version").asInt();

        for (JsonNode example : examples) {
            var result = mvc.perform(post("/api/public/rule-changes/test-run").contentType(MediaType.APPLICATION_JSON)
                    .content(json(example)));
            switch (example.get("id").asText()) {
                case "clash" -> result.andExpect(jsonPath("$.passed").value(false))
                        .andExpect(jsonPath("$.conflicts.length()").value(1));
                case "gap" -> result.andExpect(jsonPath("$.unrouted").value(12));
                case "invalid" -> result.andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.problems[0]").isNotEmpty());
                default -> result.andExpect(jsonPath("$.passed").value(true))
                        .andExpect(jsonPath("$.unrouted").value(0));
            }
        }

        mvc.perform(get("/api/public/rules")).andExpect(jsonPath("$.version").value(version));
    }

    @Test
    void uploadedContractComesBackAsTextReadyToCheck() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "terms.txt", "text/plain",
                "5.1 Liability is limited to 1 SDR per kilogram.".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/public/contract-checks/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("Text"))
                .andExpect(jsonPath("$.text").value("5.1 Liability is limited to 1 SDR per kilogram."));
    }

    @Test
    void aFullLengthContractIsReadAndChecked() throws Exception {
        String clause = "Der Paketdienst haftet bis zu den gesetzlichen Haftungsgrenzen. ";
        String text = "(1) " + clause.repeat(60_000 / clause.length());
        MockMultipartFile file = new MockMultipartFile("file", "agb.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));

        String read = mapper.readTree(mvc.perform(multipart("/api/public/contract-checks/documents").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("text").asText();

        mvc.perform(post("/api/public/contract-checks").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carrierName", "DHL", "text", read, "senderType", "BUSINESS",
                                "termsBasis", "STANDARD_TERMS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms.liabilityCap.sdrPerKilogram").value(8.33))
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    void unreadableUploadExplainsWhy() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "old.doc", "application/msword",
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 1});

        mvc.perform(multipart("/api/public/contract-checks/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString(".docx or PDF")));
    }

    @Test
    void consoleStaysClosedAndUnknownPathsAreDenied() throws Exception {
        mvc.perform(get("/api/console/rules")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/public/anything")).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/products")).andExpect(status().isForbidden());
    }
}
