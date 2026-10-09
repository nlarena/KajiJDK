package jdk.jfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A set of settings with a name: what a {@code .jfc} file contains.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>A useful recording configures hundreds of settings --each event has its own-- and nobody
 * writes them by hand. The JDK brings two ready-made configurations, {@code default} and {@code
 * profile}, and the difference between them is how much they cost: the first one is meant to be
 * left on in production, the second one for an investigation with the machine dedicated.
 *
 * <p>Choosing between those two is the decision almost everybody takes, and this class is how they
 * are named.
 *
 * <h2>The format</h2>
 *
 * <p>A {@code .jfc} is XML: a root element with a name, a label and a provider, and inside it an
 * {@code <event>} per type with a {@code <setting>} per knob. {@link #getSettings} returns that
 * flattened to {@code "jdk.CPULoad#period" -> "1 s"}, which is the form in which the settings
 * travel through the rest of the API.
 *
 * <h2>State in this VM</h2>
 *
 * <p>{@link #create(Reader)} and {@link #create(Path)} <strong>really read and analyse</strong> a
 * {@code .jfc} file: the analyser is written here and returns a configuration with its settings
 * loaded. It can be used to inspect a configuration with no JFR.
 *
 * <p>{@link #getConfigurations} and {@link #getConfiguration(String)} are the ones that can give
 * nothing: they look in the {@code lib/jfr} directory of the installation of the JDK, which this
 * library does not have. The list comes out empty and the search by name fails saying that there is
 * none installed -- they do not invent an empty configuration with that name, which is what would
 * confuse the caller.
 *
 * @since 9
 */
public final class Configuration {

    private final String name;
    private final String label;
    private final String description;
    private final String provider;
    private final String contents;
    private final Map<String, String> settings;

    private Configuration(final String name, final String label, final String description,
            final String provider, final String contents, final Map<String, String> settings) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.provider = provider;
        this.contents = contents;
        this.settings = Collections.unmodifiableMap(settings);
    }

    /**
     * The settings, with the key {@code "event#setting"}.
     *
     * @return the settings
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * The name of the configuration.
     *
     * @return the name, or {@code null} if the file does not bring it
     */
    public String getName() {
        return name;
    }

    /**
     * The readable name.
     *
     * @return the label, or {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * The explanation of what this configuration is for.
     *
     * @return the description, or {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Who wrote it.
     *
     * @return the provider, or {@code null}
     */
    public String getProvider() {
        return provider;
    }

    /**
     * The original text of the file.
     *
     * <p>It is kept whole, without generating it again from the settings: that way a configuration
     * that is read and written again is left just as it was, with its comments and its order.
     *
     * @return the contents
     */
    public String getContents() {
        return contents;
    }

    /**
     * It reads a configuration from a file.
     *
     * @param path the file
     * @return the configuration
     * @throws IOException if it could not be read
     * @throws ParseException if the XML is malformed
     * @throws NullPointerException if it is {@code null}
     */
    public static Configuration create(final Path path) throws IOException, ParseException {
        Objects.requireNonNull(path, "path");
        final Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            return create(r);
        } finally {
            r.close();
        }
    }

    /**
     * It reads a configuration from a stream.
     *
     * @param reader where to read from
     * @return the configuration
     * @throws IOException if it could not be read
     * @throws ParseException if the XML is malformed
     * @throws NullPointerException if it is {@code null}
     */
    public static Configuration create(final Reader reader) throws IOException, ParseException {
        Objects.requireNonNull(reader, "reader");
        final StringBuilder sb = new StringBuilder();
        final BufferedReader br = reader instanceof BufferedReader
                ? (BufferedReader) reader : new BufferedReader(reader);
        final char[] buf = new char[4096];
        int n;
        while ((n = br.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        return parse(sb.toString());
    }

    /**
     * An installed configuration, by name.
     *
     * @param name the name, for example {@code "default"}
     * @return the configuration
     * @throws IOException if there are no installed configurations or it could not be read
     * @throws ParseException if the XML is malformed
     */
    public static Configuration getConfiguration(final String name)
            throws IOException, ParseException {
        Objects.requireNonNull(name, "name");
        throw new IOException(
                "there are no installed configurations: the lib/jfr directory of an installation "
                + "of the JDK brings them, and this library does not include it. To read one of "
                + "your own, use create(Path)");
    }

    /**
     * The installed configurations.
     *
     * <p>Empty in this library. It is a list and not a failure because the contract does not allow
     * one to warn, and because "there is none installed" is a legitimate answer the caller has to
     * be able to handle all the same.
     *
     * @return the list, empty
     */
    public static List<Configuration> getConfigurations() {
        return Collections.emptyList();
    }

    // ---- the analyser ----
    //
    // It is an XML analyser bounded to the shape of a .jfc and not a general analyser: the
    // alternative was dragging a complete parser along in order to read a file whose structure is
    // three elements. It recognises opening tags with attributes, text, closings and comments,
    // which is everything a .jfc uses.

    private static Configuration parse(final String xml) throws ParseException {
        String name = null;
        String label = null;
        String description = null;
        String provider = null;
        String currentEvent = null;
        String currentSetting = null;
        final Map<String, String> settings = new LinkedHashMap<String, String>();

        int i = 0;
        while (i < xml.length()) {
            final int open = xml.indexOf('<', i);
            if (open < 0) {
                break;
            }
            if (xml.startsWith("<!--", open)) {
                final int end = xml.indexOf("-->", open);
                if (end < 0) {
                    throw new ParseException("unclosed comment", open);
                }
                i = end + 3;
                continue;
            }
            if (xml.startsWith("<?", open) || xml.startsWith("<!", open)) {
                final int end = xml.indexOf('>', open);
                if (end < 0) {
                    throw new ParseException("unclosed declaration", open);
                }
                i = end + 1;
                continue;
            }
            final int close = xml.indexOf('>', open);
            if (close < 0) {
                throw new ParseException("unclosed tag", open);
            }
            String tag = xml.substring(open + 1, close).trim();
            final boolean empty = tag.endsWith("/");
            if (empty) {
                tag = tag.substring(0, tag.length() - 1).trim();
            }

            if (tag.startsWith("/")) {
                final String closed = tag.substring(1).trim();
                if ("setting".equals(closed)) {
                    currentSetting = null;
                } else if ("event".equals(closed)) {
                    currentEvent = null;
                }
                i = close + 1;
                continue;
            }

            final int sp = firstSpace(tag);
            final String elem = sp < 0 ? tag : tag.substring(0, sp);
            final Map<String, String> attrs =
                    sp < 0 ? Collections.<String, String>emptyMap() : attributes(tag.substring(sp));

            if ("configuration".equals(elem)) {
                name = attrs.get("name");
                label = attrs.get("label");
                description = attrs.get("description");
                provider = attrs.get("provider");
            } else if ("event".equals(elem)) {
                currentEvent = attrs.get("name");
            } else if ("setting".equals(elem)) {
                currentSetting = attrs.get("name");
                if (empty && currentEvent != null && currentSetting != null) {
                    // <setting name="x"/> with no text: the setting is left at the empty string,
                    // which is what the format means by that.
                    settings.put(currentEvent + "#" + currentSetting, "");
                    currentSetting = null;
                }
            }

            i = close + 1;

            if (currentSetting != null && !empty && currentEvent != null) {
                final int next = xml.indexOf('<', i);
                final String txt = next < 0 ? xml.substring(i) : xml.substring(i, next);
                settings.put(currentEvent + "#" + currentSetting, txt.trim());
            }
        }

        return new Configuration(name, label, description, provider, xml, settings);
    }

    private static int firstSpace(final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                return i;
            }
        }
        return -1;
    }

    /** Attributes {@code key="value"}, with single or double quotes. */
    private static Map<String, String> attributes(final String s) throws ParseException {
        final Map<String, String> out = new LinkedHashMap<String, String>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= s.length()) {
                break;
            }
            final int eq = s.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            final String key = s.substring(i, eq).trim();
            int j = eq + 1;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
                j++;
            }
            if (j >= s.length() || (s.charAt(j) != '"' && s.charAt(j) != '\'')) {
                throw new ParseException("attribute with no quotes: " + key, j);
            }
            final char quote = s.charAt(j);
            final int end = s.indexOf(quote, j + 1);
            if (end < 0) {
                throw new ParseException("unclosed attribute: " + key, j);
            }
            out.put(key, unescape(s.substring(j + 1, end)));
            i = end + 1;
        }
        return out;
    }

    /** The five entities XML defines; a {@code .jfc} uses no others. */
    private static String unescape(final String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }

    /** For the tests: it analyses a text without going through a file. */
    static Configuration fromText(final String xml) throws IOException, ParseException {
        return create(new StringReader(xml));
    }
}
