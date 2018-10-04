package app.tandv.services.util;

import app.tandv.services.data.entity.BookFormat;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vic on 8/31/2018
 **/
public class EntityUtilsTest {
    @Test
    public void testHashOfAll() {
        int output = -735122101;
        int input = EntityUtils.entityHash(
                null,
                Boolean.FALSE,
                Boolean.TRUE,
                (byte) 0x56,
                'V',
                (short) 69,
                77,
                (long) 10,
                52.8f,
                (double) (24 / 7),
                "A string",
                BookFormat.PB,
                new Exception("some object")
        );
        Assert.assertEquals(output, input);
    }
}
