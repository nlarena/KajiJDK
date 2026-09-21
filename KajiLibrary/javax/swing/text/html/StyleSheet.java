package javax.swing.text.html;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleContext$SmallAttributeSet;
import javax.swing.text.View;

/**
 * A style sheet: CSS rules that give attributes to the elements.
 *
 * <h2>It is a {@link StyleContext}, and that is no accident</h2>
 *
 * <p>A style context already knows how to share attribute sets and look them up by name. A style
 * sheet needs exactly that, plus the CSS part: reading rules, choosing which one applies to an
 * element, and translating the old HTML attributes (<code>bgcolor</code>, <code>align</code>) to
 * CSS properties.
 *
 * <h2>How the rule is chosen</h2>
 *
 * <p>A rule is kept under a name that is the path of elements that selects it, for instance
 * <code>html body p</code>. For a document element its path is built and every rule that is a
 * suffix of it is looked up; those that appear are joined, and the most specific one wins.
 *
 * <h2>How far this implementation goes</h2>
 *
 * <p>It reads rules, joins them, resolves colours and letter sizes, and translates the HTML
 * attributes. What it does not do is the full box model: {@link BoxPainter} and
 * {@link ListPainter} compute margins and draw bullets, but not styled borders or background
 * images. They are the parts that only show with a screen, and this library has nothing to
 * compare them against yet.
 */
public class StyleSheet extends StyleContext {

    private Vector<StyleSheet> linkedStyleSheets;
    private URL base;
    private int baseFontSize = 4;

    /** HTML's letter sizes, from 1 to 7. */
    private static final int[] sizeMapDefault = {8, 10, 12, 14, 18, 24, 36};

    private static final Hashtable<String, Color> colors = new Hashtable<String, Color>();

    /** An empty sheet. */
    public StyleSheet() {
        super();
    }

    /**
     * The rule that applies to that element with that tag.
     *
     * <p>It builds the path from the root and joins everything that matches; see the class note.
     */
    public Style getRule(HTML.Tag t, Element e) {
        String path = pathOf(t, e);
        return getRule(path);
    }

    /** An element's path, from the root downwards, for looking up rules. */
    private String pathOf(HTML.Tag t, Element e) {
        Vector<String> parts = new Vector<String>();
        parts.addElement(t.toString());
        for (Element p = (e == null) ? null : e.getParentElement(); p != null;
                p = p.getParentElement()) {
            AttributeSet a = p.getAttributes();
            Object name = a.getAttribute(StyleConstants.NameAttribute);
            if (name instanceof HTML.Tag) {
                parts.insertElementAt(name.toString(), 0);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(parts.elementAt(i));
        }
        return sb.toString();
    }

    /**
     * The rule with that selector.
     *
     * <p>It joins the exact rule and every one that is a suffix of the path: for <code>html body
     * p</code>, <code>body p</code> and <code>p</code> come in too. They are applied from the least
     * specific to the most specific, so the longest one wins.
     */
    public Style getRule(String selector) {
        selector = cleanSelector(selector);
        String[] parts = splitPath(selector);
        Vector<Style> found = new Vector<Style>();
        // From the longest to the shortest: the first one that answers wins.
        for (int from = 0; from < parts.length; from++) {
            StringBuilder sb = new StringBuilder();
            for (int i = from; i < parts.length; i++) {
                if (i > from) {
                    sb.append(' ');
                }
                sb.append(parts[i]);
            }
            Style s = getStyle(sb.toString());
            if (s != null) {
                found.addElement(s);
            }
        }
        Style[] arr = new Style[found.size()];
        found.copyInto(arr);
        return new ResolvedStyle(selector, arr);
    }

    /**
     * The style {@link #getRule} returns: a view over the rules that matched.
     *
     * <h2>It does not copy, it multiplexes</h2>
     *
     * <p>It keeps the rules that matched, from the most specific to the least, and answers each
     * query by walking them in that order. The first one that has the attribute wins.
     *
     * <p>Multiplexing instead of copying has a consequence that shows: an attribute that is in two
     * rules appears twice when enumerating, even though {@code getAttribute} always returns the one
     * from the most specific. It is the JDK's form and it is kept because
     * {@code getAttributeCount} is public and somebody may be counting.
     *
     * <p>Neither is it registered in the sheet. If it were, every query with a new path would leave
     * a style kept for ever, and a long page would pile them up with nobody to delete them.
     */
    static final class ResolvedStyle implements Style {

        private final String name;
        private final Style[] reglas;

        ResolvedStyle(String name, Style[] reglas) {
            this.name = name;
            this.reglas = reglas;
        }

        public String getName() {
            return name;
        }

        public void addChangeListener(javax.swing.event.ChangeListener l) {
        }

        public void removeChangeListener(javax.swing.event.ChangeListener l) {
        }

        public int getAttributeCount() {
            int n = 0;
            for (int i = 0; i < reglas.length; i++) {
                n = n + reglas[i].getAttributeCount();
            }
            return n;
        }

        public boolean isDefined(Object attrName) {
            for (int i = 0; i < reglas.length; i++) {
                if (reglas[i].isDefined(attrName)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEqual(AttributeSet attr) {
            return ((getAttributeCount() == attr.getAttributeCount())
                    && containsAttributes(attr));
        }

        public AttributeSet copyAttributes() {
            SimpleAttributeSet copy = new SimpleAttributeSet();
            copy.addAttributes(this);
            return copy;
        }

        public Object getAttribute(Object key) {
            for (int i = 0; i < reglas.length; i++) {
                Object v = reglas[i].getAttribute(key);
                if (v != null) {
                    return v;
                }
            }
            return null;
        }

        public Enumeration<?> getAttributeNames() {
            Vector<Object> all = new Vector<Object>();
            for (int i = 0; i < reglas.length; i++) {
                Enumeration<?> e = reglas[i].getAttributeNames();
                while (e.hasMoreElements()) {
                    all.addElement(e.nextElement());
                }
            }
            return all.elements();
        }

        public boolean containsAttribute(Object name, Object value) {
            Object v = getAttribute(name);
            return (v != null && v.equals(value));
        }

        public boolean containsAttributes(AttributeSet attrs) {
            Enumeration<?> e = attrs.getAttributeNames();
            while (e.hasMoreElements()) {
                Object name = e.nextElement();
                if (!containsAttribute(name, attrs.getAttribute(name))) {
                    return false;
                }
            }
            return true;
        }

        public AttributeSet getResolveParent() {
            return null;
        }

        public void addAttribute(Object name, Object value) {
        }

        public void addAttributes(AttributeSet attributes) {
        }

        public void removeAttribute(Object name) {
        }

        public void removeAttributes(Enumeration<?> names) {
        }

        public void removeAttributes(AttributeSet attributes) {
        }

        public void setResolveParent(AttributeSet parent) {
        }
    }

    private static String cleanSelector(String s) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s.toLowerCase(java.util.Locale.ROOT),
                " \t\n\r\f");
        while (st.hasMoreTokens()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(st.nextToken());
        }
        return sb.toString();
    }

    private static String[] splitPath(String s) {
        StringTokenizer st = new StringTokenizer(s, " ");
        String[] out = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            out[i] = st.nextToken();
        }
        return out;
    }

    /**
     * It adds a rule written in CSS.
     *
     * <p>A selector with commas defines several equal rules, and that is why it is split: writing
     * <code>h1, h2 { color: red }</code> is the same as writing the two separately.
     */
    public void addRule(String rule) {
        if (rule == null) {
            return;
        }
        String text = withoutComments(rule);
        int i = 0;
        while (i < text.length()) {
            int opens = text.indexOf('{', i);
            if (opens < 0) {
                break;
            }
            int closes = text.indexOf('}', opens);
            if (closes < 0) {
                break;
            }
            String selectors = text.substring(i, opens).trim();
            AttributeSet decl = getDeclaration(text.substring(opens + 1, closes));
            StringTokenizer st = new StringTokenizer(selectors, ",");
            while (st.hasMoreTokens()) {
                String sel = cleanSelector(st.nextToken());
                if (sel.length() > 0) {
                    Style s = getStyle(sel);
                    if (s == null) {
                        s = addStyle(sel, null);
                    }
                    s.addAttributes(decl);
                }
            }
            i = closes + 1;
        }
    }

    /** It removes the comments; a rule inside a comment does not count. */
    private static String withoutComments(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int opens = s.indexOf("/*", i);
            if (opens < 0) {
                sb.append(s.substring(i));
                break;
            }
            sb.append(s, i, opens);
            int closes = s.indexOf("*/", opens + 2);
            if (closes < 0) {
                break;
            }
            i = closes + 2;
        }
        return sb.toString();
    }

    /** The attributes that text declares, without the braces or the selector. */
    public AttributeSet getDeclaration(String decl) {
        MutableAttributeSet a = new SimpleAttributeSet();
        if (decl == null) {
            return a;
        }
        StringTokenizer st = new StringTokenizer(withoutComments(decl), ";");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int colon = pair.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = pair.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
            String value = pair.substring(colon + 1).trim();
            CSS.Attribute key = CSS.getAttribute(name);
            if (key != null && value.length() > 0) {
                addCSSAttribute(a, key, value);
            }
        }
        return a;
    }

    /** It reads rules from a text; the address serves to resolve those that are relative. */
    public void loadRules(Reader in, URL ref) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        addRule(sb.toString());
    }

    /** The attributes that fall to that view, already resolved. */
    public AttributeSet getViewAttributes(View v) {
        return v.getElement().getAttributes();
    }

    public void removeStyle(String nm) {
        super.removeStyle(nm);
    }

    /**
     * It adds another sheet underneath this one.
     *
     * <p>The added sheets are consulted after the rules of its own, in the order they were added.
     * It is what allows having a sheet of the program's and on top of it the page's.
     */
    public void addStyleSheet(StyleSheet ss) {
        synchronized (this) {
            if (linkedStyleSheets == null) {
                linkedStyleSheets = new Vector<StyleSheet>();
            }
            if (!linkedStyleSheets.contains(ss)) {
                linkedStyleSheets.insertElementAt(ss, 0);
            }
        }
    }

    /**
     * It removes an added sheet.
     *
     * <p>When the last one goes, the list becomes null again and {@link #getStyleSheets} answers
     * null again, not an empty array. It is the same answer as before adding the first one: the
     * state after removing everything is the initial state, and not one that looks like it.
     */
    public void removeStyleSheet(StyleSheet ss) {
        synchronized (this) {
            if (linkedStyleSheets != null) {
                linkedStyleSheets.removeElement(ss);
                if (linkedStyleSheets.size() == 0) {
                    linkedStyleSheets = null;
                }
            }
        }
    }

    /** The added sheets, or null if there are none. */
    public StyleSheet[] getStyleSheets() {
        StyleSheet[] retValue = null;
        synchronized (this) {
            if (linkedStyleSheets != null) {
                retValue = new StyleSheet[linkedStyleSheets.size()];
                linkedStyleSheets.copyInto(retValue);
            }
        }
        return retValue;
    }

    /** It fetches a sheet from that address and adds it. */
    public void importStyleSheet(URL url) {
        if (url == null) {
            return;
        }
        try {
            java.io.InputStream is = url.openStream();
            Reader r = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StyleSheet ss = new StyleSheet();
            ss.loadRules(r, url);
            r.close();
            addStyleSheet(ss);
        } catch (Throwable e) {
            // A sheet that cannot be fetched is ignored: the page is shown without it.
        }
    }

    /** The address the relative ones are resolved against. */
    public void setBase(URL base) {
        this.base = base;
    }

    public URL getBase() {
        return base;
    }

    /** It sets a CSS property with its value written as in the sheet. */
    public void addCSSAttribute(MutableAttributeSet attr, CSS.Attribute key, String value) {
        attr.addAttribute(key, value);
    }

    /**
     * The same, but it reports whether the value is not understood.
     *
     * <p>The difference from {@link #addCSSAttribute} is who decides: when the value comes from an
     * HTML attribute and not from a sheet, it is better not to keep rubbish, because old HTML
     * carries values that are not CSS.
     */
    public boolean addCSSAttributeFromHTML(MutableAttributeSet attr, CSS.Attribute key,
            String value) {
        if (value == null) {
            return false;
        }
        // The empty string does hold: for a colour it is black. Rejecting it up front would be
                // tidier and would not be what the JDK does.
        if (key == CSS.Attribute.COLOR || key == CSS.Attribute.BACKGROUND_COLOR) {
            if (stringToColor(value) == null) {
                return false;
            }
        }
        attr.addAttribute(key, value);
        return true;
    }

    /**
     * It translates the old HTML attributes to CSS properties.
     *
     * <p>It is what makes <code>&lt;font color="red"&gt;</code> and
     * <code>&lt;span style="color: red"&gt;</code> end up in the same place. Without this
     * translation there would be two paths for each aspect and the precedence rules could not be
     * written.
     */
    public AttributeSet translateHTMLToCSS(AttributeSet htmlAttrSet) {
        MutableAttributeSet cssAttrSet = new SimpleAttributeSet();
        Enumeration<?> names = htmlAttrSet.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Attribute) {
                HTML.Attribute a = (HTML.Attribute) name;
                Object v = htmlAttrSet.getAttribute(a);
                if (v != null) {
                    translate(cssAttrSet, a, v.toString());
                }
            }
        }
        return cssAttrSet;
    }

    private void translate(MutableAttributeSet out, HTML.Attribute a, String v) {
        if (a == HTML.Attribute.COLOR) {
            addCSSAttributeFromHTML(out, CSS.Attribute.COLOR, v);
        } else if (a == HTML.Attribute.TEXT) {
            addCSSAttributeFromHTML(out, CSS.Attribute.COLOR, v);
        } else if (a == HTML.Attribute.BGCOLOR) {
            addCSSAttributeFromHTML(out, CSS.Attribute.BACKGROUND_COLOR, v);
        } else if (a == HTML.Attribute.BACKGROUND) {
            addCSSAttribute(out, CSS.Attribute.BACKGROUND_IMAGE, v);
        } else if (a == HTML.Attribute.FACE) {
            addCSSAttribute(out, CSS.Attribute.FONT_FAMILY, v);
        } else if (a == HTML.Attribute.SIZE) {
            addCSSAttribute(out, CSS.Attribute.FONT_SIZE, v);
        } else if (a == HTML.Attribute.WIDTH) {
            addCSSAttribute(out, CSS.Attribute.WIDTH, v);
        } else if (a == HTML.Attribute.HEIGHT) {
            addCSSAttribute(out, CSS.Attribute.HEIGHT, v);
        } else if (a == HTML.Attribute.ALIGN) {
            addCSSAttribute(out, CSS.Attribute.TEXT_ALIGN, v);
        } else if (a == HTML.Attribute.VALIGN) {
            addCSSAttribute(out, CSS.Attribute.VERTICAL_ALIGN, v);
        } else if (a == HTML.Attribute.HSPACE) {
            addCSSAttribute(out, CSS.Attribute.MARGIN_LEFT, v);
            addCSSAttribute(out, CSS.Attribute.MARGIN_RIGHT, v);
        } else if (a == HTML.Attribute.VSPACE) {
            addCSSAttribute(out, CSS.Attribute.MARGIN_TOP, v);
            addCSSAttribute(out, CSS.Attribute.MARGIN_BOTTOM, v);
        }
    }

    public AttributeSet addAttribute(AttributeSet old, Object key, Object value) {
        return super.addAttribute(old, key, value);
    }

    public AttributeSet addAttributes(AttributeSet old, AttributeSet attr) {
        return super.addAttributes(old, attr);
    }

    public AttributeSet removeAttribute(AttributeSet old, Object key) {
        return super.removeAttribute(old, key);
    }

    public AttributeSet removeAttributes(AttributeSet old, Enumeration<?> names) {
        return super.removeAttributes(old, names);
    }

    public AttributeSet removeAttributes(AttributeSet old, AttributeSet attrs) {
        return super.removeAttributes(old, attrs);
    }

    protected StyleContext$SmallAttributeSet createSmallAttributeSet(AttributeSet a) {
        return super.createSmallAttributeSet(a);
    }

    protected MutableAttributeSet createLargeAttributeSet(AttributeSet a) {
        return super.createLargeAttributeSet(a);
    }

    /** The typeface that corresponds to those attributes. */
    public Font getFont(AttributeSet a) {
        String family = value(a, CSS.Attribute.FONT_FAMILY);
        if (family == null) {
            family = "SansSerif";
        }
        int style = Font.PLAIN;
        String peso = value(a, CSS.Attribute.FONT_WEIGHT);
        if (peso != null && (peso.equals("bold") || peso.equals("bolder"))) {
            style = style | Font.BOLD;
        }
        String slant = value(a, CSS.Attribute.FONT_STYLE);
        if (slant != null && (slant.equals("italic")
                || slant.equals("oblique"))) {
            style = style | Font.ITALIC;
        }
        String tam = value(a, CSS.Attribute.FONT_SIZE);
        int points = (tam == null) ? 12 : (int) sizeInPoints(tam);
        return getFont(family, style, points);
    }

    /** The letter's colour, or black. */
    public Color getForeground(AttributeSet a) {
        String c = value(a, CSS.Attribute.COLOR);
        Color col = (c == null) ? null : stringToColor(c);
        return (col == null) ? Color.black : col;
    }

    /** The background colour, or null if it is transparent. */
    public Color getBackground(AttributeSet a) {
        String c = value(a, CSS.Attribute.BACKGROUND_COLOR);
        if (c == null || "transparent".equals(c)) {
            return null;
        }
        return stringToColor(c);
    }

    private static String value(AttributeSet a, CSS.Attribute key) {
        Object v = a.getAttribute(key);
        return (v == null) ? null : v.toString();
    }

    /** Who computes margins and draws a block's background. */
    public BoxPainter getBoxPainter(AttributeSet a) {
        return new BoxPainter(a, this);
    }

    /** Who draws a list's bullets or numbers. */
    public ListPainter getListPainter(AttributeSet a) {
        return new ListPainter(a, this);
    }

    /**
     * The base size the relative ones are counted from.
     *
     * <p>It does not change what {@link #getPointSize(int)} returns: the sizes from 1 to 7 are
     * fixed. It changes the starting point of those written as <code>+1</code> or <code>-2</code>.
     */
    public void setBaseFontSize(int sz) {
        if (sz < 1) {
            baseFontSize = 1;
        } else if (sz > 7) {
            baseFontSize = 7;
        } else {
            baseFontSize = sz;
        }
    }

    /** The same, with the size written out; it accepts the relative forms. */
    public void setBaseFontSize(String size) {
        if (size == null) {
            return;
        }
        size = size.trim();
        if (size.length() == 0) {
            return;
        }
        // A size that is not understood comes out as NumberFormatException, as in the JDK.
                // Swallowing it would leave the program believing it set a size that was never set.
        if (size.charAt(0) == '+') {
            setBaseFontSize(baseFontSize + Integer.parseInt(size.substring(1)));
        } else if (size.charAt(0) == '-') {
            setBaseFontSize(baseFontSize - Integer.parseInt(size.substring(1)));
        } else {
            setBaseFontSize(Integer.parseInt(size));
        }
    }

    /**
     * Which of HTML's seven sizes that number of points falls in.
     *
     * <p>The first one that reaches or exceeds is chosen: 9 points no longer fits in 1, so it is
     * 2.
     */
    public static int getIndexOfSize(float pt) {
        for (int i = 0; i < sizeMapDefault.length; i++) {
            if (pt <= sizeMapDefault[i]) {
                return i + 1;
            }
        }
        return sizeMapDefault.length;
    }

    /** The points that HTML size is worth, from 1 to 7. */
    public float getPointSize(int index) {
        if (index < 1) {
            index = 1;
        } else if (index > sizeMapDefault.length) {
            index = sizeMapDefault.length;
        }
        return sizeMapDefault[index - 1];
    }

    /**
     * The points of a written size, absolute or relative to the base one.
     *
     * @throws NumberFormatException if it is not a number.
     */
    public float getPointSize(String size) {
        int relative = 0;
        if (size.startsWith("+")) {
            relative = Integer.parseInt(size.substring(1));
            return getPointSize(baseFontSize + relative);
        }
        if (size.startsWith("-")) {
            relative = Integer.parseInt(size.substring(1));
            return getPointSize(baseFontSize - relative);
        }
        return getPointSize(Integer.parseInt(size));
    }

    /** The points of a CSS size, with or without a unit. */
    private float sizeInPoints(String v) {
        v = v.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            if (v.endsWith("pt")) {
                return Float.parseFloat(v.substring(0, v.length() - 2));
            }
            if (v.endsWith("px")) {
                return Float.parseFloat(v.substring(0, v.length() - 2));
            }
            return getPointSize(v);
        } catch (NumberFormatException nfe) {
            return 12f;
        }
    }

    /**
     * The colour that text names, or null if it is not understood.
     *
     * <p>It accepts CSS's sixteen names ignoring case, a hexadecimal number with or without
     * <code>#</code>, and <code>rgb(r,g,b)</code> in lower case.
     *
     * <p>Returning null and not black matters: whoever asks needs to tell "black was asked for"
     * from "it could not be read", because in the second case the colour that was already there
     * has to be left. The only exception is the empty string, which gives black.
     *
     * <p>The spaces are not trimmed. It looks like an omission and it is not: a <code>" red"</code>
     * with a space in front is not a valid colour in CSS, and accepting it would cover up an error
     * in the sheet.
     */
    public Color stringToColor(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return Color.black;
        }
        if (str.startsWith("rgb(")) {
            return readRGB(str);
        }
        if (str.charAt(0) == '#') {
            return hexToColor(str);
        }
        Color named = colors.get(str.toLowerCase(java.util.Locale.ROOT));
        if (named != null) {
            return named;
        }
        // What is not a name is tried as hexadecimal without `#`; that way `ff0000` works.
        return hexToColor(str);
    }

    /**
     * A colour written in hexadecimal.
     *
     * <p>At most six digits are taken and read as a single number, so <code>#ff00</code> is green
     * and not an error. Those of three digits are doubled: <code>#f00</code> is
     * <code>#ff0000</code>.
     */
    private static Color hexToColor(String value) {
        String digits;
        if (value.startsWith("#")) {
            digits = value.substring(1, Math.min(value.length(), 7));
        } else {
            digits = value;
        }
        if (digits.length() == 3) {
            digits = "" + digits.charAt(0) + digits.charAt(0) + digits.charAt(1)
                    + digits.charAt(1) + digits.charAt(2) + digits.charAt(2);
        }
        try {
            return Color.decode("0x" + digits);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * A colour written as <code>rgb(r,g,b)</code>.
     *
     * <p>The components that are missing are worth zero and those that overflow are clamped to
     * 0..255. It is on purpose: a badly written colour is shown all the same, instead of leaving
     * the page with no colour.
     */
    private static Color readRGB(String string) {
        int[] index = new int[1];
        index[0] = 4;
        int red = component(string, index);
        int green = component(string, index);
        int blue = component(string, index);
        return new Color(red, green, blue);
    }

    /** The next number in the string, clamped to 0..255; zero if there is none. */
    private static int component(String string, int[] index) {
        int length = string.length();
        char c;
        while (index[0] < length && (c = string.charAt(index[0])) != '-'
                && !Character.isDigit(c) && c != '.') {
            index[0] = index[0] + 1;
        }
        int from = index[0];
        if (from < length && string.charAt(index[0]) == '-') {
            index[0] = index[0] + 1;
        }
        while (index[0] < length && Character.isDigit(string.charAt(index[0]))) {
            index[0] = index[0] + 1;
        }
        if (index[0] < length && string.charAt(index[0]) == '.') {
            index[0] = index[0] + 1;
            while (index[0] < length && Character.isDigit(string.charAt(index[0]))) {
                index[0] = index[0] + 1;
            }
        }
        if (from != index[0]) {
            try {
                float value = Float.parseFloat(string.substring(from, index[0]));
                if (index[0] < length && string.charAt(index[0]) == '%') {
                    index[0] = index[0] + 1;
                    value = value * 255f / 100f;
                }
                return Math.min(255, Math.max(0, (int) value));
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * A block's margins and background.
     *
     * <p>In the JDK it is an inner class; here it is static and takes the sheet, because of
     * findings #507 and #508. The resulting signature is not public, so it is not seen from
     * outside.
     */
    public static final class BoxPainter implements java.io.Serializable {

        private final AttributeSet a;
        private final StyleSheet sheet;

        BoxPainter(AttributeSet a, StyleSheet sheet) {
            this.a = a;
            this.sheet = sheet;
        }

        /** How much margin it leaves on that side, in pixels. */
        public float getInset(int side, View v) {
            CSS.Attribute key;
            if (side == View.TOP) {
                key = CSS.Attribute.MARGIN_TOP;
            } else if (side == View.BOTTOM) {
                key = CSS.Attribute.MARGIN_BOTTOM;
            } else if (side == View.LEFT) {
                key = CSS.Attribute.MARGIN_LEFT;
            } else {
                key = CSS.Attribute.MARGIN_RIGHT;
            }
            Object o = a.getAttribute(key);
            if (o == null) {
                return 0f;
            }
            try {
                String s = o.toString().trim();
                if (s.endsWith("px") || s.endsWith("pt")) {
                    s = s.substring(0, s.length() - 2);
                }
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException nfe) {
                return 0f;
            }
        }

        /** It paints the block's background, if it has a colour. */
        public void paint(Graphics g, float x, float y, float w, float h, View v) {
            Color background = sheet.getBackground(a);
            if (background != null) {
                g.setColor(background);
                g.fillRect((int) x, (int) y, (int) w, (int) h);
            }
        }
    }

    /**
     * A list row's bullet or number.
     *
     * <p>The row's number arrives as a parameter and is not kept: the same list is drawn many times
     * and keeping it would force one painter per row.
     */
    public static final class ListPainter implements java.io.Serializable {

        private final AttributeSet a;
        private final StyleSheet sheet;

        ListPainter(AttributeSet a, StyleSheet sheet) {
            this.a = a;
            this.sheet = sheet;
        }

        /** It draws the mark of row number such and such. */
        public void paint(Graphics g, float x, float y, float w, float h, View v, int item) {
            Object o = a.getAttribute(CSS.Attribute.LIST_STYLE_TYPE);
            String type = (o == null) ? "disc" : o.toString();
            g.setColor(sheet.getForeground(a));
            if ("none".equals(type)) {
                return;
            }
            if ("decimal".equals(type)) {
                g.drawString((item + 1) + ".", (int) x, (int) (y + h));
                return;
            }
            int d = (int) Math.max(4, h / 3);
            int cx = (int) x;
            int cy = (int) (y + (h - d) / 2);
            if ("circle".equals(type)) {
                g.drawOval(cx, cy, d, d);
            } else if ("square".equals(type)) {
                g.fillRect(cx, cy, d, d);
            } else {
                g.fillOval(cx, cy, d, d);
            }
        }
    }

    static {
        colors.put("black", new Color(0, 0, 0));
        colors.put("silver", new Color(192, 192, 192));
        colors.put("gray", new Color(128, 128, 128));
        colors.put("white", new Color(255, 255, 255));
        colors.put("maroon", new Color(128, 0, 0));
        colors.put("red", new Color(255, 0, 0));
        colors.put("purple", new Color(128, 0, 128));
        colors.put("fuchsia", new Color(255, 0, 255));
        colors.put("green", new Color(0, 128, 0));
        colors.put("lime", new Color(0, 255, 0));
        colors.put("olive", new Color(128, 128, 0));
        colors.put("yellow", new Color(255, 255, 0));
        colors.put("navy", new Color(0, 0, 128));
        colors.put("blue", new Color(0, 0, 255));
        colors.put("teal", new Color(0, 128, 128));
        colors.put("aqua", new Color(0, 255, 255));
    }
}
