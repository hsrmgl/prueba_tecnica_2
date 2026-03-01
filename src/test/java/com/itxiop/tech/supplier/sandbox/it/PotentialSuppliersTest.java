package com.itxiop.tech.supplier.sandbox.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("flow")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PotentialSuppliersTest {

    @Autowired
    private MockMvc mockMvc;

    private void createAndAccept(int duns, String name, String country, long turnover, String rating) throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"annualTurnover\": %d, \"country\": \"%s\", \"duns\": %d, \"name\": \"%s\"}",
                                turnover, country, duns, name)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/candidates/{duns}/accept", duns)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"sustainabilityRating\": \"%s\"}", rating)))
                .andExpect(status().isNoContent());
    }

    // ── Potential Suppliers filtering ─────────────────────────────────

    @Test
    void potential_suppliers_excludes_disqualified() throws Exception {
        createAndAccept(100000001, "Acme", "ES", 2000000, "C");
        createAndAccept(100000002, "Beta", "ES", 3000000, "A");

        // Ban Acme (On Probation → Disqualified)
        mockMvc.perform(post("/suppliers/{duns}/ban", 100000001))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "1200000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].duns", is(100000002)))
                .andExpect(jsonPath("$.pagination.total", is(1)));
    }

    @Test
    void potential_suppliers_filters_by_rate() throws Exception {
        createAndAccept(100000001, "Small", "ES", 2000000, "A");
        createAndAccept(100000002, "Big", "ES", 5000000, "A");

        // rate=3000000: only Big (5M > 3M), Small (2M) is excluded
        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "3000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].duns", is(100000002)))
                .andExpect(jsonPath("$.pagination.total", is(1)));
    }

    @Test
    void potential_suppliers_returns_empty_when_none_qualify() throws Exception {
        createAndAccept(100000001, "Acme", "ES", 2000000, "A");

        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "5000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.total", is(0)));
    }

    @Test
    void potential_suppliers_sorted_descending_by_score() throws Exception {
        createAndAccept(100000001, "Low", "ES", 2000000, "A");
        createAndAccept(100000002, "High", "ES", 5000000, "A");

        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "1200000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].duns", is(100000002)))
                .andExpect(jsonPath("$.data[1].duns", is(100000001)));
    }

    @Test
    void potential_suppliers_pagination_structure() throws Exception {
        createAndAccept(100000001, "Acme", "ES", 2000000, "A");

        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "1200000")
                        .param("limit", "5")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit", is(5)))
                .andExpect(jsonPath("$.pagination.offset", is(0)))
                .andExpect(jsonPath("$.pagination.total", is(1)));
    }

    // ── Bonus calculation ─────────────────────────────────────────────

    @Test
    void potential_suppliers_applies_25_percent_bonus_to_two_lowest_turnovers() throws Exception {
        // Three suppliers in ES: turnovers 2.2M, 3M, 5M → two lowest unique: 2.2M, 3M
        createAndAccept(100000001, "S1", "ES", 2200000, "B");  // gets bonus: 2200000*0.1*0.75*1.25 = 206250
        createAndAccept(100000002, "S2", "ES", 3000000, "B");  // gets bonus: 3000000*0.1*0.75*1.25 = 281250
        createAndAccept(100000003, "S3", "ES", 5000000, "B");  // no bonus:   5000000*0.1*0.75      = 375000

        mockMvc.perform(get("/suppliers/potential")
                        .param("rate", "1200000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.duns == 100000001)][?(@.score == 206250.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000002)][?(@.score == 281250.0)]").exists())
                .andExpect(jsonPath("$.data[?(@.duns == 100000003)][?(@.score == 375000.0)]").exists());
    }

    // ── Sustainability Rating Update ──────────────────────────────────

    @Test
    void sustainability_rating_update_changes_supplier_rating() throws Exception {
        createAndAccept(100000001, "Acme", "ES", 2000000, "A");

        // Verify initial rating
        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustainabilityRating", is("A")))
                .andExpect(jsonPath("$.status", is("Active")));

        // Trigger rating change via sustainability endpoint
        mockMvc.perform(post("/sustainability/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duns\": 100000001, \"score\": \"C\"}"))
                .andExpect(status().isOk());

        // Verify rating changed; On Probation still shows as "Active" in API
        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustainabilityRating", is("C")))
                .andExpect(jsonPath("$.status", is("Active")));
    }

    @Test
    void sustainability_rating_update_from_bad_to_good_keeps_active() throws Exception {
        createAndAccept(100000001, "Acme", "ES", 2000000, "D");

        // Update to good rating
        mockMvc.perform(post("/sustainability/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duns\": 100000001, \"score\": \"A\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/suppliers/{duns}", 100000001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustainabilityRating", is("A")))
                .andExpect(jsonPath("$.status", is("Active")));
    }
}
