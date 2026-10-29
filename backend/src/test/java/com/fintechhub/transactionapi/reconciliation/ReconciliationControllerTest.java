// Controller test to verify POST /api/reconciliation
// Test HTTP endpoint without launching Python or live database

package com.fintechhub.transactionapi.reconciliation;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ReconciliationController.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void reconcileReturnsResultsFromService() throws Exception {
        var expected = objectMapper.readTree("""
                {
                  "matched": [],
                  "mismatched": [],
                  "missing_external": [],
                  "missing_internal": []
                }
                """);

        when(reconciliationService.reconcile()).thenReturn(expected);

        mockMvc.perform(post("/api/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "matched": [],
                          "mismatched": [],
                          "missing_external": [],
                          "missing_internal": []
                        }
                        """));
    }
}