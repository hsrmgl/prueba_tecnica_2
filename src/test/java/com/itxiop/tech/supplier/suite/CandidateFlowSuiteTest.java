package com.itxiop.tech.supplier.suite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("flow")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CandidateFlowSuiteTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createCandidates_acceptCandidates_potentialSuppliers() throws Exception {
        // Create Candidates
        List.of(
                "{\"annualTurnover\": 2200000,\"country\": \"ES\",\"duns\": 100000001,\"name\": \"One\"}",
                "{\"annualTurnover\": 3000000,\"country\": \"ES\",\"duns\": 100000002,\"name\": \"Two\"}",
                "{\"annualTurnover\": 3850000,\"country\": \"ES\",\"duns\": 100000003,\"name\": \"Three\"}",
                "{\"annualTurnover\": 4500000,\"country\": \"ES\",\"duns\": 100000004,\"name\": \"Four\"}",
                "{\"annualTurnover\": 3800000,\"country\": \"ES\",\"duns\": 100000005,\"name\": \"Five\"}",
                "{\"annualTurnover\": 2000000,\"country\": \"DE\",\"duns\": 100000006,\"name\": \"Six\"}",
                "{\"annualTurnover\": 2100000,\"country\": \"DE\",\"duns\": 100000007,\"name\": \"Seven\"}",
                "{\"annualTurnover\": 2000000,\"country\": \"DE\",\"duns\": 100000008,\"name\": \"Eight\"}",
                "{\"annualTurnover\": 16500000,\"country\": \"DE\",\"duns\": 100000009,\"name\": \"Nine\"}",
                "{\"annualTurnover\": 1500000,\"country\": \"FR\",\"duns\": 100000010,\"name\": \"Ten\"}"
        ).forEach(candidate -> {
            try {
                mockMvc.perform(post("/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(candidate))
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Accept Candidates
        List.of(
                List.of("100000001", "{\"sustainabilityRating\": \"B\"}"),
                List.of("100000002", "{\"sustainabilityRating\": \"C\"}"),
                List.of("100000003", "{\"sustainabilityRating\": \"D\"}"),
                List.of("100000004", "{\"sustainabilityRating\": \"D\"}"),
                List.of("100000005", "{\"sustainabilityRating\": \"D\"}"),
                List.of("100000006", "{\"sustainabilityRating\": \"A\"}"),
                List.of("100000007", "{\"sustainabilityRating\": \"B\"}"),
                List.of("100000008", "{\"sustainabilityRating\": \"C\"}"),
                List.of("100000009", "{\"sustainabilityRating\": \"E\"}"),
                List.of("100000010", "{\"sustainabilityRating\": \"B\"}")
        ).forEach(item -> {
            try {
                mockMvc.perform(post("/candidates/{duns}/accept", item.get(0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(item.get(1)))
                        .andExpect(status().isNoContent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Potential Suppliers (given rate allow all existing)
        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "1200000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.data[?(@.duns == 100000001)][?(@.score == 206250.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000002)][?(@.score == 187500.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000003)][?(@.score == 96250.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000004)][?(@.score == 112500.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000005)][?(@.score == 95000.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000006)][?(@.score == 250000.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000007)][?(@.score == 196875.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000008)][?(@.score == 125000.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000009)][?(@.score == 165000.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000010)][?(@.score == 140625.0)]").exists())
                .andExpect(jsonPath("$.pagination.limit", is(10)))
                .andExpect(jsonPath("$.pagination.offset", is(0)))
                .andExpect(jsonPath("$.pagination.total", is(10)));

        // Potential Suppliers (only 4 suppliers for given rate)
        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "3500000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[?(@.duns == 100000003)][?(@.score == 96250.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000004)][?(@.score == 112500.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000005)][?(@.score == 95000.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000009)][?(@.score == 165000.0)]").exists())
                .andExpect(jsonPath("$.pagination.limit", is(10)))
                .andExpect(jsonPath("$.pagination.offset", is(0)))
                .andExpect(jsonPath("$.pagination.total", is(4)));
    }

}
