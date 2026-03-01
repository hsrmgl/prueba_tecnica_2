package com.itxiop.tech.supplier.sandbox.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("flow")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CandidateLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Create Candidate ──────────────────────────────────────────────

    @Test
    void create_candidate_returns_201() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duns", is(100000001)))
                .andExpect(jsonPath("$.name", is("Acme")))
                .andExpect(jsonPath("$.country", is("ES")))
                .andExpect(jsonPath("$.annualTurnover", is(2000000)));
    }

    @Test
    void create_duplicate_active_candidate_returns_409() throws Exception {
        String candidate = "{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}";

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidate))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidate))
                .andExpect(status().isConflict());
    }

    // ── Get Candidate ─────────────────────────────────────────────────

    @Test
    void get_candidate_by_duns_returns_200() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/candidates/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duns", is(100000001)))
                .andExpect(jsonPath("$.name", is("Acme")))
                .andExpect(jsonPath("$.country", is("ES")))
                .andExpect(jsonPath("$.annualTurnover", is(2000000)));
    }

    @Test
    void get_candidate_not_found_returns_404() throws Exception {
        mockMvc.perform(get("/candidates/{duns}", 999999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_candidate_after_acceptance_returns_404() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/candidates/{duns}", 100000001))
                .andExpect(status().isNotFound());
    }

    // ── Refuse Candidate ──────────────────────────────────────────────

    @Test
    void refuse_candidate_returns_204() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/refuse", 100000001))
                .andExpect(status().isNoContent());
    }

    @Test
    void refuse_non_existing_candidate_returns_404() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/refuse", 999999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void refuse_already_accepted_supplier_returns_409() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/candidates/{duns}/refuse", 100000001))
                .andExpect(status().isConflict());
    }

    // ── Reapply after refusal ─────────────────────────────────────────

    @Test
    void declined_candidate_can_reapply() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/refuse", 100000001))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 3000000, \"country\": \"DE\", \"duns\": 100000001, \"name\": \"Acme Reloaded\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/candidates/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Acme Reloaded")))
                .andExpect(jsonPath("$.country", is("DE")))
                .andExpect(jsonPath("$.annualTurnover", is(3000000)));
    }
}
