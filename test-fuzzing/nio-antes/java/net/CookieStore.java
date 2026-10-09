package java.net;

import java.util.List;

// Where the stored cookies live.
//
// The store is defined as an interface --and not as a concrete class-- because persistence is the
// application's decision: in memory for as long as the process lasts, in a file, or shared between
// several. `CookieManager` uses an in-memory one if it is not given another.
//
// The two queries are not the same: `get(URI)` returns the ones that **belong** to that URI, applying
// the domain rules, and `getCookies()` returns all of them. The first is the one the HTTP client
// uses; the second, whoever wants to inspect or export the store.
//
// Keeping and looking up is pure computation. Nothing omitted.
public interface CookieStore {

    /**
     * Stores {@code cookie} as having come from {@code uri}.
     *
     * <p>If there was already one with the same name, domain and path, it replaces it: those three
     * fields are a cookie's identity (see {@link HttpCookie#equals}).
     */
    void add(URI uri, HttpCookie cookie);

    /** The cookies that belong to {@code uri}, with the expired ones already discarded. */
    List<HttpCookie> get(URI uri);

    /** Every live cookie in the store. */
    List<HttpCookie> getCookies();

    /** The URIs that have some cookie associated with them. */
    List<URI> getURIs();

    /** Removes that cookie. Returns whether it was there. */
    boolean remove(URI uri, HttpCookie cookie);

    /** Empties the store. Returns whether there was anything to remove. */
    boolean removeAll();
}
