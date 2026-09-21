package java.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

// An HTTP cookie: a name-value pair with rules about who it is sent back to.
//
// The class carries **three specifications that contradict each other**: Netscape's original draft,
// RFC 2109 and RFC 2965. The visible difference is the version: a version 0 cookie prints as "n=v"
// and uses the `Expires` attribute; a version 1 one prints with quotes and `$Path`/`$Domain`, and
// uses `Max-Age`. `parse` does not pick the version by taste but **guesses it from the text**, and
// the rules of that guess are in `guessVersion`: if "expires=" turns up it is Netscape, if
// "version=" or "max-age" turns up it is RFC. That explains something that confuses everybody:
// parsing "foo=bar" gives version 0, but `new HttpCookie("foo","bar")` gives version 1.
//
// The other consequence of the mixture: the separator between cookies. In the RFC format several
// cookies come in one header separated by commas, and in Netscape's the comma is a legal part of
// the `Expires` date. That is why `parse` only splits on commas when it guessed version 1 -- and
// even there it respects the quotes.
//
// ===========================================================================================
// THE ONE THING THAT IS NOT IDENTICAL TO THE JDK: PARSING `Expires`
// ===========================================================================================
//
// The JDK tries six `SimpleDateFormat` patterns one after another. Here the date is parsed with a
// reader of our own that recognizes the same six forms --day of the week, day, month, two- or
// four-digit year, time-- but does **not** reproduce `SimpleDateFormat`'s quirks with malformed
// input. The two-digit year rule is the same as RFC 6265's and the JDK's: under 70 is 20xx,
// otherwise 19xx.
//
// When the date is not understood, the result is `maxAge = 0`, that is, "already expired", which is
// also what the JDK does. The failure mode is conservative: a cookie that is not stored, never one
// that is stored too readily.
//
// Nothing else is omitted. A cookie is a piece of data, not a connection.
public final class HttpCookie implements Cloneable {

    // That the maximum is unset is encoded as -1, which is also the value the user can set to say
    // "until the browser closes". Both mean "it does not expire on its own".
    private static final long MAX_AGE_UNSPECIFIED = -1;

    private static final String SET_COOKIE = "set-cookie:";
    private static final String SET_COOKIE2 = "set-cookie2:";

    // What cannot appear in an RFC 2616 token. The space is in there on purpose.
    private static final String TSPECIALS = ",; ";

    private final String name;
    private String value;

    private String comment;
    private String commentURL;
    private boolean toDiscard;
    private String domain;
    private long maxAge = MAX_AGE_UNSPECIFIED;
    private String path;
    private String portlist;
    private boolean secure;
    private boolean httpOnly;
    private int version = 1;

    // The moment it was created, which is what `maxAge` is measured against. Without this field
    // `hasExpired` would have no origin: "it lasts a hundred seconds" says nothing without saying
    // from when.
    private final long whenCreated;

    /**
     * A new cookie with that name and that value.
     *
     * <p>It is born at version 1 (RFC 2965). See the header: `parse` may give it version 0.
     *
     * @throws IllegalArgumentException if the name is not a token, is empty, or starts with '$'
     *     (that prefix is reserved by the protocol for its own attributes)
     */
    public HttpCookie(String name, String value) {
        this(name, value, System.currentTimeMillis());
    }

    HttpCookie(String name, String value, long creationTime) {
        name = name.trim();
        if (name.length() == 0 || !isToken(name) || name.charAt(0) == '$') {
            throw new IllegalArgumentException("Illegal cookie name");
        }
        this.name = name;
        this.value = value;
        this.whenCreated = creationTime;
    }

    /**
     * The cookies a {@code Set-Cookie} or {@code Set-Cookie2} header describes.
     *
     * <p>The header prefix may or may not be there. Unrecognized attributes are ignored silently,
     * which is what the protocol requires: a new server must not break an old client.
     *
     * @throws IllegalArgumentException if the text has no valid name-value pair at the start
     */
    public static List<HttpCookie> parse(String header) {
        int version = guessVersion(header);
        String body = header;
        if (startsWithIgnoreCase(body, SET_COOKIE2)) {
            body = body.substring(SET_COOKIE2.length());
        } else if (startsWithIgnoreCase(body, SET_COOKIE)) {
            body = body.substring(SET_COOKIE.length());
        }
        List<HttpCookie> cookies = new ArrayList<HttpCookie>();
        if (version == 0) {
            // Netscape: the comma is part of the `Expires` date, so it is not split on commas and
            // the header carries a single cookie.
            HttpCookie cookie = parseInternal(body);
            cookie.setVersion(0);
            cookies.add(cookie);
        } else {
            List<String> parts = splitMultiCookies(body);
            int i = 0;
            while (i < parts.size()) {
                HttpCookie cookie = parseInternal(parts.get(i));
                cookie.setVersion(1);
                cookies.add(cookie);
                i = i + 1;
            }
        }
        return cookies;
    }

    /** Whether it has expired, by its {@code maxAge} and the moment it was created. */
    public boolean hasExpired() {
        if (this.maxAge == 0) {
            return true;
        }
        if (this.maxAge == MAX_AGE_UNSPECIFIED) {
            return false;
        }
        long delta = (System.currentTimeMillis() - this.whenCreated) / 1000;
        return delta > this.maxAge;
    }

    /** The cookie's purpose, to show the user. Version 1 only. */
    public void setComment(String purpose) {
        this.comment = purpose;
    }

    public String getComment() {
        return this.comment;
    }

    /** A URL where the purpose is explained. Version 1 only. */
    public void setCommentURL(String purpose) {
        this.commentURL = purpose;
    }

    public String getCommentURL() {
        return this.commentURL;
    }

    /** Whether the client should throw it away on closing, ignoring its expiry. Version 1 only. */
    public void setDiscard(boolean discard) {
        this.toDiscard = discard;
    }

    public boolean getDiscard() {
        return this.toDiscard;
    }

    /** The ports it may be sent to, comma-separated. Version 1 only. */
    public void setPortlist(String ports) {
        this.portlist = ports;
    }

    public String getPortlist() {
        return this.portlist;
    }

    /** The domain it belongs to. It is stored in lower case: domains are case-insensitive. */
    public void setDomain(String pattern) {
        if (pattern != null) {
            this.domain = pattern.toLowerCase();
        } else {
            this.domain = null;
        }
    }

    public String getDomain() {
        return this.domain;
    }

    /** Seconds of life. Zero expires it on the spot; negative means "until the client closes". */
    public void setMaxAge(long expiry) {
        this.maxAge = expiry;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    /** The path prefix it is sent to. */
    public void setPath(String uri) {
        this.path = uri;
    }

    public String getPath() {
        return this.path;
    }

    /** Whether it travels over secure connections only. */
    public void setSecure(boolean flag) {
        this.secure = flag;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String getName() {
        return this.name;
    }

    public void setValue(String newValue) {
        this.value = newValue;
    }

    public String getValue() {
        return this.value;
    }

    /** 0 for the Netscape format, 1 for RFC 2965's. */
    public int getVersion() {
        return this.version;
    }

    /**
     * @throws IllegalArgumentException if it is neither 0 nor 1
     */
    public void setVersion(int v) {
        if (v != 0 && v != 1) {
            throw new IllegalArgumentException("cookie version should be 0 or 1");
        }
        this.version = v;
    }

    /** Whether it is invisible to the page's code (the classic defence against theft by XSS). */
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Whether {@code host} gets {@code domain}'s cookies, per RFC 2965.
     *
     * <p>The odd rules here are all defensive and are worth reading backwards: they exist so that
     * ".com" does **not** match "bank.com". A domain has to have an internal dot, the host's
     * leftover cannot have dots --so ".foo.com" covers "x.foo.com" but not "a.b.foo.com"-- and
     * ".foo.com" covers bare "foo.com" as a special case.
     */
    public static boolean domainMatches(String domain, String host) {
        if (domain == null || host == null) {
            return false;
        }
        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDot = domain.indexOf('.');
        if (embeddedDot == 0) {
            embeddedDot = domain.indexOf('.', 1);
        }
        if (!isLocalDomain && (embeddedDot == -1 || embeddedDot == domain.length() - 1)) {
            return false;
        }
        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
            return true;
        }
        int lengthDiff = host.length() - domain.length();
        if (lengthDiff == 0) {
            return host.equalsIgnoreCase(domain);
        }
        if (lengthDiff > 0) {
            String h = host.substring(0, lengthDiff);
            String d = host.substring(lengthDiff);
            return h.indexOf('.') == -1 && d.equalsIgnoreCase(domain);
        }
        if (lengthDiff == -1) {
            return domain.charAt(0) == '.' && host.equalsIgnoreCase(domain.substring(1));
        }
        return false;
    }

    /** The cookie in the format matching its version. */
    public String toString() {
        if (this.getVersion() > 0) {
            return this.toRFC2965HeaderString();
        }
        return this.getName() + "=" + this.getValue();
    }

    private String toRFC2965HeaderString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append("=\"").append(this.getValue()).append('"');
        if (this.getPath() != null) {
            sb.append(";$Path=\"").append(this.getPath()).append('"');
        }
        if (this.getDomain() != null) {
            sb.append(";$Domain=\"").append(this.getDomain()).append('"');
        }
        if (this.getPortlist() != null) {
            sb.append(";$Port=\"").append(this.getPortlist()).append('"');
        }
        return sb.toString();
    }

    // Two cookies are the same if name, domain and path match -- **not** the value. It comes from
    // RFC 2965 and it is what makes storing a new cookie overwrite the previous one instead of
    // accumulating them: those three fields are the identity, the value is the content.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpCookie)) {
            return false;
        }
        HttpCookie other = (HttpCookie) obj;
        return equalsIgnoreCase(this.getName(), other.getName())
                && equalsIgnoreCase(this.getDomain(), other.getDomain())
                && Objects.equals(this.getPath(), other.getPath());
    }

    public int hashCode() {
        int h1 = this.name.toLowerCase().hashCode();
        int h2 = (this.domain != null) ? this.domain.toLowerCase().hashCode() : 0;
        int h3 = (this.path != null) ? this.path.hashCode() : 0;
        return h1 + h2 + h3;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    long getCreationTime() {
        return this.whenCreated;
    }

    // ---- parsing --------------------------------------------------------------------------------

    // The version comes out of the text, not out of an attribute: see the header.
    private static int guessVersion(String header) {
        String h = header.toLowerCase();
        if (h.indexOf("expires=") != -1) {
            return 0;
        }
        if (h.indexOf("version=") != -1) {
            return 1;
        }
        if (h.indexOf("max-age") != -1) {
            return 1;
        }
        if (startsWithIgnoreCase(h, SET_COOKIE2)) {
            return 1;
        }
        return 0;
    }

    private static HttpCookie parseInternal(String header) {
        StringTokenizer tokenizer = new StringTokenizer(header, ";");
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Empty cookie header string");
        }
        String pair = tokenizer.nextToken();
        int index = pair.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException("Invalid cookie name-value pair");
        }
        String n = pair.substring(0, index).trim();
        String v = pair.substring(index + 1).trim();
        HttpCookie cookie = new HttpCookie(n, stripOffSurroundingQuote(v));
        while (tokenizer.hasMoreTokens()) {
            pair = tokenizer.nextToken();
            index = pair.indexOf('=');
            String an;
            String av;
            if (index != -1) {
                an = pair.substring(0, index).trim();
                av = pair.substring(index + 1).trim();
            } else {
                an = pair.trim();
                av = null;
            }
            assignAttribute(cookie, an, av);
        }
        return cookie;
    }

    // The "first one wins" of almost every attribute is not a whim: a header with a repeated
    // attribute is suspicious, and keeping the first is the only choice that does not depend on the
    // order some intermediary may have reordered them into.
    private static void assignAttribute(HttpCookie cookie, String attrName, String attrValue) {
        attrValue = stripOffSurroundingQuote(attrValue);
        String key = attrName.toLowerCase();
        if (key.equals("comment")) {
            if (cookie.getComment() == null) {
                cookie.setComment(attrValue);
            }
        } else if (key.equals("commenturl")) {
            if (cookie.getCommentURL() == null) {
                cookie.setCommentURL(attrValue);
            }
        } else if (key.equals("discard")) {
            cookie.setDiscard(true);
        } else if (key.equals("domain")) {
            if (cookie.getDomain() == null) {
                cookie.setDomain(attrValue);
            }
        } else if (key.equals("max-age")) {
            long maxage;
            try {
                maxage = Long.parseLong(attrValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal cookie max-age attribute");
            }
            if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED) {
                cookie.setMaxAge(maxage);
            }
        } else if (key.equals("path")) {
            if (cookie.getPath() == null) {
                cookie.setPath(attrValue);
            }
        } else if (key.equals("port")) {
            if (cookie.getPortlist() == null) {
                cookie.setPortlist(attrValue == null ? "" : attrValue);
            }
        } else if (key.equals("secure")) {
            cookie.setSecure(true);
        } else if (key.equals("httponly")) {
            cookie.setHttpOnly(true);
        } else if (key.equals("version")) {
            try {
                cookie.setVersion(Integer.parseInt(attrValue));
            } catch (NumberFormatException e) {
                // A version number that is not understood does not invalidate the cookie.
            }
        } else if (key.equals("expires")) {
            if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED) {
                long delta = cookie.expiryDate2DeltaSeconds(attrValue);
                cookie.setMaxAge(delta > 0 ? delta : 0);
            }
        }
        // Any other attribute is ignored: see the javadoc of `parse`.
    }

    // Turns an absolute `Expires` date into the seconds of life it has left, counted from when this
    // cookie was created. Zero --or less-- means expired.
    private long expiryDate2DeltaSeconds(String dateString) {
        long millis = parseCookieDate(dateString);
        if (millis == Long.MIN_VALUE) {
            return 0;
        }
        return (millis - this.whenCreated) / 1000;
    }

    private static final String[] MONTHS = {
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    // The reader for cookie dates. See the header for the exact scope. It returns `Long.MIN_VALUE`
    // when it does not understand.
    static long parseCookieDate(String s) {
        if (s == null) {
            return Long.MIN_VALUE;
        }
        int day = -1;
        int month = -1;
        int year = -1;
        boolean twoDigitYear = false;
        int hh = -1;
        int mm = -1;
        int ss = -1;
        StringTokenizer st = new StringTokenizer(s, " ,-\t");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.indexOf(':') != -1) {
                if (hh != -1) {
                    return Long.MIN_VALUE;
                }
                StringTokenizer ht = new StringTokenizer(tok, ":");
                int[] parts = new int[3];
                int k = 0;
                while (ht.hasMoreTokens() && k < 3) {
                    parts[k] = parseUnsigned(ht.nextToken());
                    if (parts[k] < 0) {
                        return Long.MIN_VALUE;
                    }
                    k = k + 1;
                }
                if (k != 3 || ht.hasMoreTokens()) {
                    return Long.MIN_VALUE;
                }
                hh = parts[0];
                mm = parts[1];
                ss = parts[2];
                continue;
            }
            int m = monthIndex(tok);
            if (m >= 0) {
                if (month != -1) {
                    return Long.MIN_VALUE;
                }
                month = m;
                continue;
            }
            int num = parseUnsigned(tok);
            if (num >= 0) {
                // A numeric token is the day if there is no day yet and it fits a month; otherwise
                // the year. It is the only ambiguity of these grammars and it is resolved by
                // position.
                if (day == -1 && tok.length() <= 2 && num >= 1 && num <= 31) {
                    day = num;
                } else if (year == -1) {
                    year = num;
                    twoDigitYear = tok.length() <= 2;
                } else {
                    return Long.MIN_VALUE;
                }
                continue;
            }
            // Day of the week, "GMT", time offsets: they are ignored. Every form the JDK accepts
            // has the time in GMT.
        }
        if (day == -1 || month == -1 || year == -1 || hh == -1) {
            return Long.MIN_VALUE;
        }
        if (twoDigitYear) {
            // RFC 6265's rule, the same as the JDK's.
            if (year < 70) {
                year = year + 2000;
            } else {
                year = year + 1900;
            }
        }
        if (hh > 23 || mm > 59 || ss > 59 || day > daysInMonth(month, year)) {
            return Long.MIN_VALUE;
        }
        long days = daysFromCivil(year, month + 1, day);
        return ((days * 24 + hh) * 60 + mm) * 60000L + ss * 1000L;
    }

    private static int monthIndex(String tok) {
        if (tok.length() < 3) {
            return -1;
        }
        String p = tok.substring(0, 3).toLowerCase();
        int i = 0;
        while (i < MONTHS.length) {
            if (MONTHS[i].equals(p)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    private static int parseUnsigned(String tok) {
        if (tok.length() == 0) {
            return -1;
        }
        int v = 0;
        int i = 0;
        while (i < tok.length()) {
            char c = tok.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
            if (v > 999999) {
                return -1;
            }
            i = i + 1;
        }
        return v;
    }

    private static boolean isLeap(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }

    private static int daysInMonth(int month0, int year) {
        int[] d = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month0 == 1 && isLeap(year)) {
            return 29;
        }
        return d[month0];
    }

    // Days since 1970-01-01. The trick is to shift the year so that it starts in March: that way
    // the leap day falls at the end and the count of days per month becomes a formula with no
    // table.
    private static long daysFromCivil(int y, int m, int d) {
        long yy = y;
        yy = yy - (m <= 2 ? 1 : 0);
        long era = (yy >= 0 ? yy : yy - 399) / 400;
        long yoe = yy - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    // ---- text helpers ----------------------------------------------------------------------------

    private static boolean isToken(String value) {
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f || TSPECIALS.indexOf(c) != -1) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static String stripOffSurroundingQuote(String str) {
        if (str != null && str.length() > 2 && str.charAt(0) == '"'
                && str.charAt(str.length() - 1) == '"') {
            return str.substring(1, str.length() - 1);
        }
        if (str != null && str.length() > 2 && str.charAt(0) == '\''
                && str.charAt(str.length() - 1) == '\'') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static boolean equalsIgnoreCase(String s, String t) {
        if (s == t) {
            return true;
        }
        if (s != null && t != null) {
            return s.equalsIgnoreCase(t);
        }
        return false;
    }

    private static boolean startsWithIgnoreCase(String s, String start) {
        if (s == null || start == null) {
            return false;
        }
        return s.length() >= start.length()
                && start.equalsIgnoreCase(s.substring(0, start.length()));
    }

    // Splits on commas, but only on the ones outside quotes: the port list is written Port="80,81"
    // and that comma does not separate cookies.
    private static List<String> splitMultiCookies(String header) {
        List<String> cookies = new ArrayList<String>();
        int quoteCount = 0;
        int q = 0;
        int p = 0;
        while (p < header.length()) {
            char c = header.charAt(p);
            if (c == '"') {
                quoteCount = quoteCount + 1;
            }
            if (c == ',' && (quoteCount % 2 == 0)) {
                cookies.add(header.substring(q, p));
                q = p + 1;
            }
            p = p + 1;
        }
        cookies.add(header.substring(q));
        return cookies;
    }
}
