package app.vyh.services.util;

import org.junit.Assert;

import java.util.List;

/**
 * @author Vic on 9/7/2018
 **/
public final class TestUtils {
    private TestUtils() {
    }

    public static void assertList(List target, int expectedSize){
        Assert.assertNotNull(target);
        Assert.assertFalse(target.isEmpty());
        Assert.assertEquals(expectedSize, target.size());
    }
}
