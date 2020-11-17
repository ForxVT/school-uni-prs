package com.g10.prs.njson;

import com.g10.prs.common.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class is used to read the NJson data format.
 * The NJson data format is completely inspired by JSON, but it is NOT JSON,
 * it does not follow any JSON RFC and has a few particularities.
 * The parser uses a recursive descent parser algorithm.
 *
 * NJson's basic data types are:
 * Integer: Can contain an unary minus and an integer number.
 * Double: Must contain a period, can contain an unary minus and a double number.
 * String: Can contain any string sequence between double quotes (with support for escape sequences).
 * Character: Can contain a character between single quotes (also with support for escape sequences).
 * Boolean: Can contain a boolean, with the keywords 'true' and 'false'.
 * Null: Can contain a null value, with keyword 'null'.
 * Map: Can contain a map (comparable to a JSON Object), within curly brackets.
 * Array: Can contain an ordered list of zero or more values, within square brackets.
 *
 * It also supports comments and multiline comments (same syntax as Java).
 *
 * Example:
 * {
 *     "name": "NJson",
 *     "version": 1,
 *     "types": [
 *         "Integer",
 *         "Double"
 *     },
 *     "example-null": null
 * }
 */
public class NJsonReader {
    /** The file reader, used to scan a file. */
    private final Reader reader;

    /** The current column when scanning, could be useful for debugging purposes. */
    private int currentColumn;

    /** The current line when scanning, could be useful for debugging purposes. */
    private int currentLine;

    /** The current character when scanning, to keep it easily accessible for any function in the class. */
    private char currentCharacter;

    /** Utility array to use in some functions containing space characters. */
    private static final char[] spaces = {' ', '\t'};

    /** Utility array to use in some functions containing digit characters. */
    private static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * Class constructor.
     *
     * @param filePath The file path to use.
     */
    public NJsonReader(String filePath) throws IOException {
        this(new FileInputStream(new File(filePath)));
    }

    /**
     * Class constructor.
     *
     * @param file The input stream to use.
     */
    public NJsonReader(InputStream file) throws IOException {
        reader = new BufferedReader(new InputStreamReader(file, StandardCharsets.UTF_8));
        currentColumn = 1;
        currentLine = 1;
        next();
    }

    /**
     * Read the file given in the constructor.
     *
     * @return a map of the file.
     */
    public Map<String, Object> readMap() throws NJSonCannotParseException, IOException {
        return parseMap();
    }

    /**
     * Read the file given in the constructor.
     *
     * @return an array of the file.
     */
    public List<Object> readArray() throws NJSonCannotParseException, IOException {
        return parseArray();
    }

    /**
     * Parses a map element, meaning a identifier followed by a value.
     *
     * @return a pair containing the identifier and the value.
     */
    private Pair<String, Object> parseMapElement() throws NJSonCannotParseException, IOException {
        String identifier = parseString();
        check(currentCharacter, ':');
        Object value = parseValue();

        return new Pair<String, Object>(identifier, value);
    }

    /**
     * Parses a value, which is any data type supported by NJson.
     *
     * @return the value as a Java Object.
     */
    private Object parseValue() throws IOException, NJSonCannotParseException {
        next();

        if (is(currentCharacter, '"')) {
            return parseString();
        } else if (is(currentCharacter, digits, spaces, '-', '.')) {
            return parseNumber();
        } else if (is(currentCharacter, '{')) {
            return parseMap();
        } else if (is(currentCharacter, 't', 'f')) {
            return parseBoolean();
        } else if (is(currentCharacter, '\'')) {
            return parseCharacter();
        } else if (is(currentCharacter, '[')) {
            return parseArray();
        } else if (is(currentCharacter, 'n')) {
            return parseNull();
        }

        return null;
    }

    /**
     * Parses a string.
     *
     * @return the string.
     */
    private String parseString() throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        while (!is(next(true), '"')) {
            contentBuilder.append(getEscaped());
        }
        next();

        return contentBuilder.toString();
    }

    /**
     * Parses a character.
     *
     * @return the character.
     */
    private char parseCharacter() throws IOException {
        next();
        String character = getEscaped();
        next();
        next();

        return character.charAt(0);
    }

    /**
     * Parses a number, either an integer or a double.
     *
     * @return the number.
     */
    private Object parseNumber() throws IOException {
        StringBuilder contentBuilder = new StringBuilder(new String(new char[] {currentCharacter}));
        while (is(next(), digits, '-', '.')) {
            contentBuilder.append(currentCharacter);
        }

        String number = contentBuilder.toString();
        if (number.contains(".")) {
            return Double.parseDouble(number);
        }

        return Integer.parseInt(number);
    }

    /**
     * Parses a boolean, either the keyword 'true' or the keyword 'false'.
     *
     * @return the boolean.
     */
    private boolean parseBoolean() throws IOException, NJSonCannotParseException {
        StringBuilder contentBuilder = new StringBuilder(new String(new char[] {currentCharacter}));
        while (Character.isAlphabetic(next(true))) {
            if (!"false".startsWith(contentBuilder.toString()) && !"true".startsWith(contentBuilder.toString())) {
                throw new NJSonCannotParseException("Not boolean");
            }

            contentBuilder.append(currentCharacter);
        }

        return contentBuilder.toString().equals("true");
    }

    /**
     * Parses a null value, meaning the keyword 'null'.
     *
     * @return null.
     */
    private Object parseNull() throws IOException, NJSonCannotParseException {
        StringBuilder contentBuilder = new StringBuilder(new String(new char[] {currentCharacter}));
        while (Character.isAlphabetic(next(true))) {
            if (!"null".startsWith(contentBuilder.toString())) {
                throw new NJSonCannotParseException("Not null");
            }

            contentBuilder.append(currentCharacter);
        }

        return null;
    }

    /**
     * Parses a map, either empty or with map elements.
     *
     * @return the map.
     */
    private Map<String, Object> parseMap() throws NJSonCannotParseException, IOException {
        Map<String, Object> elements = new LinkedHashMap<>();

        do {
            check(next(), '}', '"');
            if (currentCharacter == '}') {
                break;
            }
            Pair<String, Object> pair = parseMapElement();
            elements.put(pair.getObject1(), pair.getObject2());
            check(currentCharacter, '}', ',');
        } while (!is(currentCharacter, '}'));
        next();

        return elements;
    }

    /**
     * Parses an array, either empty or with values.
     *
     * @return the array as Java ArrayList.
     */
    private List<Object> parseArray() throws NJSonCannotParseException, IOException {
        List<Object> elements = new ArrayList<>();

        do {
            Object element = parseValue();
            if (element == null) {
                break;
            }
            elements.add(element);
            check(currentCharacter, ']', ',');
        } while (!is(currentCharacter, ']'));
        next();

        return elements;
    }

    /**
     * Reads the next character (outside of a string literal or a comment).
     *
     * @return the next character.
     */
    private char next() throws IOException {
        return next(false);
    }

    /**
     * Reads the next character.
     *
     * @param isInStringOrComment Defines if we are inside a string literal or a comment.
     * @return the next character.
     */
    private char next(boolean isInStringOrComment) throws IOException {
        currentCharacter = (char)reader.read();
        currentColumn++;

        if (!isInStringOrComment && is(currentCharacter, '/')) {
            char currChar = next(true);
            if (is(currChar, '/')) {
                // Comments
                int currentLine = this.currentLine;

                while (currentLine == this.currentLine) {
                    next();
                }
            } else if (is(currChar, '*')) {
                // Multiline comments
                boolean foundFirst = false;
                do {
                    if (!foundFirst && next(true) == '*') {
                        foundFirst = true;
                    } else if (foundFirst) {
                        if (next(true) == '/') {
                            break;
                        } else {
                            foundFirst = false;
                        }
                    }
                } while (true);
                next();
            }
        } else if (isLF()) {
            currentLine++;
            currentColumn = 0;
        }

        while (isCRLF() || (!isInStringOrComment && isSpace())) {
            next();
        }

        return currentCharacter;
    }

    /** @return the current character as an escaped string. */
    private String getEscaped() throws IOException {
        if (currentCharacter == '\\') {
            char oldChar = currentCharacter;
            next();
            if (currentCharacter == 'b') {
                return "\b";
            } else if (currentCharacter == 't') {
                return "\t";
            } else if (currentCharacter == 'n') {
                return "\n";
            } else if (currentCharacter == 'f') {
                return "\f";
            } else if (currentCharacter == 'r') {
                return "\r";
            } else if (currentCharacter == '"') {
                return "\"";
            } else if (currentCharacter == '\'') {
                return "'";
            } else if (currentCharacter == '\\') {
                return "\\";
            }

            return new String(new char[] {oldChar, currentCharacter});
        }

        return new String(new char[] {currentCharacter});
    }

    /**
     * Check if c is one of objects, throws an exception if it ain't.
     *
     * @param c The character to search for.
     * @param objects The characters to search in.
     */
    private void check(char c, Object... objects) throws NJSonCannotParseException {
        if (!is(c, objects)) {
            throw new NJSonCannotParseException("Unexpected character '" + c + "' instead of '" + Arrays.toString(objects) + "'!");
        }
    }

    /**
     * Check if c is one of objects.
     *
     * @param c The character to search for.
     * @param objects The characters to search in.
     * @return if c is one of objects.
     */
    private boolean is(char c, Object... objects) {
        boolean found = false;

        for (Object expected : objects) {
            if (expected instanceof char[]) {
                char[] values = (char[])expected;
                for (char value : values) {
                    found = is(c, value);

                    if (found) {
                        break;
                    }
                }
            } else {
                char value = (char)expected;

                if (c == value) {
                    found = true;
                }
            }

            if (found) {
                break;
            }
        }

        return found;
    }

    /** @return if the current character is a space. */
    private boolean isSpace() {
        return currentCharacter == ' ' || currentCharacter == '\t';
    }

    /** @return if the current character is a LF (line feed). */
    private boolean isLF() {
        return currentCharacter == '\n';
    }

    /** @return if the current character is a CRLF (carriage return line feed). */
    private boolean isCRLF() {
        return currentCharacter == '\r' || isLF();
    }
}
