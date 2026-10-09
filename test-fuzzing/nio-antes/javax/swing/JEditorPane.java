package javax.swing;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.accessibility.AccessibleContext;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

/**
 * A text component that changes personality according to the content type.
 *
 * <h2>The type rules</h2>
 *
 * <p>Telling it {@code setContentType("text/html")} changes its {@link EditorKit}, and with it
 * change the document, the views and the actions. The table from types to kits is static and
 * can be extended ({@link #registerEditorKitForContentType}), which is how an application adds
 * its own format.
 *
 * <p>The kits registered by default are the JDK's three -- plain text, HTML and RTF --; in this
 * library only the plain text one exists, so the other two are asked for and do not appear. It
 * is said here because the symptom -- an HTML document that is seen as text -- would confuse.
 *
 * <h2>The pages</h2>
 *
 * <p>{@link #setPage} reads from an address. It works with the addresses
 * {@code java.net.URL} knows how to open on this VM; the loading is synchronous, without the
 * background thread the JDK uses for large documents
 * ({@code AsynchronousLoadPriority}).
 */
public class JEditorPane extends JTextComponent {

    private static final String uiClassID = "EditorPaneUI";

    /** The document's property with the data of a submitted form. */
    public static final String PostDataProperty = "javax.swing.JEditorPane.postdata";

    /** The property that asks for the length units to be interpreted as the W3C requires. */
    public static final String W3C_LENGTH_UNITS = "JEditorPane.w3cLengthUnits";

    /** The property that asks for the component's typeface and colour to be respected. */
    public static final String HONOR_DISPLAY_PROPERTIES = "JEditorPane.honorDisplayProperties";

    /** The default editor kits, by content type. */
    static final Map<String, String> defaultEditorKitMap = new Hashtable<String, String>(0);

    private static final Hashtable<String, String> kitRegistry =
            new Hashtable<String, String>(3);

    private Hashtable<String, EditorKit> typeHandlers;
    private EditorKit kit;
    private String contentType = "text/plain";
    private URL pageUrl;
    private boolean isUserSetEditorKit;

    /** An empty plain text pane. */
    public JEditorPane() {
        super();
        setFocusCycleRoot(true);
    }

    /** A pane that shows that address. */
    public JEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    /** A pane that shows that address, written as text. */
    public JEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    /** A pane with that content type and that text. */
    public JEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }

    public synchronized void addHyperlinkListener(HyperlinkListener listener) {
        listenerList.add(HyperlinkListener.class, listener);
    }

    public synchronized void removeHyperlinkListener(HyperlinkListener listener) {
        listenerList.remove(HyperlinkListener.class, listener);
    }

    public synchronized HyperlinkListener[] getHyperlinkListeners() {
        return listenerList.getListeners(HyperlinkListener.class);
    }

    /** It gives notice that a link was touched; the view that drew it calls it. */
    public void fireHyperlinkUpdate(HyperlinkEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == HyperlinkListener.class) {
                ((HyperlinkListener) listeners[i + 1]).hyperlinkUpdate(e);
            }
        }
    }

    /** It shows that address; the loading is synchronous, see the class note. */
    public void setPage(URL page) throws IOException {
        if (page == null) {
            throw new IOException("invalid url");
        }
        URL loaded = getPage();
        if (loaded == null || !loaded.sameFile(page)) {
            InputStream in = getStream(page);
            if (kit != null) {
                Document doc = kit.createDefaultDocument();
                doc.putProperty(Document.StreamDescriptionProperty, page);
                read(in, doc);
                setDocument(doc);
            }
            pageUrl = page;
            firePropertyChange("page", loaded, page);
        }
    }

    /** It reads from a stream with the installed editor kit. */
    public void read(InputStream in, Object desc) throws IOException {
        if (desc instanceof URL) {
            pageUrl = (URL) desc;
        }
        Document doc = kit != null ? kit.createDefaultDocument() : new javax.swing.text.PlainDocument();
        if (desc != null) {
            doc.putProperty(Document.StreamDescriptionProperty, desc);
        }
        read(in, doc);
        setDocument(doc);
    }

    /** It reads from a stream into that document. */
    void read(InputStream in, Document doc) throws IOException {
        try {
            String charset = (String) getClientProperty("charset");
            java.io.Reader r = (charset != null) ? new java.io.InputStreamReader(in, charset)
                    : new java.io.InputStreamReader(in);
            if (kit != null) {
                kit.read(r, doc, 0);
            }
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }

    /** That address's stream; a subclass may redefine it in order to read from somewhere else. */
    protected InputStream getStream(URL page) throws IOException {
        java.net.URLConnection conn = page.openConnection();
        InputStream in = conn.getInputStream();
        String type = conn.getContentType();
        if (type != null) {
            setContentType(type);
        }
        return in;
    }

    /**
     * It takes the view as far as that reference of the page.
     *
     * <p>It does nothing: with no HTML kit there are no anchors to go to.
     */
    public void scrollToReference(String reference) {
    }

    public URL getPage() {
        return pageUrl;
    }

    public void setPage(String url) throws IOException {
        if (url == null) {
            throw new IOException("invalid url");
        }
        URL page = new URL(url);
        setPage(page);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The default kit: the plain text one. */
    protected EditorKit createDefaultEditorKit() {
        return new DefaultEditorKit();
    }

    /** The installed kit; it is created on first use. */
    public EditorKit getEditorKit() {
        if (kit == null) {
            kit = createDefaultEditorKit();
            isUserSetEditorKit = false;
        }
        return kit;
    }

    public final String getContentType() {
        return (kit != null) ? kit.getContentType() : contentType;
    }

    /** It changes the type, and with it the editor kit; see the class note. */
    public final void setContentType(String type) {
        int parm = type.indexOf(";");
        if (parm > -1) {
            // What follows the semicolon is discarded, save the character set.
            String paramList = type.substring(parm);
            type = type.substring(0, parm).trim();
            int slash = paramList.toLowerCase().indexOf("charset");
            if (slash > -1) {
                slash = paramList.indexOf("=", slash);
                if (slash > -1) {
                    putClientProperty("charset", paramList.substring(slash + 1).trim());
                }
            }
        }
        if ((kit == null) || (!type.equals(kit.getContentType())) || !isUserSetEditorKit) {
            EditorKit k = getEditorKitForContentType(type);
            if (k != null && k != kit) {
                setEditorKit(k);
                isUserSetEditorKit = false;
            }
        }
        contentType = type;
    }

    /** It installs that kit; the document becomes one the kit creates. */
    public void setEditorKit(EditorKit k) {
        EditorKit old = kit;
        isUserSetEditorKit = true;
        if (old != null) {
            old.deinstall(this);
        }
        kit = k;
        if (kit != null) {
            kit.install(this);
            setDocument(kit.createDefaultDocument());
        }
        firePropertyChange("editorKit", old, k);
    }

    /** The kit for that type, from the registry; one is created the first time. */
    public EditorKit getEditorKitForContentType(String type) {
        if (typeHandlers == null) {
            typeHandlers = new Hashtable<String, EditorKit>(3);
        }
        EditorKit k = typeHandlers.get(type);
        if (k == null) {
            k = createEditorKitForContentType(type);
            if (k != null) {
                setEditorKitForContentType(type, k);
            }
        }
        if (k == null) {
            k = createDefaultEditorKit();
        }
        return k;
    }

    public void setEditorKitForContentType(String type, EditorKit k) {
        if (typeHandlers == null) {
            typeHandlers = new Hashtable<String, EditorKit>(3);
        }
        typeHandlers.put(type, k);
    }

    /** It replaces the selection; over a read-only document it does nothing. */
    public void replaceSelection(String content) {
        super.replaceSelection(content);
    }

    /**
     * It creates the kit registered for that type.
     *
     * <p>{@code null} if there is none registered or if its class cannot be loaded; see the class
     * note about the missing kits.
     */
    public static EditorKit createEditorKitForContentType(String type) {
        String className = kitRegistry.get(type);
        if (className == null) {
            return null;
        }
        try {
            Class<?> c = Class.forName(className);
            return (EditorKit) c.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public static void registerEditorKitForContentType(String type, String classname) {
        kitRegistry.put(type, classname);
    }

    /** Like the previous one; the loader is ignored, this VM has a single one. */
    public static void registerEditorKitForContentType(String type, String classname,
            ClassLoader loader) {
        registerEditorKitForContentType(type, classname);
    }

    public static String getEditorKitClassNameForContentType(String type) {
        return kitRegistry.get(type);
    }

    /**
     * The preferred size, enlarged to that of the viewport that shows it.
     *
     * <p>Without this, a short document inside a pane with bars would leave the pane's background
     * in sight below the text.
     */
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            java.awt.Dimension ext = port.getExtentSize();
            if (getScrollableTracksViewportWidth()) {
                d.width = Math.max(d.width, ext.width);
            }
            if (getScrollableTracksViewportHeight()) {
                d.height = Math.max(d.height, ext.height);
            }
        }
        return d;
    }

    public void setText(String t) {
        try {
            Document doc = getDocument();
            doc.remove(0, doc.getLength());
            if (t == null || t.equals("")) {
                return;
            }
            java.io.Reader r = new java.io.StringReader(t);
            EditorKit k = getEditorKit();
            k.read(r, doc, 0);
        } catch (IOException ioe) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (BadLocationException ble) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    public String getText() {
        java.io.StringWriter out = new java.io.StringWriter();
        try {
            write(out);
        } catch (IOException ioe) {
            return null;
        }
        return out.toString();
    }

    public boolean getScrollableTracksViewportWidth() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    protected String paramString() {
        String kitString = (kit != null ? kit.toString() : "");
        String typeHandlersString = (typeHandlers != null ? typeHandlers.toString() : "");
        return super.paramString() + ",kit=" + kitString + ",typeHandlers=" + typeHandlersString;
    }

    /** With no accessibility context: there is no assistive technology on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
