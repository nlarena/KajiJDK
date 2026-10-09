package java.util.regex;

import java.util.Map;

// KajiLibrary's java.util.regex.MatchResult — the read-only result of a match operation.
// Matcher implements it directly (so a live Matcher *is* a MatchResult, exactly as in the
// JDK), and Matcher.toMatchResult() hands back an immutable snapshot that survives the next
// find()/reset().
//
// The by-name overloads are `default` methods that resolve the name through namedGroups(),
// the same shape the JDK uses since 20. A MatchResult that has no notion of named groups
// keeps the inherited namedGroups()/hasMatch(), which throw UnsupportedOperationException —
// again the JDK's own defaults.
public interface MatchResult {

    int start();

    int start(int group);

    default int start(String name) {
        return this.start(groupNumber(this.namedGroups(), name));
    }

    int end();

    int end(int group);

    default int end(String name) {
        return this.end(groupNumber(this.namedGroups(), name));
    }

    String group();

    String group(int group);

    default String group(String name) {
        return this.group(groupNumber(this.namedGroups(), name));
    }

    int groupCount();

    // Maps each named capturing group to its group number. The JDK's default throws; an
    // implementation backed by a Pattern (i.e. Matcher and our snapshot) overrides it.
    default Map<String, Integer> namedGroups() {
        throw new UnsupportedOperationException("namedGroups()");
    }

    // Resolves a group name to its number.
    //
    // DEVIATION: the JDK declares this as `private int groupNumber(String)` — a private
    // *instance* interface method. Our javac mis-handles those: it counts them as abstract
    // members, so every implementer is rejected with "no es abstracta y no implementa
    // groupNumber" (JLS 9.4: a private interface method is implicitly non-abstract and is
    // not inherited). `private static` is compiled correctly, so the helper takes the map as
    // a parameter instead. Private either way, so the public API surface is unchanged.
    private static int groupNumber(Map<String, Integer> named, String name) {
        Integer number = named.get(name);
        if (number != null) {
            return number.intValue();
        }
        throw new IllegalArgumentException("No group with name <" + name + ">");
    }

    default boolean hasMatch() {
        throw new UnsupportedOperationException("hasMatch()");
    }
}

// The immutable snapshot handed back by Matcher.toMatchResult() and by Matcher.results().
// Package-private and top-level: the JDK nests it inside Matcher, but our javac has trouble
// with generic-enclosing capture, and the API gate skips package-private types with no JDK
// counterpart (the same deal Node/RegexParser already have).
final class ImmutableMatchResult implements MatchResult {

    private final int first;
    private final int last;
    private final int[] groups;
    private final String text;
    private final Pattern parentPattern;

    ImmutableMatchResult(int first, int last, int[] groups, String text, Pattern parentPattern) {
        this.first = first;
        this.last = last;
        this.groups = groups;
        this.text = text;
        this.parentPattern = parentPattern;
    }

    private void checkMatch() {
        if (this.first < 0) {
            throw new IllegalStateException("No match found");
        }
    }

    public int start() {
        this.checkMatch();
        return this.first;
    }

    public int start(int group) {
        this.checkMatch();
        if (group < 0 || group > this.groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        return this.groups[group * 2];
    }

    public int end() {
        this.checkMatch();
        return this.last;
    }

    public int end(int group) {
        this.checkMatch();
        if (group < 0 || group > this.groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        return this.groups[group * 2 + 1];
    }

    public String group() {
        return this.group(0);
    }

    public String group(int group) {
        this.checkMatch();
        if (group < 0 || group > this.groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        int s = this.groups[group * 2];
        int e = this.groups[group * 2 + 1];
        if (s < 0 || e < 0) {
            return null;
        }
        return this.text.substring(s, e);
    }

    public int groupCount() {
        return this.groups.length / 2 - 1;
    }

    public Map<String, Integer> namedGroups() {
        return this.parentPattern.namedGroups();
    }

    public boolean hasMatch() {
        return this.first >= 0;
    }

    public String toString() {
        return this.text;
    }
}
