package app.tandv.services.test;

import app.tandv.services.util.collections.FluentArrayList;
import app.tandv.services.util.collections.FluentHashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * @author vic on 2020-08-03
 */
class FluentArrayListTest {
    @Test
    void testEmptyProvider() {
        FluentArrayList<String> empty = FluentArrayList.empty();
        Assertions.assertNotNull(empty);
        Assertions.assertTrue(empty.isEmpty());
    }

    @Test
    void testConstructorByVarargs() {
        FluentArrayList<Character> test = new FluentArrayList<>('t', 'e', 's', 't');
        assertSize(test, 4);
    }

    @Test
    void testConstructorByCollection() {
        FluentHashSet<Integer> numbers = new FluentHashSet<Integer>()
                .thenAdd(1)
                .thenAdd(2)
                .thenAdd(3)
                .thenAdd(2)
                .thenAdd(1)
                .thenAdd(4);
        FluentArrayList<Integer> test = new FluentArrayList<>(numbers);
        assertSize(test, 4);
    }

    @Test
    void testRemoveBefore() {
        FluentArrayList<String> test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeBefore(4);
        assertSize(test, 4);
        Assertions.assertEquals("rock", test.getFirst());
        Assertions.assertEquals("you", test.getLast());

        test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeBefore(0);
        assertSize(test, 8);
        Assertions.assertEquals("for", test.getFirst());
        Assertions.assertEquals("you", test.getLast());

        test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeBefore(8);
        Assertions.assertNotNull(test);
        Assertions.assertTrue(test.isEmpty());
    }

    @Test
    void testRemoveAfter() {
        FluentArrayList<String> test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeAfter(4);
        assertSize(test, 4);
        Assertions.assertEquals("for", test.getFirst());
        Assertions.assertEquals("to", test.getLast());

        test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeAfter(0);
        Assertions.assertNotNull(test);
        Assertions.assertTrue(test.isEmpty());

        test = new FluentArrayList<>("for", "those", "about", "to", "rock")
                .thenAdd("we")
                .thenAdd("salute")
                .thenAdd("you")
                .removeAfter(8);
        assertSize(test, 8);
        Assertions.assertEquals("for", test.getFirst());
        Assertions.assertEquals("you", test.getLast());
    }

    @Test
    void testSwap() {
        FluentArrayList<String> test = new FluentArrayList<String>()
                .thenAdd("lions")
                .thenAdd("bears")
                .thenAdd("oh my!")
                .thenAdd(1, "tigers") // tigers in second place as the song goes
                .swap(0, -1) // lions to the end
                .swap(1, -2) // bears to one before the last
                .swap(-3, 0); // oh my to the beginning

        assertSize(test, 4);
        Assertions.assertEquals("oh my!", test.getFirst());
        Assertions.assertEquals("tigers", test.get(1));
        Assertions.assertEquals("bears", test.get(2));
        Assertions.assertEquals("lions", test.getLast());
    }

    @Test
    void testReplace() {
        FluentArrayList<String> test = new FluentArrayList<String>()
                .thenAdd("en")
                .thenAdd("algun")
                .thenAdd("lugar")
                .thenAdd("de")
                .thenAdd("la")
                .thenAdd("mancha")
                .thenReplaceAll(String::toUpperCase)
                .thenReplace(this::reverse, 0, 2, 4)
                // cut short characters
                .thenRemoveIf(s -> s.length() < 3);

        assertSize(test, 3);
        Assertions.assertEquals("ALGUN", test.getFirst());
        Assertions.assertEquals("RAGUL", test.get(1));
        Assertions.assertEquals("MANCHA", test.getLast());
    }

    @Test
    void testFirstNotIn() {
        FluentHashSet<String> firstWords = new FluentHashSet<String>()
                .thenAdd("en")
                .thenAdd("algun")
                .thenAdd("lugar");
        FluentHashSet<String> noWords = new FluentHashSet<String>()
                .thenAdd("these")
                .thenAdd("are")
                .thenAdd("not")
                .thenAdd("there");
        FluentArrayList<String> test = new FluentArrayList<>(firstWords)
                .thenAdd("de")
                .thenAdd("la")
                .thenAdd("mancha");

        assertSize(test, 6);
        Assertions.assertEquals(3, test.firstNotIn(firstWords));
        Assertions.assertEquals(0, test.firstNotIn(noWords));
        Assertions.assertEquals(0, test.firstNotIn(Collections.emptyList()));
    }

    private static <T> void assertSize(final FluentArrayList<T> test, int expectedSize) {
        Assertions.assertNotNull(test);
        Assertions.assertFalse(test.isEmpty());
        Assertions.assertEquals(expectedSize, test.size());
    }

    private String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }
}
