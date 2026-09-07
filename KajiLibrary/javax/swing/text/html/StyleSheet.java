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
 * Una hoja de estilos: reglas de CSS que le dan atributos a los elementos.
 *
 * <h2>Es un {@link StyleContext}, y eso no es casual</h2>
 *
 * <p>Un contexto de estilos ya sabe compartir conjuntos de atributos y buscarlos por nombre. Una
 * hoja de estilos necesita exactamente eso, mas la parte de CSS: leer reglas, elegir cual se aplica
 * a un elemento, y traducir los atributos viejos de HTML (<code>bgcolor</code>,
 * <code>align</code>) a propiedades de CSS.
 *
 * <h2>Como se elige la regla</h2>
 *
 * <p>Una regla se guarda con un nombre que es el camino de elementos que la selecciona, por ejemplo
 * <code>html body p</code>. Para un elemento del documento se arma su camino y se buscan todas las
 * reglas que sean un sufijo de el; las que aparecen se juntan, y la mas especifica gana.
 *
 * <h2>Hasta donde llega esta implementacion</h2>
 *
 * <p>Lee reglas, las junta, resuelve colores y tamanos de letra, y traduce los atributos de HTML.
 * Lo que no hace es el modelo de caja completo: {@link BoxPainter} y {@link ListPainter} calculan
 * margenes y dibujan vinetas, pero no bordes con estilo ni imagenes de fondo. Son las partes que
 * solo se notan con una pantalla, y esta biblioteca todavia no tiene con que compararlas.
 */
public class StyleSheet extends StyleContext {

    private Vector<StyleSheet> linkedStyleSheets;
    private URL base;
    private int baseFontSize = 4;

    /** Los tamanos de letra de HTML, del 1 al 7. */
    private static final int[] sizeMapDefault = {8, 10, 12, 14, 18, 24, 36};

    private static final Hashtable<String, Color> colores = new Hashtable<String, Color>();

    /** Una hoja vacia. */
    public StyleSheet() {
        super();
    }

    /**
     * La regla que se aplica a ese elemento con esa etiqueta.
     *
     * <p>Arma el camino desde la raiz y junta todo lo que coincida; ver la nota de la clase.
     */
    public Style getRule(HTML.Tag t, Element e) {
        String camino = caminoDe(t, e);
        return getRule(camino);
    }

    /** El camino de un elemento, de la raiz hacia abajo, para buscar reglas. */
    private String caminoDe(HTML.Tag t, Element e) {
        Vector<String> partes = new Vector<String>();
        partes.addElement(t.toString());
        for (Element p = (e == null) ? null : e.getParentElement(); p != null;
                p = p.getParentElement()) {
            AttributeSet a = p.getAttributes();
            Object nombre = a.getAttribute(StyleConstants.NameAttribute);
            if (nombre instanceof HTML.Tag) {
                partes.insertElementAt(nombre.toString(), 0);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(partes.elementAt(i));
        }
        return sb.toString();
    }

    /**
     * La regla con ese selector.
     *
     * <p>Junta la regla exacta y todas las que sean un sufijo del camino: para
     * <code>html body p</code> tambien entran <code>body p</code> y <code>p</code>. Se aplican de
     * la menos especifica a la mas especifica, asi que la mas larga gana.
     */
    public Style getRule(String selector) {
        selector = limpiarSelector(selector);
        String[] partes = partirCamino(selector);
        Vector<Style> encontradas = new Vector<Style>();
        // De la mas larga a la mas corta: la primera que conteste gana.
        for (int desde = 0; desde < partes.length; desde++) {
            StringBuilder sb = new StringBuilder();
            for (int i = desde; i < partes.length; i++) {
                if (i > desde) {
                    sb.append(' ');
                }
                sb.append(partes[i]);
            }
            Style s = getStyle(sb.toString());
            if (s != null) {
                encontradas.addElement(s);
            }
        }
        Style[] arr = new Style[encontradas.size()];
        encontradas.copyInto(arr);
        return new EstiloResuelto(selector, arr);
    }

    /**
     * El estilo que devuelve {@link #getRule}: una vista sobre las reglas que coincidieron.
     *
     * <h2>No copia, multiplexa</h2>
     *
     * <p>Guarda las reglas que coincidieron, de la mas especifica a la menos, y contesta cada
     * consulta recorriendolas en ese orden. La primera que tenga el atributo gana.
     *
     * <p>Multiplexar en lugar de copiar tiene una consecuencia que se ve: un atributo que esta en
     * dos reglas aparece dos veces al enumerar, aunque {@code getAttribute} devuelva siempre el de
     * la mas especifica. Es la forma del JDK y se conserva porque {@code getAttributeCount} es
     * publico y alguien puede estar contando.
     *
     * <p>Tampoco se registra en la hoja. Si se registrara, cada consulta con un camino nuevo
     * dejaria un estilo guardado para siempre, y una pagina larga los acumularia sin que nadie los
     * borre.
     */
    static final class EstiloResuelto implements Style {

        private final String nombre;
        private final Style[] reglas;

        EstiloResuelto(String nombre, Style[] reglas) {
            this.nombre = nombre;
            this.reglas = reglas;
        }

        public String getName() {
            return nombre;
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
            SimpleAttributeSet copia = new SimpleAttributeSet();
            copia.addAttributes(this);
            return copia;
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
            Vector<Object> todos = new Vector<Object>();
            for (int i = 0; i < reglas.length; i++) {
                Enumeration<?> e = reglas[i].getAttributeNames();
                while (e.hasMoreElements()) {
                    todos.addElement(e.nextElement());
                }
            }
            return todos.elements();
        }

        public boolean containsAttribute(Object name, Object value) {
            Object v = getAttribute(name);
            return (v != null && v.equals(value));
        }

        public boolean containsAttributes(AttributeSet attrs) {
            Enumeration<?> e = attrs.getAttributeNames();
            while (e.hasMoreElements()) {
                Object nombre = e.nextElement();
                if (!containsAttribute(nombre, attrs.getAttribute(nombre))) {
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

    private static String limpiarSelector(String s) {
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

    private static String[] partirCamino(String s) {
        StringTokenizer st = new StringTokenizer(s, " ");
        String[] out = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            out[i] = st.nextToken();
        }
        return out;
    }

    /**
     * Agrega una regla escrita en CSS.
     *
     * <p>Un selector con comas define varias reglas iguales, y por eso se parte: escribir
     * <code>h1, h2 { color: red }</code> es lo mismo que escribir las dos por separado.
     */
    public void addRule(String rule) {
        if (rule == null) {
            return;
        }
        String texto = sinComentarios(rule);
        int i = 0;
        while (i < texto.length()) {
            int abre = texto.indexOf('{', i);
            if (abre < 0) {
                break;
            }
            int cierra = texto.indexOf('}', abre);
            if (cierra < 0) {
                break;
            }
            String selectores = texto.substring(i, abre).trim();
            AttributeSet decl = getDeclaration(texto.substring(abre + 1, cierra));
            StringTokenizer st = new StringTokenizer(selectores, ",");
            while (st.hasMoreTokens()) {
                String sel = limpiarSelector(st.nextToken());
                if (sel.length() > 0) {
                    Style s = getStyle(sel);
                    if (s == null) {
                        s = addStyle(sel, null);
                    }
                    s.addAttributes(decl);
                }
            }
            i = cierra + 1;
        }
    }

    /** Saca los comentarios; una regla adentro de un comentario no cuenta. */
    private static String sinComentarios(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int abre = s.indexOf("/*", i);
            if (abre < 0) {
                sb.append(s.substring(i));
                break;
            }
            sb.append(s, i, abre);
            int cierra = s.indexOf("*/", abre + 2);
            if (cierra < 0) {
                break;
            }
            i = cierra + 2;
        }
        return sb.toString();
    }

    /** Los atributos que declara ese texto, sin las llaves ni el selector. */
    public AttributeSet getDeclaration(String decl) {
        MutableAttributeSet a = new SimpleAttributeSet();
        if (decl == null) {
            return a;
        }
        StringTokenizer st = new StringTokenizer(sinComentarios(decl), ";");
        while (st.hasMoreTokens()) {
            String par = st.nextToken();
            int dosp = par.indexOf(':');
            if (dosp < 0) {
                continue;
            }
            String nombre = par.substring(0, dosp).trim().toLowerCase(java.util.Locale.ROOT);
            String valor = par.substring(dosp + 1).trim();
            CSS.Attribute clave = CSS.getAttribute(nombre);
            if (clave != null && valor.length() > 0) {
                addCSSAttribute(a, clave, valor);
            }
        }
        return a;
    }

    /** Lee reglas de un texto; la direccion sirve para resolver las que sean relativas. */
    public void loadRules(Reader in, URL ref) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        addRule(sb.toString());
    }

    /** Los atributos que le tocan a esa vista, ya resueltos. */
    public AttributeSet getViewAttributes(View v) {
        return v.getElement().getAttributes();
    }

    public void removeStyle(String nm) {
        super.removeStyle(nm);
    }

    /**
     * Agrega otra hoja debajo de esta.
     *
     * <p>Las hojas agregadas se consultan despues de las reglas propias, en el orden en que se
     * agregaron. Es lo que permite tener una hoja del programa y encima la de la pagina.
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
     * Saca una hoja agregada.
     *
     * <p>Cuando se va la ultima, la lista vuelve a ser nula y {@link #getStyleSheets} vuelve a
     * contestar nulo, no un arreglo vacio. Es la misma respuesta que antes de agregar la primera:
     * el estado despues de sacar todo es el estado inicial, y no uno parecido.
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

    /** Las hojas agregadas, o nulo si no hay ninguna. */
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

    /** Trae una hoja de esa direccion y la agrega. */
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
            // Una hoja que no se puede traer se ignora: la pagina se muestra sin ella.
        }
    }

    /** La direccion contra la que se resuelven las relativas. */
    public void setBase(URL base) {
        this.base = base;
    }

    public URL getBase() {
        return base;
    }

    /** Pone una propiedad de CSS con su valor escrito como en la hoja. */
    public void addCSSAttribute(MutableAttributeSet attr, CSS.Attribute key, String value) {
        attr.addAttribute(key, value);
    }

    /**
     * Igual, pero avisa si el valor no se entiende.
     *
     * <p>La diferencia con {@link #addCSSAttribute} es quien decide: cuando el valor viene de un
     * atributo de HTML y no de una hoja, conviene no guardar basura, porque el HTML viejo trae
     * valores que no son CSS.
     */
    public boolean addCSSAttributeFromHTML(MutableAttributeSet attr, CSS.Attribute key,
            String value) {
        if (value == null) {
            return false;
        }
        // La cadena vacia si vale: para un color es negro. Rechazarla de entrada seria mas
        // prolijo y no seria lo que hace el JDK.
        if (key == CSS.Attribute.COLOR || key == CSS.Attribute.BACKGROUND_COLOR) {
            if (stringToColor(value) == null) {
                return false;
            }
        }
        attr.addAttribute(key, value);
        return true;
    }

    /**
     * Traduce los atributos viejos de HTML a propiedades de CSS.
     *
     * <p>Es lo que permite que <code>&lt;font color="red"&gt;</code> y
     * <code>&lt;span style="color: red"&gt;</code> terminen en el mismo lugar. Sin esta traduccion
     * habria dos caminos para cada aspecto y las reglas de precedencia no se podrian escribir.
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
                    traducir(cssAttrSet, a, v.toString());
                }
            }
        }
        return cssAttrSet;
    }

    private void traducir(MutableAttributeSet out, HTML.Attribute a, String v) {
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

    /** La tipografia que corresponde a esos atributos. */
    public Font getFont(AttributeSet a) {
        String familia = valor(a, CSS.Attribute.FONT_FAMILY);
        if (familia == null) {
            familia = "SansSerif";
        }
        int estilo = Font.PLAIN;
        String peso = valor(a, CSS.Attribute.FONT_WEIGHT);
        if (peso != null && (peso.equals("bold") || peso.equals("bolder"))) {
            estilo = estilo | Font.BOLD;
        }
        String inclinacion = valor(a, CSS.Attribute.FONT_STYLE);
        if (inclinacion != null && (inclinacion.equals("italic")
                || inclinacion.equals("oblique"))) {
            estilo = estilo | Font.ITALIC;
        }
        String tam = valor(a, CSS.Attribute.FONT_SIZE);
        int puntos = (tam == null) ? 12 : (int) tamanoEnPuntos(tam);
        return getFont(familia, estilo, puntos);
    }

    /** El color de la letra, o negro. */
    public Color getForeground(AttributeSet a) {
        String c = valor(a, CSS.Attribute.COLOR);
        Color col = (c == null) ? null : stringToColor(c);
        return (col == null) ? Color.black : col;
    }

    /** El color de fondo, o nulo si es transparente. */
    public Color getBackground(AttributeSet a) {
        String c = valor(a, CSS.Attribute.BACKGROUND_COLOR);
        if (c == null || "transparent".equals(c)) {
            return null;
        }
        return stringToColor(c);
    }

    private static String valor(AttributeSet a, CSS.Attribute clave) {
        Object v = a.getAttribute(clave);
        return (v == null) ? null : v.toString();
    }

    /** Quien calcula margenes y dibuja el fondo de un bloque. */
    public BoxPainter getBoxPainter(AttributeSet a) {
        return new BoxPainter(a, this);
    }

    /** Quien dibuja las vinetas o los numeros de una lista. */
    public ListPainter getListPainter(AttributeSet a) {
        return new ListPainter(a, this);
    }

    /**
     * El tamano base con el que se cuentan los relativos.
     *
     * <p>No cambia lo que devuelve {@link #getPointSize(int)}: los tamanos del 1 al 7 son fijos.
     * Cambia el punto de partida de los que se escriben como <code>+1</code> o <code>-2</code>.
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

    /** Igual, con el tamano escrito; acepta las formas relativas. */
    public void setBaseFontSize(String size) {
        if (size == null) {
            return;
        }
        size = size.trim();
        if (size.length() == 0) {
            return;
        }
        // Un tamano que no se entiende sale como NumberFormatException, igual que en el JDK.
        // Tragarlo dejaria al programa creyendo que puso un tamano que nunca se puso.
        if (size.charAt(0) == '+') {
            setBaseFontSize(baseFontSize + Integer.parseInt(size.substring(1)));
        } else if (size.charAt(0) == '-') {
            setBaseFontSize(baseFontSize - Integer.parseInt(size.substring(1)));
        } else {
            setBaseFontSize(Integer.parseInt(size));
        }
    }

    /**
     * En cual de los siete tamanos de HTML cae esa cantidad de puntos.
     *
     * <p>Se elige el primero que llegue o pase: 9 puntos ya no entra en el 1, asi que es el 2.
     */
    public static int getIndexOfSize(float pt) {
        for (int i = 0; i < sizeMapDefault.length; i++) {
            if (pt <= sizeMapDefault[i]) {
                return i + 1;
            }
        }
        return sizeMapDefault.length;
    }

    /** Los puntos que valen ese tamano de HTML, del 1 al 7. */
    public float getPointSize(int index) {
        if (index < 1) {
            index = 1;
        } else if (index > sizeMapDefault.length) {
            index = sizeMapDefault.length;
        }
        return sizeMapDefault[index - 1];
    }

    /**
     * Los puntos de un tamano escrito, absoluto o relativo al base.
     *
     * @throws NumberFormatException si no es un numero.
     */
    public float getPointSize(String size) {
        int relativo = 0;
        if (size.startsWith("+")) {
            relativo = Integer.parseInt(size.substring(1));
            return getPointSize(baseFontSize + relativo);
        }
        if (size.startsWith("-")) {
            relativo = Integer.parseInt(size.substring(1));
            return getPointSize(baseFontSize - relativo);
        }
        return getPointSize(Integer.parseInt(size));
    }

    /** Los puntos de un tamano de CSS, con o sin unidad. */
    private float tamanoEnPuntos(String v) {
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
     * El color que nombra ese texto, o nulo si no se entiende.
     *
     * <p>Acepta los dieciseis nombres de CSS sin distinguir mayusculas, un numero hexadecimal con
     * o sin <code>#</code>, y <code>rgb(r,g,b)</code> en minusculas.
     *
     * <p>Devolver nulo y no negro importa: quien pregunta necesita distinguir "pidieron negro" de
     * "no se pudo leer", porque en el segundo caso hay que dejar el color que ya estaba. La unica
     * excepcion es la cadena vacia, que da negro.
     *
     * <p>No se recortan los espacios. Parece una omision y no lo es: un <code>" red"</code> con un
     * espacio adelante no es un color valido en CSS, y aceptarlo taparia un error de la hoja.
     */
    public Color stringToColor(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return Color.black;
        }
        if (str.startsWith("rgb(")) {
            return leerRGB(str);
        }
        if (str.charAt(0) == '#') {
            return hexAColor(str);
        }
        Color nombrado = colores.get(str.toLowerCase(java.util.Locale.ROOT));
        if (nombrado != null) {
            return nombrado;
        }
        // Lo que no es un nombre se prueba como hexadecimal sin `#`; asi anda `ff0000`.
        return hexAColor(str);
    }

    /**
     * Un color escrito en hexadecimal.
     *
     * <p>Se toman a lo sumo seis digitos y se leen como un solo numero, asi que
     * <code>#ff00</code> es verde y no un error. Los de tres digitos se duplican: <code>#f00</code>
     * es <code>#ff0000</code>.
     */
    private static Color hexAColor(String value) {
        String digitos;
        if (value.startsWith("#")) {
            digitos = value.substring(1, Math.min(value.length(), 7));
        } else {
            digitos = value;
        }
        if (digitos.length() == 3) {
            digitos = "" + digitos.charAt(0) + digitos.charAt(0) + digitos.charAt(1)
                    + digitos.charAt(1) + digitos.charAt(2) + digitos.charAt(2);
        }
        try {
            return Color.decode("0x" + digitos);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Un color escrito como <code>rgb(r,g,b)</code>.
     *
     * <p>Los componentes que falten valen cero y los que se pasen se recortan a 0..255. Es a
     * proposito: un color mal escrito se muestra igual, en lugar de dejar la pagina sin color.
     */
    private static Color leerRGB(String string) {
        int[] indice = new int[1];
        indice[0] = 4;
        int rojo = componente(string, indice);
        int verde = componente(string, indice);
        int azul = componente(string, indice);
        return new Color(rojo, verde, azul);
    }

    /** El proximo numero de la cadena, recortado a 0..255; cero si no hay ninguno. */
    private static int componente(String string, int[] indice) {
        int largo = string.length();
        char c;
        while (indice[0] < largo && (c = string.charAt(indice[0])) != '-'
                && !Character.isDigit(c) && c != '.') {
            indice[0] = indice[0] + 1;
        }
        int desde = indice[0];
        if (desde < largo && string.charAt(indice[0]) == '-') {
            indice[0] = indice[0] + 1;
        }
        while (indice[0] < largo && Character.isDigit(string.charAt(indice[0]))) {
            indice[0] = indice[0] + 1;
        }
        if (indice[0] < largo && string.charAt(indice[0]) == '.') {
            indice[0] = indice[0] + 1;
            while (indice[0] < largo && Character.isDigit(string.charAt(indice[0]))) {
                indice[0] = indice[0] + 1;
            }
        }
        if (desde != indice[0]) {
            try {
                float valor = Float.parseFloat(string.substring(desde, indice[0]));
                if (indice[0] < largo && string.charAt(indice[0]) == '%') {
                    indice[0] = indice[0] + 1;
                    valor = valor * 255f / 100f;
                }
                return Math.min(255, Math.max(0, (int) valor));
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Los margenes y el fondo de un bloque.
     *
     * <p>En el JDK es una clase interna; aca es estatica y recibe la hoja, por los hallazgos #507 y
     * #508. La firma que queda no es publica, asi que no se ve desde afuera.
     */
    public static final class BoxPainter implements java.io.Serializable {

        private final AttributeSet a;
        private final StyleSheet hoja;

        BoxPainter(AttributeSet a, StyleSheet hoja) {
            this.a = a;
            this.hoja = hoja;
        }

        /** Cuanto margen deja de ese lado, en pixeles. */
        public float getInset(int side, View v) {
            CSS.Attribute clave;
            if (side == View.TOP) {
                clave = CSS.Attribute.MARGIN_TOP;
            } else if (side == View.BOTTOM) {
                clave = CSS.Attribute.MARGIN_BOTTOM;
            } else if (side == View.LEFT) {
                clave = CSS.Attribute.MARGIN_LEFT;
            } else {
                clave = CSS.Attribute.MARGIN_RIGHT;
            }
            Object o = a.getAttribute(clave);
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

        /** Pinta el fondo del bloque, si tiene color. */
        public void paint(Graphics g, float x, float y, float w, float h, View v) {
            Color fondo = hoja.getBackground(a);
            if (fondo != null) {
                g.setColor(fondo);
                g.fillRect((int) x, (int) y, (int) w, (int) h);
            }
        }
    }

    /**
     * La vineta o el numero de un renglon de lista.
     *
     * <p>El numero del renglon llega como parametro y no se guarda: la misma lista se dibuja muchas
     * veces y guardarlo obligaria a un pintor por renglon.
     */
    public static final class ListPainter implements java.io.Serializable {

        private final AttributeSet a;
        private final StyleSheet hoja;

        ListPainter(AttributeSet a, StyleSheet hoja) {
            this.a = a;
            this.hoja = hoja;
        }

        /** Dibuja la marca del renglon numero tal. */
        public void paint(Graphics g, float x, float y, float w, float h, View v, int item) {
            Object o = a.getAttribute(CSS.Attribute.LIST_STYLE_TYPE);
            String tipo = (o == null) ? "disc" : o.toString();
            g.setColor(hoja.getForeground(a));
            if ("none".equals(tipo)) {
                return;
            }
            if ("decimal".equals(tipo)) {
                g.drawString((item + 1) + ".", (int) x, (int) (y + h));
                return;
            }
            int d = (int) Math.max(4, h / 3);
            int cx = (int) x;
            int cy = (int) (y + (h - d) / 2);
            if ("circle".equals(tipo)) {
                g.drawOval(cx, cy, d, d);
            } else if ("square".equals(tipo)) {
                g.fillRect(cx, cy, d, d);
            } else {
                g.fillOval(cx, cy, d, d);
            }
        }
    }

    static {
        colores.put("black", new Color(0, 0, 0));
        colores.put("silver", new Color(192, 192, 192));
        colores.put("gray", new Color(128, 128, 128));
        colores.put("white", new Color(255, 255, 255));
        colores.put("maroon", new Color(128, 0, 0));
        colores.put("red", new Color(255, 0, 0));
        colores.put("purple", new Color(128, 0, 128));
        colores.put("fuchsia", new Color(255, 0, 255));
        colores.put("green", new Color(0, 128, 0));
        colores.put("lime", new Color(0, 255, 0));
        colores.put("olive", new Color(128, 128, 0));
        colores.put("yellow", new Color(255, 255, 0));
        colores.put("navy", new Color(0, 0, 128));
        colores.put("blue", new Color(0, 0, 255));
        colores.put("teal", new Color(0, 128, 128));
        colores.put("aqua", new Color(0, 255, 255));
    }
}
