package java.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The store `CookieManager` uses when it is not given another: all in memory and nothing on disk.
//
// It is not public and is not part of the API -- it is reached through `new CookieManager()` and used
// through `CookieStore`. The JDK does exactly the same with a class of the same name.
//
// It keeps each cookie together with the **effective URI** it came from, which is the URI cut down to
// scheme and host ("http://example.org"). The trimming is what lets two pages of the same site share
// cookies without the path or the port separating them; the path rules are applied by `CookieManager`
// further up, which is where they belong.
final class InMemoryCookieStore implements CookieStore {

    // A list and not a map: the store is small and every interesting operation --domain matching,
    // discarding expired ones-- walks it anyway. An index by domain here would only add one more
    // piece of state to keep consistent.
    private final List<URI> uris = new ArrayList<URI>();
    private final List<HttpCookie> cookies = new ArrayList<HttpCookie>();

    InMemoryCookieStore() {
    }

    public void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        }
        synchronized (this) {
            // A cookie with the same name, domain and path is **the same** cookie with another
            // value, so it overwrites the previous one instead of adding to it.
            int i = this.cookies.indexOf(cookie);
            while (i >= 0) {
                this.cookies.remove(i);
                this.uris.remove(i);
                i = this.cookies.indexOf(cookie);
            }
            // A maxAge of zero means "expired at birth": it is how a server deletes a cookie.
            // Storing it would be storing exactly what it asked to have deleted.
            if (cookie.getMaxAge() != 0) {
                this.cookies.add(cookie);
                this.uris.add(effectiveUri(uri));
            }
        }
    }

    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        List<HttpCookie> out = new ArrayList<HttpCookie>();
        synchronized (this) {
            this.purge();
            boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
            String host = uri.getHost();
            URI eff = effectiveUri(uri);
            int i = 0;
            while (i < this.cookies.size()) {
                HttpCookie c = this.cookies.get(i);
                // A cookie marked `Secure` does not go out over a link that is not: that is its
                // entire purpose.
                if (secureLink || !c.getSecure()) {
                    boolean matches;
                    if (c.getDomain() != null) {
                        matches = HttpCookie.domainMatches(c.getDomain(), host);
                    } else {
                        // With no domain, it only goes back to the same site it came from.
                        matches = eff != null && eff.equals(this.uris.get(i));
                    }
                    if (matches && !out.contains(c)) {
                        out.add(c);
                    }
                }
                i = i + 1;
            }
        }
        return out;
    }

    public List<HttpCookie> getCookies() {
        synchronized (this) {
            this.purge();
            return Collections.unmodifiableList(new ArrayList<HttpCookie>(this.cookies));
        }
    }

    public List<URI> getURIs() {
        List<URI> out = new ArrayList<URI>();
        synchronized (this) {
            int i = 0;
            while (i < this.uris.size()) {
                URI u = this.uris.get(i);
                if (u != null && !out.contains(u)) {
                    out.add(u);
                }
                i = i + 1;
            }
        }
        return Collections.unmodifiableList(out);
    }

    public boolean remove(URI uri, HttpCookie ck) {
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        }
        synchronized (this) {
            int i = this.cookies.indexOf(ck);
            if (i < 0) {
                return false;
            }
            this.cookies.remove(i);
            this.uris.remove(i);
            return true;
        }
    }

    public boolean removeAll() {
        synchronized (this) {
            if (this.cookies.isEmpty()) {
                return false;
            }
            this.cookies.clear();
            this.uris.clear();
            return true;
        }
    }

    // The expired ones are removed on lookup and not by a timer: a store nobody consults matters to
    // nobody, and a cleaning thread would be more machinery than benefit.
    private void purge() {
        int i = 0;
        while (i < this.cookies.size()) {
            if (this.cookies.get(i).hasExpired()) {
                this.cookies.remove(i);
                this.uris.remove(i);
            } else {
                i = i + 1;
            }
        }
    }

    // The URI cut down to scheme and host. If it cannot be built, the original is kept: a key that is
    // too specific --which only makes the cookie go back to fewer places-- is preferable to losing
    // it.
    private static URI effectiveUri(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            return new URI("http", uri.getHost(), null, null, null);
        } catch (URISyntaxException e) {
            return uri;
        }
    }
}
