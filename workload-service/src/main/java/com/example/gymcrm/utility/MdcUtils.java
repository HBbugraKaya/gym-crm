package com.example.gymcrm.utility;

import java.util.UUID;
import org.slf4j.MDC;

public final class MdcUtils {

    public static final String TRANSACTION_ID = "transactionId";

    private MdcUtils() {}

    public static void setTransactionId(String transactionId) {
        if (transactionId != null && !transactionId.isBlank()) {
            MDC.put(TRANSACTION_ID, transactionId);
        }
    }

    public static String getOrGenerateTransactionId() {
        String txId = MDC.get(TRANSACTION_ID);
        return (txId != null && !txId.isBlank()) ? txId : UUID.randomUUID().toString();
    }

    public static void clear() {
        MDC.remove(TRANSACTION_ID);
    }
}
