package app.tandv.services.util;

import app.tandv.services.util.collections.FluentArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;


/**
 * @author vic on 2018-08-28
 */
public final class StringUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtils.class);

    public static final Pattern ROMAN_NUMERAL = Pattern.compile("^m{0,3}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$");

    private static final Set<String> ARTICLES = new HashSet<>(Arrays.asList("a", "an", "of", "the", "is", "in", "to"));
    private static final Set<String> TITLE_ARTICLES = new HashSet<>(Arrays.asList("a", "an", "the"));
    private static final Set<String> HONORIFICS = new HashSet<>(Arrays.asList("sir", "sire", "mrs", "miss", "ms", "lord", "dr", "phd", "dphil", "md", "do", "doc", "sr", "jr"));
    private static final String WORD_SEPARATOR = " ";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final char EMPTY_SPACE_ASCII = 0x20;
    private static final IntPredicate IS_ALPHANUM = charCodePoint -> Character.isAlphabetic(charCodePoint)
            || Character.isDigit(charCodePoint)
            || charCodePoint == EMPTY_SPACE_ASCII;
    private static final Predicate<String> INVALID_AUTHOR_WORD = word -> HONORIFICS.contains(word)
            || ROMAN_NUMERAL.matcher(word).matches();

    private static MessageDigest DIGEST = null;

    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            LOGGER.error("Unable to get SHA 256 digest", exception);
        }
    }

    private StringUtils() {
    }

    public static Optional<String> titleCase(final String title) {
        return titleCase(title, false);
    }

    /**
     * @param title to convert to proper case
     * @param force to force all upper case to be capitalized
     * @return an optional that will contain a string with each individual word capitalized. Articles contained in the
     * original string will not be capitalized except if it is the first word in the string.
     */
    public static Optional<String> titleCase(final String title, final boolean force) {
        return Optional.ofNullable(title)
                .filter(StringUtils::validString)
                .map(words -> title.split(WORD_SEPARATOR))
                .map(FluentArrayList::new)
                .map(words -> words.thenReplaceAll(word -> capitalize(word, force, false)))
                .map(words -> words.thenReplace(word -> capitalize(word, false, true), 0))
                .map(words -> String.join(WORD_SEPARATOR, words));
    }

    /**
     * @param title of a book to normalize and order
     * @return an {@link Optional} containing the title of a book in normalized form starting from the first non article
     * word
     */
    public static Optional<String> titleForOrdering(final String title) {
        return Optional.ofNullable(title)
                .filter(StringUtils::validString)
                // normalize the input string
                .map(StringUtils::normalize)
                // split by any word separator
                .map(normalized -> normalized.split(WORD_SEPARATOR))
                // convert to fluent array list
                .map(FluentArrayList::new)
                // cut after first word not in title articles
                .map(words -> words.removeBefore(words.firstNotIn(TITLE_ARTICLES)))
                // join back together
                .map(words -> String.join(WORD_SEPARATOR, words));
    }

    /**
     * @param name of an author to normalize and order
     * @return an {@link Optional} containing the author name in normalized form removing any honorific or roman
     * numerals from the name and starting from the first last name
     */
    public static Optional<String> authorForOrdering(final String name) {
        return Optional.ofNullable(name)
                .filter(StringUtils::validString)
                // normalize the input string
                .map(StringUtils::normalize)
                // split by any word separator
                .map(normalized -> normalized.split(WORD_SEPARATOR))
                // convert to fluent array list
                .map(FluentArrayList::new)
                // remove all non invalid words for an author ordering
                .map(words -> words.thenRemoveIf(StringUtils.INVALID_AUTHOR_WORD))
                // swap first to last word
                .map(words -> words.swap(0, -1))
                // join back on a single string
                .map(words -> String.join(WORD_SEPARATOR, words));
    }

    /**
     * @param string to get its sha 256 representation
     * @return a sha 256 digest over the normalization of the string
     */
    public static Optional<String> sha256(final String string) {
        return Optional.ofNullable(string)
                .filter(StringUtils::validString)
                // normalize the input string
                .map(StringUtils::normalize)
                // convert to bytes
                .map(w -> w.getBytes(StandardCharsets.UTF_8))
                // if digest available, generate
                .flatMap(bytes -> Optional.ofNullable(DIGEST).map(digest -> digest.digest(bytes)))
                // convert to hex string
                .map(StringUtils::byteToString);
    }

    public static boolean validString(final String string) {
        return string != null && !string.isEmpty();
    }

    /**
     * This function won't do any null checking
     *
     * @param toClean self explanatory
     * @return a normalized string containing only [a-z0-9 ]
     */
    private static String normalize(final String toClean) {
        // convert to code points
        return toClean.codePoints()
                // extract only those valid ones
                .filter(StringUtils.IS_ALPHANUM)
                // convert to lower case
                .map(Character::toLowerCase)
                .mapToObj(codePoint -> (char) codePoint)
                // put back on a single string
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * This function won't do any null checking.
     * <p>
     * These are the capitalization rules:
     * - The first alphabetic character ([a-z]) will be set to upper case, the rest to lower case, except for the
     * following cases
     * - If the word already contains 2 or more upper case characters (as in the case of acronyms) no change will be
     * made, unless force flag is set
     * - If the word is a valid roman numeral as defined by {@link StringUtils#ROMAN_NUMERAL}, the word will be returned
     * in all upper case
     * - If the word is a valid article it will depend on the flag passed whether the first capitalization rule is
     * applied.
     *
     * @param word               to capitalize
     * @param force              whether more than one capital letter should be ignored
     * @param capitalizeArticles whether articles should be or not capitalized
     * @return a capitalized word as described before.
     */
    private static String capitalize(final String word, final boolean force, final boolean capitalizeArticles) {
        // We need to check if a given word has two or more uppercase letters
        for (int ind = 0, counter = 0; !force && ind < word.length(); ind++) {
            if (Character.isUpperCase(word.codePointAt(ind))) {
                counter++;
                if (counter == 2) {
                    // If the word already has 2 upper case letters, we wont do anything for it and we'll leave it unchanged
                    return word;
                }
            }
        }
        String lowerCaseWord = word.toLowerCase();
        if (ROMAN_NUMERAL.matcher(lowerCaseWord).matches()) {
            return word.toUpperCase();
        }
        if (!capitalizeArticles && ARTICLES.contains(lowerCaseWord)) {
            return lowerCaseWord;
        }
        char[] working = lowerCaseWord.toCharArray();
        for (int ind = 0; ind < working.length; ind++) {
            if (Character.isAlphabetic(working[ind])) {
                working[ind] = (char) Character.toUpperCase(word.codePointAt(ind));
                break;
            }
        }
        return new String(working);
    }

    private static String byteToString(final byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
