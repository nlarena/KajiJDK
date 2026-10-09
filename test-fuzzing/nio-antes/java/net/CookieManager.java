package java.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The `CookieHandler` implementation the platform ships: a store plus a policy.
//
// The split into three pieces --handler, store, policy-- is what lets one be changed without touching
// the others: persisting to disk means changing the `CookieStore`, tightening the criterion means
// changing the `CookiePolicy`, and the code making requests hears about neither.
//
// This class's real work is in `put`, and it is filling in what the server did not say: a cookie with
// no `Path` inherits the **directory** of the page that sent it (not the page), and one with no
// `Domain` inherits the host. Both defaults come from RFC 2965 and both are restrictive on purpose --
// a cookie with no attributes goes back to the smallest possible place, not the largest.
//
// The other detail people overlook: the order the cookies come out in from `get` **is part of the
// protocol**. RFC 2965 asks for the most specific path first, because a server receiving two cookies
// of the same name keeps the first. It is sorted in `sortByPathAndAge`.
//
// ===========================================================================================
// WHY THIS GOES IN COMPLETE WITH NO NETWORK
// ===========================================================================================
//
// `get` and `put` take and return **header maps**. They open nothing and send nothing: they translate
// between header text and the store. The whole contract is fulfilled here. What KajiJDK lacks is an
// HTTP client to call these methods, but that is an absence on the other side of the interface, not a
// hole in this class.
//
// Nothing omitted.
public class CookieManager extends CookieHandler {

    private CookiePolicy policyCallback;
    private CookieStore cookieJar;

    /** A manager with an in-memory store and the {@link CookiePolicy#ACCEPT_ORIGINAL_SERVER} policy. */
    public CookieManager() {
        this(null, null);
    }

    /**
     * A manager with that store and that policy; null in either of the two takes the default.
     */
    public CookieManager(CookieStore store, CookiePolicy cookiePolicy) {
        if (cookiePolicy == null) {
            this.policyCallback = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        } else {
            this.policyCallback = cookiePolicy;
        }
        if (store == null) {
            this.cookieJar = new InMemoryCookieStore();
        } else {
            this.cookieJar = store;
        }
    }

    /** Changes the policy. The store cannot be changed: the cookies already stored are its own. */
    public void setCookiePolicy(CookiePolicy cookiePolicy) {
        if (cookiePolicy != null) {
            this.policyCallback = cookiePolicy;
        }
    }

    public CookieStore getCookieStore() {
        return this.cookieJar;
    }

    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
            throws IOException {
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        Map<String, List<String>> cookieMap = new HashMap<String, List<String>>();
        if (this.cookieJar == null) {
            return Collections.unmodifiableMap(cookieMap);
        }
        boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        List<HttpCookie> cookies = new ArrayList<HttpCookie>();
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            path = "/";
        }
        List<HttpCookie> candidates = this.cookieJar.get(uri);
        int i = 0;
        while (i < candidates.size()) {
            HttpCookie cookie = candidates.get(i);
            if (pathMatches(path, cookie.getPath()) && (secureLink || !cookie.getSecure())) {
                cookies.add(cookie);
            }
            i = i + 1;
        }
        cookieMap.put("Cookie", sortByPathAndAge(cookies));
        return Collections.unmodifiableMap(cookieMap);
    }

    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        if (this.cookieJar == null) {
            return;
        }
        for (String headerKey : responseHeaders.keySet()) {
            if (headerKey == null) {
                continue;
            }
            if (!headerKey.equalsIgnoreCase("Set-Cookie2")
                    && !headerKey.equalsIgnoreCase("Set-Cookie")) {
                continue;
            }
            List<String> values = responseHeaders.get(headerKey);
            if (values == null) {
                continue;
            }
            int v = 0;
            while (v < values.size()) {
                String headerValue = values.get(v);
                v = v + 1;
                List<HttpCookie> cookies;
                try {
                    cookies = HttpCookie.parse(headerValue);
                } catch (IllegalArgumentException e) {
                    // A broken header does not invalidate the others in the same message.
                    continue;
                }
                int c = 0;
                while (c < cookies.size()) {
                    HttpCookie cookie = cookies.get(c);
                    c = c + 1;
                    if (cookie.getPath() == null) {
                        // The page's **directory**, not the page: /dir/page gives "/dir/".
                        String p = uri.getPath();
                        if (p == null) {
                            p = "/";
                        }
                        if (!p.endsWith("/")) {
                            int slash = p.lastIndexOf('/');
                            if (slash > 0) {
                                p = p.substring(0, slash + 1);
                            } else {
                                p = "/";
                            }
                        }
                        cookie.setPath(p);
                    }
                    if (cookie.getDomain() == null) {
                        String host = uri.getHost();
                        // A host with no dots ("intranet") cannot match any domain with an internal
                        // dot; the ".local" suffix is what gives it one.
                        if (host != null && host.indexOf('.') == -1) {
                            host = host + ".local";
                        }
                        cookie.setDomain(host);
                    }
                    String ports = cookie.getPortlist();
                    if (ports != null) {
                        int port = uri.getPort();
                        if (port == -1) {
                            port = "https".equals(uri.getScheme()) ? 443 : 80;
                        }
                        if (ports.length() == 0) {
                            // A valueless Port means "only the port it arrived on".
                            cookie.setPortlist("" + port);
                            if (this.shouldAcceptInternal(uri, cookie)) {
                                this.cookieJar.add(uri, cookie);
                            }
                        } else if (isInPortList(ports, port)) {
                            if (this.shouldAcceptInternal(uri, cookie)) {
                                this.cookieJar.add(uri, cookie);
                            }
                        }
                    } else if (this.shouldAcceptInternal(uri, cookie)) {
                        this.cookieJar.add(uri, cookie);
                    }
                }
            }
        }
    }

    // A policy that blows up cannot decide, and "could not decide" is "no". Letting the exception rise
    // would make a badly written policy break the whole request.
    private boolean shouldAcceptInternal(URI uri, HttpCookie cookie) {
        try {
            return this.policyCallback.shouldAccept(uri, cookie);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isInPortList(String lst, int port) {
        int i = lst.indexOf(",");
        int val = -1;
        while (i > 0) {
            try {
                val = Integer.parseInt(lst.substring(0, i));
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // An unreadable port is skipped; the others in the list still count.
            }
            lst = lst.substring(i + 1);
            i = lst.indexOf(",");
        }
        if (lst.length() > 0) {
            try {
                val = Integer.parseInt(lst);
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // ditto
            }
        }
        return false;
    }

    // The cookie's path has to be a prefix of the request's. It is a textual prefix and not a
    // segment one, just as in the JDK: "/ab" covers "/abc".
    private static boolean pathMatches(String path, String pathToMatchWith) {
        if (path == pathToMatchWith) {
            return true;
        }
        if (path == null || pathToMatchWith == null) {
            return false;
        }
        return path.startsWith(pathToMatchWith);
    }

    // Longest path first, and for equal paths the oldest first. See the header: the order is part of
    // the protocol, not a convenience.
    static List<String> sortByPathAndAge(List<HttpCookie> cookies) {
        List<HttpCookie> copy = new ArrayList<HttpCookie>(cookies);
        int i = 1;
        while (i < copy.size()) {
            HttpCookie c = copy.get(i);
            int j = i - 1;
            while (j >= 0 && precedes(c, copy.get(j))) {
                copy.set(j + 1, copy.get(j));
                j = j - 1;
            }
            copy.set(j + 1, c);
            i = i + 1;
        }
        List<String> out = new ArrayList<String>();
        i = 0;
        while (i < copy.size()) {
            out.add(copy.get(i).toString());
            i = i + 1;
        }
        return out;
    }

    private static boolean precedes(HttpCookie a, HttpCookie b) {
        int la = a.getPath() == null ? 0 : a.getPath().length();
        int lb = b.getPath() == null ? 0 : b.getPath().length();
        if (la != lb) {
            return la > lb;
        }
        return a.getCreationTime() < b.getCreationTime();
    }
}
