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
 * Un componente de texto que cambia de personalidad segun el tipo de contenido.
 *
 * <h2>El tipo manda</h2>
 *
 * <p>Decirle {@code setContentType("text/html")} le cambia el {@link EditorKit}, y con el cambian
 * el documento, las vistas y las acciones. La tabla de tipos a juegos es estatica y se puede
 * ampliar ({@link #registerEditorKitForContentType}), que es como una aplicacion agrega su propio
 * formato.
 *
 * <p>Los juegos registrados por omision son los tres del JDK —texto plano, HTML y RTF—; en esta
 * biblioteca solo el de texto plano existe, asi que los otros dos se piden y no aparecen. Esta
 * dicho aca porque el sintoma —un documento HTML que se ve como texto— confundiria.
 *
 * <h2>Las paginas</h2>
 *
 * <p>{@link #setPage} lee de una direccion. Funciona con las direcciones que
 * {@code java.net.URL} sepa abrir en esta VM; la carga es sincronica, sin el hilo de fondo que el
 * JDK usa para documentos grandes ({@code AsynchronousLoadPriority}).
 */
public class JEditorPane extends JTextComponent {

    private static final String uiClassID = "EditorPaneUI";

    /** La propiedad del documento con los datos de un formulario enviado. */
    public static final String PostDataProperty = "javax.swing.JEditorPane.postdata";

    /** La propiedad que pide interpretar las unidades de largo como manda el W3C. */
    public static final String W3C_LENGTH_UNITS = "JEditorPane.w3cLengthUnits";

    /** La propiedad que pide respetar la fuente y el color del componente. */
    public static final String HONOR_DISPLAY_PROPERTIES = "JEditorPane.honorDisplayProperties";

    /** Los juegos de edicion por omision, por tipo de contenido. */
    static final Map<String, String> defaultEditorKitMap = new Hashtable<String, String>(0);

    private static final Hashtable<String, String> kitRegistry =
            new Hashtable<String, String>(3);

    private Hashtable<String, EditorKit> typeHandlers;
    private EditorKit kit;
    private String contentType = "text/plain";
    private URL pageUrl;
    private boolean isUserSetEditorKit;

    /** Un panel vacio de texto plano. */
    public JEditorPane() {
        super();
        setFocusCycleRoot(true);
    }

    /** Un panel que muestra esa direccion. */
    public JEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    /** Un panel que muestra esa direccion, escrita como texto. */
    public JEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    /** Un panel con ese tipo de contenido y ese texto. */
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

    /** Avisa que se toco un enlace; lo llama la vista que lo dibujo. */
    public void fireHyperlinkUpdate(HyperlinkEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == HyperlinkListener.class) {
                ((HyperlinkListener) listeners[i + 1]).hyperlinkUpdate(e);
            }
        }
    }

    /** Muestra esa direccion; la carga es sincronica, ver la nota de la clase. */
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

    /** Lee de un flujo con el juego de edicion instalado. */
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

    /** Lee de un flujo dentro de ese documento. */
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

    /** El flujo de esa direccion; una subclase puede redefinirlo para leer de otro lado. */
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
     * Lleva la vista hasta esa referencia de la pagina.
     *
     * <p>No hace nada: sin el juego de HTML no hay anclas a las que ir.
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

    /** El juego de omision: el de texto plano. */
    protected EditorKit createDefaultEditorKit() {
        return new DefaultEditorKit();
    }

    /** El juego instalado; se crea al primer uso. */
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

    /** Cambia el tipo, y con el el juego de edicion; ver la nota de la clase. */
    public final void setContentType(String type) {
        int parm = type.indexOf(";");
        if (parm > -1) {
            // Se descarta lo que sigue al punto y coma, salvo el juego de caracteres.
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

    /** Instala ese juego; el documento pasa a ser uno que el juego cree. */
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

    /** El juego para ese tipo, del registro; se crea uno la primera vez. */
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

    /** Reemplaza la seleccion; sobre un documento de solo lectura no hace nada. */
    public void replaceSelection(String content) {
        super.replaceSelection(content);
    }

    /**
     * Crea el juego registrado para ese tipo.
     *
     * <p>{@code null} si no hay ninguno registrado o si su clase no se puede cargar; ver la nota
     * de la clase sobre los juegos que faltan.
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

    /** Como la anterior; el cargador se ignora, esta VM tiene uno solo. */
    public static void registerEditorKitForContentType(String type, String classname,
            ClassLoader loader) {
        registerEditorKitForContentType(type, classname);
    }

    public static String getEditorKitClassNameForContentType(String type) {
        return kitRegistry.get(type);
    }

    /**
     * El tamano preferido, agrandado hasta el de la ventana que lo muestra.
     *
     * <p>Sin esto, un documento corto dentro de un panel con barras dejaria el fondo del panel a
     * la vista debajo del texto.
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

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
