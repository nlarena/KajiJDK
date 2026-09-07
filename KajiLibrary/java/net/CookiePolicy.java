package java.net;

// The decision of whether to accept a cookie.
//
// It is a single-method interface with three ready-made implementations, and the three constants are
// the useful part: `ACCEPT_ORIGINAL_SERVER` --the one `CookieManager` uses by default-- is the only
// defence the platform ships against one server setting cookies for another domain. Without it, an
// ad embedded in a page could write cookies for the page hosting it.
//
// The policy is separated from the store on purpose: keeping and deciding are two different
// decisions, and an application usually wants to change only one of the two.
//
// Deciding is pure computation. Nothing omitted.
public interface CookiePolicy {

    /** Accepts them all. Useful for tests; in production it is a policy with no defence. */
    CookiePolicy ACCEPT_ALL = (uri, cookie) -> true;

    /** Accepts none. */
    CookiePolicy ACCEPT_NONE = (uri, cookie) -> false;

    /** Only from the server that sends it: the cookie's domain has to cover the URI's host. */
    CookiePolicy ACCEPT_ORIGINAL_SERVER = (uri, cookie) -> {
        if (uri == null || cookie == null) {
            return false;
        }
        return HttpCookie.domainMatches(cookie.getDomain(), uri.getHost());
    };

    /** Whether {@code cookie}, arrived from {@code uri}, is kept. */
    boolean shouldAccept(URI uri, HttpCookie cookie);
}
