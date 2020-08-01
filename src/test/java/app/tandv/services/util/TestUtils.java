package app.tandv.services.util;

import org.junit.jupiter.api.Assertions;

import java.util.*;

/**
 * @author Vic on 9/7/2018
 **/
public final class TestUtils {
    private TestUtils() {
    }

    public static void assertList(List target, int expectedSize) {
        Assertions.assertNotNull(target);
        Assertions.assertFalse(target.isEmpty());
        Assertions.assertEquals(expectedSize, target.size());
    }
}
