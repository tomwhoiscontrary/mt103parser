package io.pivotal.mt103;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ISO15022Parser {

    private static final int TOKEN_EOF = -1;
    private static final char TOKEN_START_BLOCK = '{';
    private static final char TOKEN_END_BLOCK = '}';
    private static final char TOKEN_TAG_SEPARATOR = ':';
    private static final char TOKEN_FIELD_SEPARATOR_1 = '\r';
    private static final char TOKEN_FIELD_SEPARATOR_2 = '\n';
    private static final char TOKEN_END_MESSAGE_BLOCK = '-';
    private static final String MULTILINE_STRING_JOINER = "\n";

    public static Map<String, Object> parse(String message) throws IOException, ParseException {
        return parse(new StringReader(message));
    }

    public static Map<String, Object> parse(Reader input) throws IOException, ParseException {
        return readBlocks(new CountingReader(input), TOKEN_EOF);
    }

    public static Map<String, Object> parseOne(CountingReader input, char terminator) throws IOException, ParseException {
        Map<String, Object> blocks = readBlocks(input, terminator);
        consume(input, terminator);
        return blocks;
    }

    private static Map<String, Object> readBlocks(CountingReader in, int endToken) throws IOException, ParseException {
        Map<String, Object> blocks = new HashMap<>();
        while (true) {
            int ch = in.peek();
            if (ch == TOKEN_START_BLOCK) {
                blocks.putAll(readBlock(in));
            } else if (ch == endToken) {
                return blocks;
            } else {
                throw newParseException("unexpected token", (char) ch, in);
            }
        }
    }

    private static Map<String, Object> readBlock(CountingReader in) throws IOException, ParseException {
        consume(in, TOKEN_START_BLOCK);

        Map<String, Object> block = new HashMap<>();

        if (in.peek() != TOKEN_END_BLOCK) {
            String tag = readString(in, TOKEN_TAG_SEPARATOR);

            consume(in, TOKEN_TAG_SEPARATOR);

            Object value;
            int ch = in.peek();
            if (ch == TOKEN_START_BLOCK) {
                value = readBlocks(in, TOKEN_END_BLOCK);
            } else if (ch == TOKEN_FIELD_SEPARATOR_1) {
                consume(in, TOKEN_FIELD_SEPARATOR_1, TOKEN_FIELD_SEPARATOR_2);
                value = readMessageBlock(in);
            } else {
                value = readString(in, TOKEN_END_BLOCK);
            }
            block.put(tag, value);
        }

        consume(in, TOKEN_END_BLOCK);

        return block;
    }

    private static Map<String, Object> readMessageBlock(CountingReader in) throws IOException, ParseException {
        HashMap<String, Object> block = new HashMap<>();

        while (true) {
            int ch = in.peek();
            if (ch == TOKEN_TAG_SEPARATOR) {
                consume(in, TOKEN_TAG_SEPARATOR);
                String tag = readString(in, TOKEN_TAG_SEPARATOR);
                consume(in, TOKEN_TAG_SEPARATOR);
                block.put(tag, readMultilineString(in, TOKEN_TAG_SEPARATOR, TOKEN_END_MESSAGE_BLOCK));
            } else if (ch == TOKEN_END_MESSAGE_BLOCK) {
                consume(in, TOKEN_END_MESSAGE_BLOCK);
                return block;
            } else {
                throw newParseException("unexpected token", (char) ch, in);
            }
        }
    }

    private static String readMultilineString(CountingReader in, Character... endTokens) throws IOException, ParseException {
        List<Character> endTokensList = Arrays.asList(endTokens);

        List<String> lines = new ArrayList<>();

        while (true) {
            lines.add(readString(in, TOKEN_FIELD_SEPARATOR_1));
            consume(in, TOKEN_FIELD_SEPARATOR_1, TOKEN_FIELD_SEPARATOR_2);

            int ch = in.peek();
            if (endTokensList.contains((char) ch)) {
                break;
            }
        }

        return String.join(MULTILINE_STRING_JOINER, lines);
    }

    private static String readString(CountingReader in, char endToken) throws IOException, ParseException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = in.read();
            if (ch == TOKEN_EOF) {
                throw newEndOfInputException(in);
            } else if (ch == endToken) {
                in.unread(ch);
                return buf.toString();
            } else {
                buf.append((char) ch);
            }
        }
    }

    private static void consume(CountingReader in, char... tokens) throws IOException, ParseException {
        for (char token : tokens) {
            int ch = in.read();
            if (ch != token) {
                throw newParseException("unexpected token (expected '" + token + "')", (char) ch, in);
            }
        }
    }

    private static ParseException newParseException(String message, int i, CountingReader in) {
        if (i == TOKEN_EOF) return newEndOfInputException(in);
        return new ParseException(message + ": '" + (char) i + "' @ " + in.getCount(), in.getCount());
    }

    private static ParseException newEndOfInputException(CountingReader in) {
        return new ParseException("end of input @ " + in.getCount(), in.getCount());
    }

}
