package com.sun.security.auth.login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * The JAAS configuration read from a text file.
 *
 * <h2>What a file of these says</h2>
 *
 * <p>Which login modules each application uses and how they are combined:
 *
 * <pre>{@code
 * MyApp {
 *     com.example.LdapModule  REQUIRED   server="ldap://x" port=389;
 *     com.example.LocalModule SUFFICIENT debug=true;
 * };
 * }</pre>
 *
 * <p>The point of the whole mechanism is in the word in the middle. {@code REQUIRED} has to
 * come out well and if it fails the asking goes on all the same -- so as not to reveal
 * <em>which</em> of the modules rejected it; {@code REQUISITE} cuts off there and then;
 * {@code SUFFICIENT} is enough by itself and finishes; {@code OPTIONAL} decides nothing.
 * Chaining them is what allows a policy to be put together without writing code, which is the
 * reason this is a file and not a class.
 *
 * <h2>Where the file comes from</h2>
 *
 * <p>From the {@link URI} that is passed to it, and if none is passed, from the property
 * {@code java.security.auth.login.config}. Finding nothing <strong>is not an error</strong>:
 * {@link #getAppConfigurationEntry} returns {@code null} for every application, which is what
 * is right when there is no configured policy.
 */
public class ConfigFile extends Configuration {

    private final URI url;
    private Map<String, List<AppConfigurationEntry>> configuration;

    /**
     * From the default place.
     *
     * @throws SecurityException if the file exists and cannot be read or is not understood
     */
    public ConfigFile() {
        this.url = null;
        refresh();
    }

    /**
     * From {@code uri}.
     *
     * @throws NullPointerException if {@code uri} is {@code null}
     * @throws SecurityException if it cannot be read or is not understood
     */
    public ConfigFile(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.url = uri;
        refresh();
    }

    /**
     * The modules configured for {@code applicationName}, or {@code null} if there are none.
     *
     * <p>{@code null} and not an empty array, and the difference matters: empty would be "it is
     * configured and uses no modules", which would let anybody in. {@code null} is "it is not
     * configured", and whoever calls has to decide what to do with that -- normally, to deny.
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
        if (applicationName == null) {
            return null;
        }
        List<AppConfigurationEntry> entries;
        synchronized (this) {
            entries = this.configuration.get(applicationName);
        }
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.toArray(new AppConfigurationEntry[entries.size()]);
    }

    /**
     * It rereads the file.
     *
     * <p>It is called from the two constructors: this object's state is the file, so building it
     * and rereading it are the same operation.
     */
    public void refresh() {
        Map<String, List<AppConfigurationEntry>> fresh =
                new HashMap<String, List<AppConfigurationEntry>>();
        String location = location();
        if (location != null) {
            try {
                load(location, fresh);
            } catch (IOException e) {
                throw new SecurityException("the login configuration could not be read: "
                        + location, e);
            }
        }
        synchronized (this) {
            this.configuration = fresh;
        }
    }

    private String location() {
        if (this.url != null) {
            return this.url.toString();
        }
        return System.getProperty("java.security.auth.login.config");
    }

    private void load(String location, Map<String, List<AppConfigurationEntry>> target)
            throws IOException {
        InputStream in;
        try {
            in = new URL(location).openStream();
        } catch (IOException e) {
            // It was not a URL: it is tried as a file path. The JDK accepts the two forms and
                        // telling them apart beforehand is guessing.
            in = new java.io.FileInputStream(location);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            parse(text(reader), target);
        } finally {
            reader.close();
        }
    }

    private String text(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            // The comments are taken out here and not in the parser: inside a value between quotes
                        // a `//` is text, but this format does not admit `//` inside quotes, so
                        // cutting by line is correct and much simpler.
            int cut = line.indexOf("//");
            if (cut >= 0) {
                line = line.substring(0, cut);
            }
            sb.append(line).append('\n');
            line = reader.readLine();
        }
        return sb.toString();
    }

    private void parse(String text, Map<String, List<AppConfigurationEntry>> target) {
        Lexer lex = new Lexer(text);
        String name = lex.word();
        while (name != null) {
            lex.expect("{");
            List<AppConfigurationEntry> entries = new ArrayList<AppConfigurationEntry>();
            String module = lex.word();
            while (module != null && !module.equals("}")) {
                String flag = lex.word();
                Map<String, Object> options = new HashMap<String, Object>();
                String t = lex.word();
                while (t != null && !t.equals(";")) {
                    int equalsAt = t.indexOf('=');
                    if (equalsAt > 0) {
                        options.put(t.substring(0, equalsAt), unquoted(t.substring(equalsAt + 1)));
                    }
                    t = lex.word();
                }
                entries.add(new AppConfigurationEntry(module, flagOf(flag), options));
                module = lex.word();
            }
            lex.expect(";");
            target.put(name, entries);
            name = lex.word();
        }
    }

    private static String unquoted(String v) {
        if (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static AppConfigurationEntry.LoginModuleControlFlag flagOf(String s) {
        if (s == null) {
            throw new SecurityException("the control flag is missing");
        }
        String b = s.toUpperCase();
        if (b.equals("REQUIRED")) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        }
        if (b.equals("REQUISITE")) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        }
        if (b.equals("SUFFICIENT")) {
            return AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        }
        if (b.equals("OPTIONAL")) {
            return AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        }
        throw new SecurityException("unknown control flag: " + s);
    }

    /**
     * The format's lexicon: words separated by spaces, with {@code {}} and {@code ;} as symbols
     * of their own and quotes that group.
     */
    private static final class Lexer {

        private final String text;
        private int i;

        Lexer(String text) {
            this.text = text;
        }

        void expect(String symbol) {
            String t = word();
            if (t == null || !t.equals(symbol)) {
                throw new SecurityException("expected " + symbol + " and got "
                        + String.valueOf(t));
            }
        }

        String word() {
            while (this.i < this.text.length()
                    && Character.isWhitespace(this.text.charAt(this.i))) {
                this.i++;
            }
            if (this.i >= this.text.length()) {
                return null;
            }
            char c = this.text.charAt(this.i);
            if (c == '{' || c == '}' || c == ';') {
                this.i++;
                return String.valueOf(c);
            }
            int from = this.i;
            boolean inQuotes = false;
            while (this.i < this.text.length()) {
                char d = this.text.charAt(this.i);
                if (d == '"') {
                    inQuotes = !inQuotes;
                } else if (!inQuotes
                        && (Character.isWhitespace(d) || d == '{' || d == '}' || d == ';')) {
                    break;
                }
                this.i++;
            }
            return this.text.substring(from, this.i);
        }
    }
}
