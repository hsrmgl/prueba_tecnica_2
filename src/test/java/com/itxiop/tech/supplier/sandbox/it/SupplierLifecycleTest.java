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
class SupplierLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Accept Candidate ──────────────────────────────────────────────

    @Test
    void accept_candidate_with_good_rating_returns_active_supplier() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duns", is(100000001)))
                .andExpect(jsonPath("$.name", is("Acme")))
                .andExpect(jsonPath("$.status", is("Active")))
                .andExpect(jsonPath("$.sustainabilityRating", is("A")));
    }

    @Test
    void accept_candidate_with_bad_rating_shows_active_in_api() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"D\"}"))
                .andExpect(status().isNoContent());

        // On Probation internally, but API shows Active
        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Active")))
                .andExpect(jsonPath("$.sustainabilityRating", is("D")));
    }

    @Test
    void accept_candidate_not_found_returns_404() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/accept", 999999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_candidate_from_banned_country_returns_400() throws Exception {
        // "NO" → first char 'n' → banned country per CountryController logic
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"NO\", \"duns\": 100000001, \"name\": \"Nordic Co\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accept_candidate_with_low_turnover_returns_400() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 500000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Small Co\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accept_already_accepted_candidate_returns_409() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"B\"}"))
                .andExpect(status().isConflict());
    }

    // ── Get Supplier ──────────────────────────────────────────────────

    @Test
    void get_supplier_not_found_returns_404() throws Exception {
        mockMvc.perform(get("/suppliers/{duns}", 999999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_supplier_for_candidate_returns_404() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isNotFound());
    }

    // ── Ban Supplier ──────────────────────────────────────────────────

    @Test
    void ban_on_probation_supplier_returns_204() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        // Accept with C rating → On Probation
        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"C\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/suppliers/{duns}/ban", 100000001))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Disqualified")));
    }

    @Test
    void ban_active_supplier_returns_409() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        // Accept with A rating → Active (not On Probation)
        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/suppliers/{duns}/ban", 100000001))
                .andExpect(status().isConflict());
    }

    @Test
    void ban_non_existing_supplier_returns_404() throws Exception {
        mockMvc.perform(post("/suppliers/{duns}/ban", 999999999))
                .andExpect(status().isNotFound());
    }

    // ── Disqualified cannot reapply ───────────────────────────────────

    @Test
    void disqualified_supplier_cannot_reapply() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"C\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/suppliers/{duns}/ban", 100000001))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 3000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme Reloaded\"}"))
                .andExpect(status().isConflict());
    }

    // ── Cannot create candidate with same DUNS as existing supplier ───

    @Test
    void cannot_create_candidate_when_supplier_exists() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 2000000, \"country\": \"ES\", \"duns\": 100000001, \"name\": \"Acme\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", 100000001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sustainabilityRating\": \"A\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annualTurnover\": 3000000, \"country\": \"DE\", \"duns\": 100000001, \"name\": \"Another Acme\"}"))
                .andExpect(status().isConflict());
    }
}
