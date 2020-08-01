package app.tandv.services.util;

import app.tandv.services.data.entity.BookFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Vic on 8/31/2018
 **/
class EntityUtilsTest {
    @Test
    void testHashOfAll() {
        int output = 1743391056;
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
                BookFormat.PAPERBACK,
                new Exception("some object")
        );
        Assertions.assertEquals(output, input);
    }
}
