package com.swe.canvas.datamodel.serialization;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.CreateShapeAction;
import com.swe.canvas.datamodel.action.ModifyShapeAction;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.RectangleShape;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

/**
 * JUnit 5 tests for the manual serialization and deserialization of Action objects.
 */
public class NetActionSerializerTest {

    private ShapeId shapeId;
    private String userId;
    private long timestamp;
    private ShapeState testState1; // A simple red square
    private ShapeState testState2; // A modified blue square

    @BeforeEach
    void setUp() {
        shapeId = new ShapeId("shape-123");
        userId = "test-user";
        timestamp = 1700000000000L;

        Shape shape1 = new RectangleShape(
            shapeId,
            List.of(new Point(10, 10), new Point(20, 20)),
            2.0,
            Color.RED,
            userId,
            userId
        );
        // We set a specific timestamp to test preservation
        testState1 = new ShapeState(shape1, false, 123L);

        Shape shape2 = new RectangleShape(
            shapeId,
            List.of(new Point(50, 50), new Point(60, 60)),
            4.0,
            Color.BLUE,
            userId,
            userId
        );
        testState2 = new ShapeState(shape2, false, 456L);
    }

    @Test
    void testActionSerialization_ModifyAction_RoundTrip() {
        Action originalAction = new ModifyShapeAction(
            "action-abc",
            userId,
            timestamp,
            shapeId,
            testState1, // prevState
            testState2  // newState
        );

        String json = NetActionSerializer.serializeAction(originalAction);
        Action restoredAction = NetActionSerializer.deserializeAction(json);

        assertNotNull(restoredAction);
        assertTrue(restoredAction instanceof ModifyShapeAction);

        assertEquals(originalAction.getActionId(), restoredAction.getActionId());
        assertEquals(originalAction.getActionType(), restoredAction.getActionType());
        assertEquals(originalAction.getUserId(), restoredAction.getUserId());
        assertEquals(originalAction.getShapeId(), restoredAction.getShapeId());

        assertShapeStatesEqual(originalAction.getPrevState(), restoredAction.getPrevState());
        assertShapeStatesEqual(originalAction.getNewState(), restoredAction.getNewState());
    }

    @Test
    void testActionSerialization_CreateAction_RoundTrip() {
        Action originalAction = new CreateShapeAction(
            "action-xyz",
            userId,
            timestamp,
            shapeId,
            testState1 // newState
        );

        String json = NetActionSerializer.serializeAction(originalAction);
        Action restoredAction = NetActionSerializer.deserializeAction(json);

        assertNotNull(restoredAction);
        assertTrue(restoredAction instanceof CreateShapeAction);
        assertEquals(originalAction.getActionId(), restoredAction.getActionId());
        assertNull(originalAction.getPrevState());
        assertNull(restoredAction.getPrevState());
        assertShapeStatesEqual(originalAction.getNewState(), restoredAction.getNewState());
    }

    private void assertShapeStatesEqual(ShapeState expected, ShapeState actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.isDeleted(), actual.isDeleted());
        
        // --- FIX: Now expecting exact timestamp match ---
        assertEquals(expected.getLastModified(), actual.getLastModified(), "Timestamp mismatch");

        assertShapesEqual(expected.getShape(), actual.getShape());
    }

    private void assertShapesEqual(Shape expected, Shape actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getShapeId(), actual.getShapeId());
        assertEquals(expected.getShapeType(), actual.getShapeType());
        assertEquals(expected.getCreatedBy(), actual.getCreatedBy());
        assertEquals(expected.getLastUpdatedBy(), actual.getLastUpdatedBy());
        assertEquals(expected.getColor(), actual.getColor());
        assertEquals(expected.getThickness(), actual.getThickness(), 0.001);
        assertEquals(expected.getPoints(), actual.getPoints());
    }
}