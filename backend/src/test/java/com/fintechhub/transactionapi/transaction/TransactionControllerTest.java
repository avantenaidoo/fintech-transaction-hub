// REST controller test testing transaction creation and retrieval 
// Using MockMvc for the HTTP layer without starting separate server

package com.fintechhub.transactionapi.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Updated to verify response
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
                            .andExpect(jsonPath("$.timestamp").exists())
                            .andExpect(jsonPath("$.status").value(404))
                            .andExpect(jsonPath("$.error").value("Not Found"))
                            .andExpect(jsonPath("$.message")
                                            .value("Transaction not found with ID: 999999999"))
                            .andExpect(jsonPath("$.path")
                                            .value("/api/transactions/999999999"));
    }

    // Adding API validation tests for invalid input and idempotency conflicts through HTTP endpoints
    // Updated to check the validation errors using same response structure
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
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/transactions"));

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

    // API validation tests for transaction without specifying currency and default to ZAR

    @Test
    void shouldRetrieveExistingTransactionThroughApi() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "description": "Retrieval payment",
                                "amount": 175.50,
                                "currency": "ZAR",
                                "idempotencyKey": "api-retrieval-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        long id = JsonPath
                .parse(result.getResponse().getContentAsString())
                .read("$.id", Long.class);

        mockMvc.perform(get("/api/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Retrieval payment"))
                .andExpect(jsonPath("$.amount").value(175.50))
                .andExpect(jsonPath("$.currency").value("ZAR"));
    }

    @Test
    void shouldDefaultCurrencyToZarWhenCurrencyIsOmitted()
            throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Default currency payment",
                                  "amount": 100.00,
                                  "idempotencyKey": "api-default-currency-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("ZAR"));
    }

    // Add Missing Amount test
    @Test
    void shouldRejectMissingAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "description": "Missing amount payment",
                                        "currency": "ZAR",
                                        "idempotencyKey": "api-missing-amount"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/transactions"));
    }

    // Tests to check existing error responses

    @Test
    void shouldReturnConsistentErrorResponseForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Incomplete payment",
                          "amount": 250.00,
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value("/api/transactions"));
    }

    @Test
    void shouldReturnConsistentErrorResponseForInvalidCurrency() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Invalid currency payment",
                          "amount": 250.00,
                          "currency": "XYZ",
                          "idempotencyKey": "api-invalid-currency-001"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value("/api/transactions"));
    }

}