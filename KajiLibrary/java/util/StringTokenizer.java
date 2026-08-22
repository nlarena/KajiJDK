package java.util;

// The pre-regex way to split a string: it hands back tokens one at a time, treating every
// character of the delimiter string as a separator. {@link java.util.regex} replaced it, but it
// is still what a lot of old code uses — and it is an Enumeration, which dates it precisely.
//
// With `returnDelims` on, the delimiters come back as tokens of their own.
public class StringTokenizer implements Enumeration<Object> {

    private final String str;
    private String delimiters;
    private final boolean returnDelims;
    private int position;

    public StringTokenizer(String str, String delim, boolean returnDelims) {
        this.str = str;
        this.delimiters = delim;
        this.returnDelims = returnDelims;
    }

    public StringTokenizer(String str, String delim) {
        this(str, delim, false);
    }

    // The default delimiters: the usual whitespace set.
    public StringTokenizer(String str) {
        this(str, " \t\n\r\f", false);
    }

    private boolean isDelimiter(char c) {
        boolean found = false;
        for (int i = 0; i < delimiters.length(); i++) {
            if (delimiters.charAt(i) == c) {
                found = true;
            }
        }
        return found;
    }

    // The index of the next token start, skipping delimiters unless they are returned too.
    private int skipDelimiters(int from) {
        int i = from;
        if (!returnDelims) {
            while (i < str.length() && isDelimiter(str.charAt(i))) {
                i++;
            }
        }
        return i;
    }

    public boolean hasMoreTokens() {
        return skipDelimiters(position) < str.length();
    }

    public String nextToken() {
        position = skipDelimiters(position);
        if (position >= str.length()) {
            throw new NoSuchElementException();
        }
        int start = position;
        char first = str.charAt(position);
        if (returnDelims && isDelimiter(first)) {
            position++;
        } else {
            while (position < str.length() && !isDelimiter(str.charAt(position))) {
                position++;
            }
        }
        return str.substring(start, position);
    }

    // Switch delimiters mid-stream, then take the next token.
    public String nextToken(String delim) {
        this.delimiters = delim;
        return nextToken();
    }

    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    public Object nextElement() {
        return nextToken();
    }

    // How many tokens are left, counted by running the scan without consuming it.
    public int countTokens() {
        int count = 0;
        int saved = position;
        while (hasMoreTokens()) {
            nextToken();
            count++;
        }
        position = saved;
        return count;
    }
}
