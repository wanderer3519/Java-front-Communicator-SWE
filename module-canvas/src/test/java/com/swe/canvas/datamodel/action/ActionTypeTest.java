package com.swe.canvas.datamodel.action;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

    @Test
    void testEnumValues() {
        // Verify all expected constants exist
        assertNotNull(ActionType.CREATE);
        assertNotNull(ActionType.MODIFY);
        assertNotNull(ActionType.DELETE);
        assertNotNull(ActionType.RESURRECT);
        assertNotNull(ActionType.UNKNOWN);

        // Verify total count (Now 5)
        assertEquals(5, ActionType.values().length);
    }

    @Test
    void testValueOf() {
        assertEquals(ActionType.CREATE, ActionType.valueOf("CREATE"));
        assertEquals(ActionType.MODIFY, ActionType.valueOf("MODIFY"));
        assertEquals(ActionType.DELETE, ActionType.valueOf("DELETE"));
        assertEquals(ActionType.RESURRECT, ActionType.valueOf("RESURRECT"));
        assertEquals(ActionType.UNKNOWN, ActionType.valueOf("UNKNOWN"));
    }
}