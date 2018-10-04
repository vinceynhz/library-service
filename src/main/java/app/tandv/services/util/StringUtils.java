package app.tandv.services.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Vic on 8/28/2018
 **/
public final class StringUtils {
    private static final Set<String> ARTICLES = new HashSet<>(Arrays.asList("a", "an", "of", "the", "is", "in", "to"));
    private static final Set<String> TITLE_ARTICLES = new HashSet<>(Arrays.asList("a", "an", "the"));
    private static final Set<String> HONORIFICS = new HashSet<>(Arrays.asList("sir", "sire", "mrs", "miss", "ms", "lord", "dr", "phd", "dphil", "md", "do", "doc", "sr", "jr"));
    private static final Pattern ROMAN_NUMERAL = Pattern.compile("^(?=[mdclxvi])m*d?c{0,4}l?x{0,4}v?i{0,4}$");
    private static final String WORD_SEPARATOR = " ";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private StringUtils() {
    }

    public static boolean notValidString(final String toValidate) {
        return toValidate == null || toValidate.isEmpty();
    }

    public static boolean validString(final String toValidate) {
        return !notValidString(toValidate);
    }

    /**
     * @param toClean string that needs to be cleared
     * @return a string in containing only [a-z0-9 ]
     */
    private static String cleanString(final String toClean) {
        if (notValidString(toClean)) {
            return toClean;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int ind = 0; ind < toClean.length(); ind++) {
            int charCodePoint = toClean.codePointAt(ind);
            if (Character.isAlphabetic(charCodePoint) || Character.isDigit(charCodePoint) || charCodePoint == ' ') {
                stringBuilder.append((char) Character.toLowerCase(charCodePoint));
            }
        }
        return stringBuilder.toString();
    }

    public static String titleForOrdering(final String toOrder) {
        if (notValidString(toOrder)) {
            return toOrder;
        }
        StringBuilder orderedTitle = new StringBuilder();
        String[] words = cleanString(toOrder).split(WORD_SEPARATOR);
        boolean firstFound = false;
        for (String word : words) {
            if (firstFound) {
                orderedTitle.append(" ").append(word);
            } else if (!TITLE_ARTICLES.contains(word)) {
                firstFound = true;
                orderedTitle.append(word);
            }
        }
        return orderedTitle.toString();
    }

    /**
     * @param toOrder name of the author that needs to be cleaned
     * @return an array of exactly 2 entries, the first index will contain the name ready for ordering, the second one
     * will contain the initials of the name
     */
    public static String[] authorForOrdering(final String toOrder) {
        if (notValidString(toOrder)) {
            return EMPTY_STRING_ARRAY;
        }
        String[] words = cleanString(toOrder).split(WORD_SEPARATOR);
        List<String> cleanedName = new ArrayList<>(words.length);
        for (String word : words) {
            if (!HONORIFICS.contains(word) && !ROMAN_NUMERAL.matcher(word).matches()) {
                cleanedName.add(word);
            }
        }
        String initials;
        if (cleanedName.size() == 1) {
            // If we have a one word name, take the first two characters of it as initials
            initials = cleanedName.get(0).substring(0, 2);
        } else {
            // If not, take the first character of the first and second words
            initials = String.valueOf(cleanedName.get(0).charAt(0)) + cleanedName.get(1).charAt(0);
        }
        // Take the last one and put it at the beginning
        cleanedName.add(0, cleanedName.remove(cleanedName.size() - 1));
        return new String[]{String.join(" ", cleanedName), initials.toUpperCase()};
    }

    /**
     * @param word to capitalize
     * @return the word with the first alphabetic ([A-Za-z]) character set to uppercase, the rest of the word to
     * lowercase.
     */
    static String capitalize(final String word, final boolean capitalizeArticles) {
        // We need to check if a given word has two or more uppercase letters
        for (int ind = 0, counter = 0; ind < word.length(); ind++) {
            if (Character.isUpperCase(word.codePointAt(ind))) {
                counter++;
                if (counter == 2) {
                    // If the word already has 2 upper case letters, we wont do anything for it and we'll leave it unchanged
                    return word;
                }
            }
        }
        String lowerCaseWord = word.toLowerCase();
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

    public static String titleCase(final String title) {
        if (notValidString(title)) {
            return title;
        }
        String[] words = title.split(WORD_SEPARATOR);
        words[0] = capitalize(words[0], true);
        if (words.length > 1) {
            for (int ind = 1; ind < words.length; ind++) {
                words[ind] = capitalize(words[ind], false);
            }
        }
        return String.join(WORD_SEPARATOR, words);
    }
}
