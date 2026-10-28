// REST controller test testing transaction creation and retrieval 
// Using MockMvc for the HTTP layer without starting separate server

package com.fintechhub.transactionapi.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateTransactionThroughApi() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "API payment",
                                  "amount": 250.00,
                                  "currency": "ZAR",
                                  "idempotencyKey": "api-payment-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reference").isString())
                .andExpect(jsonPath("$.description").value("API payment"))
                .andExpect(jsonPath("$.currency").value("ZAR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundForUnknownTransaction() throws Exception {
        mockMvc.perform(get("/api/transactions/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // Adding API validation tests for invalid input and idempotency conflicts through HTTP endpoints

    @Test
    void shouldRejectMissingDescription() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 250.00,
                                  "currency": "ZAR",
                                  "idempotencyKey": "api-missing-description"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Invalid payment",
                                  "amount": -10.00,
                                  "currency": "ZAR",
                                  "idempotencyKey": "api-invalid-amount"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyIsReused()
            throws Exception {

        String request = """
                {
                  "description": "Original payment",
                  "amount": 250.00,
                  "currency": "ZAR",
                  "idempotencyKey": "api-conflict-001"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Different payment",
                                  "amount": 500.00,
                                  "currency": "ZAR",
                                  "idempotencyKey": "api-conflict-001"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}