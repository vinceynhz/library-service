package app.tandv.services.test;

import app.tandv.services.util.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Vic on 8/28/2018
 **/
class StringUtilsTest {

    private class TestCase {
        final String input;
        final String expected;
        final boolean shouldBeEmpty;

        private TestCase(String input, String expected, boolean shouldBeEmpty) {
            this.input = input;
            this.expected = expected;
            this.shouldBeEmpty = shouldBeEmpty;
        }

        void validate(Function<String, Optional<String>> mapper) {
            Optional<String> output = mapper.apply(this.input);
            if (this.shouldBeEmpty) {
                Assertions.assertFalse(output.isPresent());
            } else {
                Assertions.assertTrue(output.isPresent());
            }
            output.ifPresent(actual -> Assertions.assertEquals(this.expected, actual));
        }
    }

    @Test
    void testValidStrings() {
        Assertions.assertTrue(StringUtils.validString("valid"));
        Assertions.assertFalse(StringUtils.validString(""));
        //we are purposefully testing this case
        //noinspection ConstantConditions
        Assertions.assertFalse(StringUtils.validString(null));
    }

    @Test
    void testTitleCase() {
        TestCase[] testCases = {
                new TestCase("it", "It", false),
                new TestCase("and then tHere Were nonE", "And Then There Were None", false),
                new TestCase("Of The Mice Of Green", "Of the Mice of Green", false),
                new TestCase("ANNE OF THE GREEN GABLES", "ANNE OF THE GREEN GABLES", false),
                new TestCase("star wars iv", "Star Wars IV", false),
                new TestCase(null, null, true),
                new TestCase("", null, true)
        };

        for (TestCase testCase : testCases) {
            testCase.validate(StringUtils::titleCase);
        }

        new TestCase("THE OUTSIDER", "The Outsider", false)
                .validate(s -> StringUtils.titleCase(s, true));
    }

    @Test
    void testTitleOrdering() {
        TestCase[] testCases = {
                new TestCase("'Salem's Lot", "salems lot", false),
                new TestCase("A Starry Night", "starry night", false),
                new TestCase("An unfortunate Case", "unfortunate case", false),
                new TestCase("The gunslinger", "gunslinger", false),
                new TestCase(null, null, true),
                new TestCase("", null, true)
        };

        for (TestCase testCase : testCases) {
            testCase.validate(StringUtils::titleForOrdering);
        }
    }

    @Test
    void testAuthorOrdering() {
        TestCase[] testCases = {
                new TestCase("Sir Isaac Newton", "newton isaac", false),
                new TestCase("Saul Hudson III", "hudson saul", false),
                new TestCase(null, null, true),
                new TestCase("", null, true)
        };

        for (TestCase testCase : testCases) {
            testCase.validate(StringUtils::authorForOrdering);
        }
    }

    @Test
    void testSha256() {
        final String stephenKingSha = "3856586E28AD1FC06ECE3C7FF94598CFD30799375C7335EE4B8D9F3BC002A85D";
        final String kingStephenSha = "E4AC4CADA83F399F6442B9996BC71B97EBB46E0630610D16AE85153241978BE5";
        final String salemsLotSha = "1C660F4FCD1746AC8C689C3142A3C79BE56077824BF11F434B6F150F78CFD236";
        TestCase[] testCases = {
                // These 3 should return the same SHA, since it's the same person
                new TestCase("Stephen King", stephenKingSha, false),
                new TestCase("STEPHEN KING", stephenKingSha, false),
                new TestCase("stephen king", stephenKingSha, false),
                // This one should return a different SHA, since technically it's a different person
                new TestCase("stephenk ing", "B4B35F86D0E5F69636CFFD06000E6016CD7A97030797589CF846A61497CA65AE", false),
                // These two should return the same SHA between them, but it should be different than Stephen King's above
                new TestCase("KING STEPHEN", kingStephenSha, false),
                new TestCase("king stephen", kingStephenSha, false),
                // These two should also return the same SHA between them since the special characters do not count
                new TestCase("'Salem's Lot", salemsLotSha, false),
                new TestCase("Salems Lot", salemsLotSha, false),
                new TestCase(null, null, true),
                new TestCase("", null, true)
        };
        for (TestCase testCase : testCases) {
            testCase.validate(StringUtils::sha256);
        }
    }

    @Test
    void testRomanNumerals() {
        String[] romans = {
                "i",
                "ii",
                "iii",
                "iv",
                "v",
                "vi",
                "vii",
                "viii",
                "ix",
                "x",
                "xiv",
                "xix",
                "xx",
                "l",
                "lix",
                "lxxx",
                "cclxxx",
                "cdlxxxviii",
                "mix",
        };
        for (String roman : romans) {
            Assertions.assertTrue(StringUtils.ROMAN_NUMERAL.matcher(roman).matches(), roman);
        }
        String[] noRomans = {
                "iiii",
                "vv",
                "iiiiv",
                "ivi",
                "ixx",
                "ixm",
                "mic",
        };
        for (String noRoman : noRomans) {
            Assertions.assertFalse(StringUtils.ROMAN_NUMERAL.matcher(noRoman).matches(), noRoman);
        }
    }
}
