package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class QueryAliasRegistryTest {

    @Test
    public void expandsAliasesBidirectionally() {
        assertEquals(
                Collections.singletonList("factura"),
                QueryAliasRegistry.expand("invoice")
        );
        assertEquals(
                Collections.singletonList("invoice"),
                QueryAliasRegistry.expand("FACTURA")
        );
    }

    @Test
    public void replacesOnlyCompleteNormalizedTokens() {
        assertEquals(
                Collections.singletonList("factura apa"),
                QueryAliasRegistry.expand("  Invoice   APA ")
        );
        assertEquals(
                Collections.singletonList("invoice apa"),
                QueryAliasRegistry.expand("factura apa")
        );
        assertEquals(
                Collections.emptyList(),
                QueryAliasRegistry.expand("invoices")
        );
        assertEquals(
                Collections.emptyList(),
                QueryAliasRegistry.expand("prefactura")
        );
    }
}
