// API error response record

package com.fintechhub.transactionapi.transaction;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}