package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The link between a URL and the resource it names, before and after fetching it.
//
// The class reads better split in two halves, because they are two different things stuck together:
//
//  1. **Configuration**: everything set BEFORE `connect()` --timeouts, `doInput`/`doOutput`, cache,
//     `If-Modified-Since`, request properties. It is a configuration object, and that is why all
//     those setters throw `IllegalStateException` if it has already connected: changing them
//     afterwards would have no effect, and failing is better than ignoring silently.
//
//  2. **Reading the result**: `getHeaderField*`, `getContentType`, `getInputStream`. It depends on
//     somebody having talked to the other side.
//
// ===========================================================================================
// WHAT GOES IN AND WHAT DOES NOT IN KajiJDK
// ===========================================================================================
//
// The **whole** class goes in, and it is not a concession: `URLConnection` is **abstract** and its
// only abstract method is `connect()`. Everything else is base implementations the JDK defines, and
// the base definitions are honest to reproduce because they promise no data:
//
//  - `getHeaderFields()` returns the empty map, `getHeaderField(String)` returns null. It is what
//    the JDK does: a base connection has no headers, and the subclasses that do have them override
//    them. `getContentType`, `getDate`, `getExpiration` and company come out of that.
//  - `getInputStream()` and `getOutputStream()` throw `UnknownServiceException` with the same text
//    as the JDK ("protocol doesn't support input"/"output"). **That is not a method that lies**: it
//    is literally the method's documented contract in the base class, not a stub. Whoever writes a
//    subclass that does know how to read overrides it and it works.
//
// What does NOT go in: nothing. The one piece left limping is `ContentHandler` resolution, which in
// the JDK sweeps packages looking for handlers by MIME type; here it only consults the factory the
// application installs with `setContentHandlerFactory`. With no factory, `getContent()` ends in
// `getInputStream()`'s `UnknownServiceException`, which is exactly where it ends in the JDK with a
// base connection.
//
// `guessContentTypeFromName` and `guessContentTypeFromStream` are pure computation --a table of
// extensions and some magic numbers-- and they are complete.
public abstract class URLConnection {

    /** This link's URL. */
    protected URL url;

    /** Whether the resource is going to be read. True by default. */
    protected boolean doInput = true;

    /** Whether the resource is going to be written to. False by default. */
    protected boolean doOutput = false;

    /** Whether the user may be asked things (a password dialog, for instance). */
    protected boolean allowUserInteraction = defaultAllowUserInteraction;

    /** Whether a cached copy may be used. */
    protected boolean useCaches = true;

    /** Fetch it only if it changed after this instant; 0 switches it off. */
    protected long ifModifiedSince = 0;

    /** Whether the other side has already been talked to. */
    protected boolean connected = false;

    private static boolean defaultAllowUserInteraction = false;
    private static volatile FileNameMap fileNameMap;
    private static ContentHandlerFactory contentHandlerFactory;
    private static final Map<String, Boolean> defaultUseCachesByProtocol =
            new HashMap<String, Boolean>();

    private int connectTimeout;
    private int readTimeout;

    // They are kept in the order they were added and in the case they were written in, but they are
    // looked up case-insensitively: it is what the JDK does, and it reflects that in HTTP a
    // header's name is case-insensitive but is sent as it was written.
    private final Map<String, List<String>> requestProperties =
            new LinkedHashMap<String, List<String>>();

    /**
     * A link to {@code url}, unconnected.
     *
     * <p>It is {@code protected} because nobody builds a `URLConnection` by hand: it is asked of a
     * `URL`, which chooses the subclass by protocol.
     */
    protected URLConnection(URL url) {
        this.url = url;
    }

    /**
     * Talks to the other side.
     *
     * <p>Abstract on purpose and from the JDK: it is THE operation that depends on the protocol,
     * and there is no base implementation that would make sense.
     */
    public abstract void connect() throws IOException;

    // ---- configuration before connecting ----

    /** Milliseconds to wait for the connection to be established; 0 is "forever". */
    public void setConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.connectTimeout = timeout;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    /** Milliseconds a read waits for data; 0 is "forever". */
    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.readTimeout = timeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public URL getURL() {
        return this.url;
    }

    private void checkNotConnected() {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
    }

    public void setDoInput(boolean doinput) {
        this.checkNotConnected();
        this.doInput = doinput;
    }

    public boolean getDoInput() {
        return this.doInput;
    }

    public void setDoOutput(boolean dooutput) {
        this.checkNotConnected();
        this.doOutput = dooutput;
    }

    public boolean getDoOutput() {
        return this.doOutput;
    }

    public void setAllowUserInteraction(boolean allowuserinteraction) {
        this.checkNotConnected();
        this.allowUserInteraction = allowuserinteraction;
    }

    public boolean getAllowUserInteraction() {
        return this.allowUserInteraction;
    }

    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    public void setUseCaches(boolean usecaches) {
        this.checkNotConnected();
        this.useCaches = usecaches;
    }

    public boolean getUseCaches() {
        return this.useCaches;
    }

    public void setIfModifiedSince(long ifmodifiedsince) {
        this.checkNotConnected();
        this.ifModifiedSince = ifmodifiedsince;
    }

    public long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    /** The value `useCaches` takes in new connections of this one's protocol. */
    public boolean getDefaultUseCaches() {
        return getDefaultUseCaches(this.urlProtocol());
    }

    public void setDefaultUseCaches(boolean defaultusecaches) {
        setDefaultUseCaches(this.urlProtocol(), defaultusecaches);
    }

    /**
     * The per-protocol cache default. Absent means true, which is the global default.
     *
     * <p>The protocol is case-insensitive: "HTTP" and "http" are the same.
     */
    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {
        synchronized (defaultUseCachesByProtocol) {
            defaultUseCachesByProtocol.put(protocol.toLowerCase(), Boolean.valueOf(defaultVal));
        }
    }

    public static boolean getDefaultUseCaches(String protocol) {
        synchronized (defaultUseCachesByProtocol) {
            Boolean v = defaultUseCachesByProtocol.get(protocol.toLowerCase());
            return v == null ? true : v.booleanValue();
        }
    }

    private String urlProtocol() {
        return this.url == null ? "" : String.valueOf(this.url.getProtocol());
    }

    // ---- the request's properties ----

    /** Sets the header {@code key}, overwriting whatever was there. */
    public void setRequestProperty(String key, String value) {
        this.checkNotConnected();
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        String existing = this.existingKey(key);
        if (existing != null) {
            this.requestProperties.remove(existing);
        }
        List<String> vals = new ArrayList<String>();
        vals.add(value);
        this.requestProperties.put(key, vals);
    }

    /**
     * Adds one more value to the header {@code key}, without overwriting the ones already there.
     */
    public void addRequestProperty(String key, String value) {
        this.checkNotConnected();
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        String existing = this.existingKey(key);
        if (existing == null) {
            List<String> vals = new ArrayList<String>();
            vals.add(value);
            this.requestProperties.put(key, vals);
        } else {
            this.requestProperties.get(existing).add(value);
        }
    }

    /**
     * The LAST value of {@code key}, or null. That it is the last and not the first is the JDK's.
     */
    public String getRequestProperty(String key) {
        this.checkNotConnected();
        String existing = this.existingKey(key);
        if (existing == null) {
            return null;
        }
        List<String> vals = this.requestProperties.get(existing);
        return vals.isEmpty() ? null : vals.get(vals.size() - 1);
    }

    /** Every header of the request, read-only. */
    public Map<String, List<String>> getRequestProperties() {
        this.checkNotConnected();
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        Iterator<String> it = this.requestProperties.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            copy.put(k, Collections.unmodifiableList(
                    new ArrayList<String>(this.requestProperties.get(k))));
        }
        return Collections.unmodifiableMap(copy);
    }

    // The already stored key matching `key` case-insensitively, or null.
    private String existingKey(String key) {
        if (key == null) {
            return null;
        }
        Iterator<String> it = this.requestProperties.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equalsIgnoreCase(key)) {
                return k;
            }
        }
        return null;
    }

    /**
     * It does nothing, and that is what the JDK does since it was deprecated in 1.3.
     *
     * @deprecated per-connection defaults are set with {@link #setRequestProperty}.
     */
    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }

    /**
     * Always null, as in the JDK.
     *
     * @deprecated see {@link #setDefaultRequestProperty}.
     */
    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }

    // ---- reading the response ----

    /**
     * Every header of the response. In the base class, the empty map.
     *
     * <p>It is the method almost every `getX` below hangs off, and that is why a subclass that
     * overrides it well inherits `getContentType`, `getDate`, `getLastModified` and the rest free.
     */
    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    /** The value of the {@code name} header, or null. */
    public String getHeaderField(String name) {
        return null;
    }

    /** The name of header number {@code n}, or null if there are not that many. */
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /** The value of header number {@code n}, or null if there are not that many. */
    public String getHeaderField(int n) {
        return null;
    }

    /**
     * The {@code name} header as an integer, or {@code Default} if it is missing or not a number.
     */
    public int getHeaderFieldInt(String name, int Default) {
        String value = this.getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return Default;
        }
    }

    /** Like {@link #getHeaderFieldInt}, in 64 bits. */
    public long getHeaderFieldLong(String name, long Default) {
        String value = this.getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return Default;
        }
    }

    /**
     * The header {@code name} as an instant, in milliseconds since 1970.
     *
     * <p>It accepts the three formats the protocol allows (RFC 9110 5.6.7): the preferred one
     * --`Sun, 06 Nov 1994 08:49:37 GMT`-- and the two historical ones an old server may still send.
     * It is pure computation over the string: there is no time zone to consult, because the format
     * **requires** GMT.
     */
    public long getHeaderFieldDate(String name, long Default) {
        String value = this.getHeaderField(name);
        long t = parseHttpDate(value);
        return t == Long.MIN_VALUE ? Default : t;
    }

    /**
     * The body's size, or -1 if it is not known. It overflows from 2 GiB on: use the long version.
     */
    public int getContentLength() {
        long l = this.getContentLengthLong();
        return l > Integer.MAX_VALUE ? -1 : (int) l;
    }

    /** The body's size in 64 bits, or -1 if it is not known. */
    public long getContentLengthLong() {
        return this.getHeaderFieldLong("content-length", -1);
    }

    public String getContentType() {
        return this.getHeaderField("content-type");
    }

    public String getContentEncoding() {
        return this.getHeaderField("content-encoding");
    }

    /** When the resource expires, or 0 if it is not known. */
    public long getExpiration() {
        return this.getHeaderFieldDate("expires", 0);
    }

    /** The message's date, or 0 if it is not known. */
    public long getDate() {
        return this.getHeaderFieldDate("date", 0);
    }

    /** When the resource was last modified, or 0 if it is not known. */
    public long getLastModified() {
        return this.getHeaderFieldDate("last-modified", 0);
    }

    /**
     * The permission needed to make this connection.
     *
     * <p>The base class returns `AllPermission`, just like the JDK: it does not know what it is
     * going to connect to, so it cannot ask for anything finer. The subclasses tighten it.
     */
    public java.security.Permission getPermission() throws IOException {
        return new java.security.AllPermission();
    }

    /**
     * The body as a stream of bytes.
     *
     * <p>In the base class it throws: a generic protocol does not know how to read. Overriding this
     * is half of a subclass's job.
     *
     * @throws UnknownServiceException always, in the base class
     */
    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }

    /**
     * Where to write the request's body.
     *
     * @throws UnknownServiceException always, in the base class
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }

    /**
     * The body already interpreted by the {@link ContentHandler} matching its MIME type.
     *
     * <p>It opens the stream first --and that is why in the base class it ends in
     * `getInputStream`'s `UnknownServiceException`, exactly as in the JDK--; only then does it look
     * for a handler.
     *
     * <p>The handler lookup is the one thing that differs: the JDK sweeps packages looking for a
     * class by naming convention; here it only consults the factory the application installed with
     * {@link #setContentHandlerFactory}. With no factory there is no handler, and that is said by
     * throwing, not by returning something invented.
     */
    public Object getContent() throws IOException {
        this.getInputStream();
        return this.handler().getContent(this);
    }

    /**
     * Like {@link #getContent()}, but it returns null if the object is of none of {@code classes}.
     */
    public Object getContent(Class<?>[] classes) throws IOException {
        this.getInputStream();
        return this.handler().getContent(this, classes);
    }

    private ContentHandler handler() throws UnknownServiceException {
        String type = this.getContentType();
        if (type != null) {
            int semicolon = type.indexOf(';');
            if (semicolon != -1) {
                type = type.substring(0, semicolon).trim();
            }
        }
        if (type == null || type.length() == 0) {
            throw new UnknownServiceException("no content-type");
        }
        ContentHandlerFactory f = contentHandlerFactory;
        ContentHandler h = f == null ? null : f.createContentHandler(type);
        if (h == null) {
            throw new UnknownServiceException("no content handler for " + type);
        }
        return h;
    }

    /**
     * Installs the content handler factory. Once only per VM.
     *
     * @throws Error if one had already been installed
     */
    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if (contentHandlerFactory != null) {
            throw new Error("factory already defined");
        }
        contentHandlerFactory = fac;
    }

    // ---- guessing the type ----

    /** The extension-to-MIME-type table {@link #guessContentTypeFromName} uses. */
    public static FileNameMap getFileNameMap() {
        FileNameMap m = fileNameMap;
        if (m == null) {
            synchronized (URLConnection.class) {
                if (fileNameMap == null) {
                    fileNameMap = new ExtensionTable();
                }
                m = fileNameMap;
            }
        }
        return m;
    }

    public static void setFileNameMap(FileNameMap map) {
        fileNameMap = map;
    }

    /** The MIME type {@code fname}'s extension suggests, or null. */
    public static String guessContentTypeFromName(String fname) {
        return getFileNameMap().getContentTypeFor(fname);
    }

    /**
     * The MIME type the first bytes of {@code is} suggest, or null.
     *
     * <p>It looks and gives the stream back as it was: it uses `mark`/`reset`, and if the stream
     * does not support marks it returns null instead of consuming it. Consuming bytes from a stream
     * somebody is going to read afterwards would be an invisible side effect.
     */
    public static String guessContentTypeFromStream(InputStream is) throws IOException {
        if (!is.markSupported()) {
            return null;
        }
        is.mark(16);
        int[] b = new int[16];
        int readSoFar = 0;
        while (readSoFar < 16) {
            int c = is.read();
            if (c == -1) {
                break;
            }
            b[readSoFar] = c;
            readSoFar = readSoFar + 1;
        }
        is.reset();
        return byMagicNumber(b, readSoFar);
    }

    private static boolean startsWith(int[] b, int n, int[] magic) {
        if (n < magic.length) {
            return false;
        }
        int i = 0;
        while (i < magic.length) {
            if (b[i] != magic[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static boolean startsWithText(int[] b, int n, String s) {
        return startsWith(b, n, toBytes(s));
    }

    private static int[] toBytes(String s) {
        int[] out = new int[s.length()];
        int i = 0;
        while (i < s.length()) {
            out[i] = s.charAt(i) & 0xff;
            i = i + 1;
        }
        return out;
    }

    // The JDK's magic-number table, transcribed **whole and with no additions**.
    //
    // The "no additions" part is the one that matters. This method's first version also recognized
    // `%!` as PostScript and `.sd` as audio -- two real signatures, which anyone would say add
    // something. But the JDK does **not** recognize them, so returning a type there is giving a
    // different answer from the JDK's over the same bytes, and that is not "more complete": it is
    // incompatible. The behaviour test caught it.
    //
    // For the same reason, JPEG asks for more than `FF D8 FF`: the JDK looks at the fourth byte and
    // only accepts E0, EE, or E1 followed by "Exif\0". An `FF D8 FF DB` --which is a perfectly
    // valid JPEG-- gives it null, and it does here too.
    private static String byMagicNumber(int[] b, int n) {
        if (startsWith(b, n, new int[] {0xCA, 0xFE, 0xBA, 0xBE})) {
            return "application/java-vm";
        }
        if (startsWith(b, n, new int[] {0xAC, 0xED})) {
            return "application/x-java-serialized-object";
        }
        if (n >= 1 && b[0] == '<') {
            // Anything starting with `<!` is HTML to the JDK -- not just `<!DOCTYPE`. That is why
            // this check goes before the `<?xml` one, and why the cases of "DOCTYPE" do not have to
            // be enumerated.
            if (n >= 2 && b[1] == '!') {
                return "text/html";
            }
            if (startsWithText(b, n, "<html") || startsWithText(b, n, "<head")
                    || startsWithText(b, n, "<body") || startsWithText(b, n, "<HTML")
                    || startsWithText(b, n, "<HEAD") || startsWithText(b, n, "<BODY")) {
                return "text/html";
            }
            if (startsWithText(b, n, "<?xml ")) {
                return "application/xml";
            }
        }
        if (startsWithText(b, n, "! XPM2")) {
            return "image/x-pixmap";
        }
        if (startsWith(b, n, new int[] {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (startsWith(b, n, new int[] {0xFF, 0xD8, 0xFF}) && n >= 4) {
            if (b[3] == 0xE0 || b[3] == 0xEE) {
                return "image/jpeg";
            }
            if (b[3] == 0xE1 && n >= 11 && b[6] == 'E' && b[7] == 'x' && b[8] == 'i'
                    && b[9] == 'f' && b[10] == 0) {
                return "image/jpeg";
            }
        }
        if (startsWithText(b, n, "GIF8")) {
            return "image/gif";
        }
        if (startsWithText(b, n, "#def")) {
            return "image/x-bitmap";
        }
        if (startsWithText(b, n, ".snd")) {
            return "audio/basic";
        }
        if (startsWithText(b, n, "dns.")) {
            return "audio/basic";
        }
        if (startsWithText(b, n, "MThd")) {
            return "audio/midi";
        }
        if (startsWithText(b, n, "RIFF")) {
            return "audio/x-wav";
        }
        if (startsWith(b, n, new int[] {0xF7, 0x02})) {
            return "application/x-dvi";
        }
        return null;
    }

    /**
     * {@code getClass().getName() + ":" + url}, as in the JDK.
     */
    @Override
    public String toString() {
        return this.getClass().getName() + ":" + this.url;
    }

    // ===========================================================================================
    // The HTTP date
    // ===========================================================================================

    // It returns `Long.MIN_VALUE` --and not an exception-- when it does not understand, because the
    // only caller already has a default value to give and an exception there would only add a
    // try/catch. No real instant falls on `Long.MIN_VALUE`.
    static long parseHttpDate(String s) {
        if (s == null) {
            return Long.MIN_VALUE;
        }
        String v = s.trim();
        int comma = v.indexOf(',');
        if (comma != -1) {
            v = v.substring(comma + 1).trim();
        }
        // Three possible forms are left: "06 Nov 1994 08:49:37 GMT" (RFC 1123, the preferred one)
        //   "06-Nov-94 08:49:37 GMT" (RFC 850, historical; the two-digit year is its problem) "Nov
        //   6 08:49:37 1994" (asctime, with no comma, which is why the trim above left it alone)
        v = v.replace('-', ' ');
        String[] p = split(v);
        if (p.length >= 4 && isNumber(p[0])) {
            int day = (int) parseLong0(p[0]);
            int month = monthByName(p[1]);
            long year = parseLong0(p[2]);
            if (month < 0 || year == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            if (year < 100) {
                // RFC 850: two digits. The JDK's convention is the 1970-2069 window.
                year = year < 70 ? year + 2000 : year + 1900;
            }
            return assemble(year, month, day, p[3]);
        }
        if (p.length >= 4 && monthByName(p[0]) >= 0) {
            int month = monthByName(p[0]);
            int day = (int) parseLong0(p[1]);
            long year = parseLong0(p[3]);
            if (year == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return assemble(year, month, day, p[2]);
        }
        return Long.MIN_VALUE;
    }

    private static long assemble(long year, int month, int day, String time) {
        String[] hms = splitOn(time, ':');
        if (hms.length < 2) {
            return Long.MIN_VALUE;
        }
        long h = parseLong0(hms[0]);
        long m = parseLong0(hms[1]);
        long sec = hms.length > 2 ? parseLong0(hms[2]) : 0;
        if (h == Long.MIN_VALUE || m == Long.MIN_VALUE || sec == Long.MIN_VALUE || day < 1) {
            return Long.MIN_VALUE;
        }
        return (daysSince1970(year, month, day) * 86400L + h * 3600L + m * 60L + sec) * 1000L;
    }

    // Howard Hinnant's algorithm: it counts the running days of a proleptic Gregorian calendar with
    // no tables and no loops. This is used and not a `Calendar` because an HTTP date is always GMT:
    // there is no zone and no summer time to consult, and bringing in a `Calendar` would bring
    // both.
    private static long daysSince1970(long year, int month, int day) {
        long y = year;
        long m = month + 1;
        y = y - (m <= 2 ? 1 : 0);
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    private static final String[] MONTHS = {
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };

    private static int monthByName(String s) {
        if (s == null || s.length() < 3) {
            return -1;
        }
        String tres = s.substring(0, 3).toLowerCase();
        int i = 0;
        while (i < MONTHS.length) {
            if (MONTHS[i].equals(tres)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    private static boolean isNumber(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static long parseLong0(String s) {
        return isNumber(s) ? Long.parseLong(s) : Long.MIN_VALUE;
    }

    private static String[] split(String s) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        StringBuilder cur = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur = new StringBuilder();
                }
            } else {
                cur.append(c);
            }
            i = i + 1;
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    private static String[] splitOn(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start <= s.length()) {
            int i = s.indexOf(sep, start);
            if (i == -1) {
                out.add(s.substring(start));
                start = s.length() + 1;
            } else {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    // The default table. The JDK reads `content-types.properties` from its own installation; here
    // the table is written out, with the same answers for the common extensions (verified against
    // the real JDK). An extension that is not there gives null, which is what is right: "I do not
    // know".
    private static class ExtensionTable implements FileNameMap {

        public String getContentTypeFor(String fileName) {
            if (fileName == null) {
                return null;
            }
            int dot = fileName.lastIndexOf('.');
            if (dot == -1 || dot == fileName.length() - 1) {
                return null;
            }
            String ext = fileName.substring(dot + 1).toLowerCase();
            if (ext.equals("html") || ext.equals("htm")) {
                return "text/html";
            }
            if (ext.equals("txt") || ext.equals("text")) {
                return "text/plain";
            }
            if (ext.equals("css")) {
                return "text/css";
            }
            if (ext.equals("js")) {
                return "text/javascript";
            }
            if (ext.equals("json")) {
                return "application/json";
            }
            if (ext.equals("xml")) {
                return "application/xml";
            }
            if (ext.equals("gif")) {
                return "image/gif";
            }
            if (ext.equals("png")) {
                return "image/png";
            }
            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("jpe")) {
                return "image/jpeg";
            }
            if (ext.equals("zip")) {
                return "application/zip";
            }
            if (ext.equals("gz")) {
                return "application/x-gzip";
            }
            if (ext.equals("pdf")) {
                return "application/pdf";
            }
            if (ext.equals("jar")) {
                return "application/java-archive";
            }
            if (ext.equals("class")) {
                return "application/java-vm";
            }
            if (ext.equals("java")) {
                return "text/plain";
            }
            return null;
        }
    }
}
