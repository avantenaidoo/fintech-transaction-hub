// Expose POST /api/reconciliation tooReconciliationService which runs Python CLI returns JSON results

package com.fintechhub.transactionapi.reconciliation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    public JsonNode reconcile() {
        return reconciliationService.reconcile();
    }
}