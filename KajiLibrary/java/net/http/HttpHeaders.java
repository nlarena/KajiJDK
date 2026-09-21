package java.net.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * The headers of a request or a response, <strong>immutable</strong>.
 *
 * <h2>How it differs from {@code com.sun.net.httpserver.Headers}</h2>
 *
 * <p>That one implements {@link Map} and is mutated; this one implements nothing and cannot be
 * changed. The difference is not taste: an {@link HttpResponse} can be shared between threads —the
 * client is asynchronous— and mutable headers would be unsynchronized shared state.
 *
 * <p>That is why {@link #map} returns an unmodifiable map and the lists inside are unmodifiable
 * too: doing it halfway would leave a door open that nobody would expect to find.
 *
 * <h2>The filter in {@link #of}</h2>
 *
 * <p>The only factory takes a predicate that decides which headers get in. It exists because
 * headers come from the network: those of the previous hop, those a proxy added, those that should
 * not be forwarded. Filtering on construction is what spares anyone from having to remember to do
 * it later.
 *
 * <p>Keys are case-insensitive, as HTTP requires, but <strong>kept</strong> as they were written:
 * {@link #map} returns them as they arrived. Two keys that differ only in case are not rejected as
 * in the JDK: the spelling met first while iterating the given map stays, and the later key's
 * values replace its values.
 *
 * @since 11
 */
public final class HttpHeaders {

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    /** The first value of that header, if present. */
    public Optional<String> firstValue(String name) {
        List<String> l = this.headers.get(name);
        return l == null || l.isEmpty() ? Optional.<String>empty() : Optional.of(l.get(0));
    }

    /**
     * The first value as a {@code long}, if present and a number.
     *
     * <p>An {@link OptionalLong} and not an {@code Optional<Long>}: it is the accessor for {@code
     * Content-Length}, which is looked at on every response, and it saves boxing a {@code Long} per
     * query.
     *
     * @throws NumberFormatException if present but not a number — a malformed header is an error on
     *     the other side, not an "absent"
     */
    public OptionalLong firstValueAsLong(String name) {
        List<String> l = this.headers.get(name);
        if (l == null || l.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(l.get(0)));
    }

    /** All the values of that header; empty if absent. */
    public List<String> allValues(String name) {
        List<String> l = this.headers.get(name);
        return l == null ? Collections.<String>emptyList() : l;
    }

    /** All the headers, unmodifiable. */
    public Map<String, List<String>> map() {
        return this.headers;
    }

    /** Over the whole map, case-insensitive in the keys. */
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpHeaders)) {
            return false;
        }
        return this.headers.equals(((HttpHeaders) obj).headers);
    }

    public final int hashCode() {
        // Over the lowercased keys, to stay consistent with an `equals` that does not distinguish
        // case.
        int h = 0;
        for (Map.Entry<String, List<String>> e : this.headers.entrySet()) {
            h = h + e.getKey().toLowerCase(java.util.Locale.ROOT).hashCode()
                    ^ e.getValue().hashCode();
        }
        return h;
    }

    public String toString() {
        return super.toString() + " { " + this.headers.toString() + " }";
    }

    /**
     * The headers that pass the filter.
     *
     * @param headerMap where they come from
     * @param filter receives name and value, and decides whether it gets in
     * @throws NullPointerException if anything is {@code null}
     * @throws IllegalArgumentException if a name is empty. This javadoc also said a name left with
     *     no values after filtering is an error; such a name is left out, as in the JDK. Unlike the
     *     JDK, a name that is blank only after trimming, or two names equal ignoring case, are not
     *     rejected.
     */
    public static HttpHeaders of(Map<String, List<String>> headerMap,
            BiPredicate<String, String> filter) {
        if (headerMap == null || filter == null) {
            throw new NullPointerException("headerMap and filter must not be null");
        }
        // TreeMap with a case-ignoring comparator: it is how the case-insensitive lookup is had
        // without losing the original spelling of the key.
        TreeMap<String, List<String>> out =
                new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> e : headerMap.entrySet()) {
            String name = e.getKey();
            if (name == null) {
                throw new NullPointerException("a header name is null");
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("a header name is empty");
            }
            List<String> values = e.getValue();
            if (values == null) {
                throw new NullPointerException("the values of " + name + " are null");
            }
            List<String> kept = new ArrayList<String>();
            for (String v : values) {
                if (v == null) {
                    throw new NullPointerException("a value of " + name + " is null");
                }
                if (filter.test(name, v)) {
                    kept.add(v);
                }
            }
            if (!kept.isEmpty()) {
                out.put(name, Collections.unmodifiableList(kept));
            }
        }
        return new HttpHeaders(Collections.unmodifiableMap(out));
    }
}
