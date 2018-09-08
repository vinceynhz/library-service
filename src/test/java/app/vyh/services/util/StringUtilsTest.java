package app.vyh.services.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vic on 8/28/2018
 **/
public class StringUtilsTest {

    @Test
    public void testCapitalize() {
        Assert.assertEquals("Abc", StringUtils.capitalize("abc"));
        Assert.assertEquals("'Salem's", StringUtils.capitalize("'Salem's"));
    }

    @Test
    public void testTitleCase() {
        Assert.assertEquals("It", StringUtils.titleCase("IT"));
        Assert.assertEquals("And Then There Were None", StringUtils.titleCase("AnD then THere WeRe NONE"));
        Assert.assertEquals("Of the Mice of Green", StringUtils.titleCase("Of The Mice Of Green"));
    }

    @Test
    public void testTitleOrdering() {
        Assert.assertEquals("salems lot", StringUtils.titleForOrdering("'Salem's Lot"));
        Assert.assertEquals("starry night", StringUtils.titleForOrdering("A Starry Night"));
        Assert.assertEquals("unfortunate case", StringUtils.titleForOrdering("An unfortunate Case"));
        Assert.assertEquals("gunslinger", StringUtils.titleForOrdering("The gunslinger"));
    }

    @Test
    public void testAuthorOrdering() {
        String[] result;
        result = StringUtils.authorForOrdering("Sir Isaac Newton");
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("newton isaac", result[0]);
        Assert.assertEquals("IN", result[1]);

        result = StringUtils.authorForOrdering("Vic Yanez III");
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("yanez vic", result[0]);
        Assert.assertEquals("VY", result[1]);
    }
}
