package com.swe.canvas.datamodel.action;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

    @Test
    void testEnumValues() {
        assertNotNull(ActionType.CREATE);
        assertNotNull(ActionType.MODIFY);
        assertNotNull(ActionType.DELETE);
        assertNotNull(ActionType.RESURRECT);

        assertEquals(4, ActionType.values().length);
    }

    @Test
    void testValueOf() {
        assertEquals(ActionType.CREATE, ActionType.valueOf("CREATE"));
        assertEquals(ActionType.MODIFY, ActionType.valueOf("MODIFY"));
        assertEquals(ActionType.DELETE, ActionType.valueOf("DELETE"));
        assertEquals(ActionType.RESURRECT, ActionType.valueOf("RESURRECT"));
    }
}