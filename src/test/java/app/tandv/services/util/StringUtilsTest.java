package app.tandv.services.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vic on 8/28/2018
 **/
class StringUtilsTest {

    @Test
    void testCapitalize() {
        Assertions.assertEquals("Abc", StringUtils.capitalize("abc", false));
        Assertions.assertEquals("'Salem's", StringUtils.capitalize("'Salem's", false));
        Assertions.assertEquals("UofA", StringUtils.capitalize("UofA", false));
        Assertions.assertEquals("the", StringUtils.capitalize("the", false));
        Assertions.assertEquals("The", StringUtils.capitalize("the", true));
    }

    @Test
    void testTitleCase() {
        Assertions.assertEquals("It", StringUtils.titleCase("it"));
        Assertions.assertEquals("And Then There Were None", StringUtils.titleCase("and then tHere Were nonE"));
        Assertions.assertEquals("Of the Mice of Green", StringUtils.titleCase("Of The Mice Of Green"));
        Assertions.assertEquals("ANNE OF THE GREEN GABLES", StringUtils.titleCase("ANNE OF THE GREEN GABLES"));
    }

    @Test
    void testTitleOrdering() {
        Assertions.assertEquals("salems lot", StringUtils.titleForOrdering("'Salem's Lot"));
        Assertions.assertEquals("starry night", StringUtils.titleForOrdering("A Starry Night"));
        Assertions.assertEquals("unfortunate case", StringUtils.titleForOrdering("An unfortunate Case"));
        Assertions.assertEquals("gunslinger", StringUtils.titleForOrdering("The gunslinger"));
    }

    @Test
    void testAuthorOrdering() {
        String[] result;
        result = StringUtils.authorForOrdering("Sir Isaac Newton");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals("newton isaac", result[0]);
        Assertions.assertEquals("IN", result[1]);

        result = StringUtils.authorForOrdering("Vic Yanez III");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals("yanez vic", result[0]);
        Assertions.assertEquals("VY", result[1]);
    }
}
