package javax.swing.text.html;

import java.io.Serializable;
import java.util.Hashtable;

import javax.swing.text.AttributeSet;

/**
 * Las etiquetas y los atributos de HTML, como constantes.
 *
 * <h2>Por que constantes y no cadenas</h2>
 *
 * <p>Un documento HTML se guarda como atributos de un {@link javax.swing.text.Document}, y las
 * claves de esos atributos son estos objetos. Que sean objetos unicos y no cadenas tiene dos
 * consecuencias que se usan en todas partes: se comparan con <code>==</code>, que es barato y no
 * confunde mayusculas, y llevan colgada informacion que una cadena no podria llevar.
 *
 * <p>Esa informacion es lo que hace {@link Tag#isBlock} y {@link Tag#breaksFlow}: saber si una
 * etiqueta arma un bloque o corta la linea no se deduce de su nombre, hay que saberlo. Aca esta
 * anotado etiqueta por etiqueta, con los mismos valores que el JDK.
 *
 * <h2>Las etiquetas que no existen en HTML</h2>
 *
 * <p>Tres de las constantes no son etiquetas de verdad: {@link Tag#IMPLIED} es el parrafo que el
 * analizador inventa cuando hay texto suelto, {@link Tag#CONTENT} marca el texto mismo y
 * {@link Tag#COMMENT} un comentario. No aparecen en {@link #getAllTags} justamente porque no se
 * pueden escribir en un documento; existen porque el arbol de elementos las necesita.
 */
public class HTML {

    /** El valor que se le pone a un atributo escrito sin valor, como <code>checked</code>. */
    public static final String NULL_ATTRIBUTE_VALUE = "#DEFAULT";

    private static final Hashtable<String, Tag> tagHashtable = new Hashtable<String, Tag>();
    private static final Hashtable<String, Attribute> attHashtable =
            new Hashtable<String, Attribute>();

    /** Nada que construir: la clase es solo un lugar donde viven las constantes. */
    public HTML() {
    }

    /**
     * Todas las etiquetas que se pueden escribir en un documento.
     *
     * <p>Devuelve una copia: si devolviera el arreglo interno, quien lo recibiera podria cambiar
     * las etiquetas de todos los documentos del programa.
     */
    public static Tag[] getAllTags() {
        Tag[] tags = new Tag[Tag.allTags.length];
        System.arraycopy(Tag.allTags, 0, tags, 0, Tag.allTags.length);
        return tags;
    }

    /** La etiqueta con ese nombre, o nulo si no se conoce. */
    public static Tag getTag(String tagName) {
        Object t = tagHashtable.get(tagName);
        return (t == null ? null : (Tag) t);
    }

    /**
     * El valor entero de un atributo, o el de reserva si no esta o no es un numero.
     *
     * <p>Devolver el de reserva y no fallar es a proposito: un HTML mal escrito es lo normal, y un
     * <code>width="tres"</code> no deberia impedir mostrar la pagina.
     */
    public static int getIntegerAttributeValue(AttributeSet attr, Attribute key, int def) {
        int value = def;
        String istr = (String) attr.getAttribute(key);
        if (istr != null) {
            try {
                value = Integer.valueOf(istr).intValue();
            } catch (NumberFormatException e) {
                value = def;
            }
        }
        return value;
    }

    /** Todos los nombres de atributo conocidos; tambien es una copia. */
    public static Attribute[] getAllAttributeKeys() {
        Attribute[] attributes = new Attribute[Attribute.allAttributes.length];
        System.arraycopy(Attribute.allAttributes, 0, attributes, 0,
                Attribute.allAttributes.length);
        return attributes;
    }

    /** El atributo con ese nombre, o nulo si no se conoce. */
    public static Attribute getAttributeKey(String attName) {
        Object a = attHashtable.get(attName);
        if (a == null) {
            return null;
        }
        return (Attribute) a;
    }

    /**
     * Una etiqueta de HTML.
     *
     * <p>Las constantes de aca son las unicas instancias que hay de cada etiqueta, asi que
     * compararlas con <code>==</code> es correcto y es lo que hace toda la biblioteca. La excepcion
     * es {@link UnknownTag}, que se crea una por cada etiqueta desconocida y por eso si define
     * {@code equals}.
     */
    public static class Tag {

        String name;
        boolean breakTag;
        boolean blockTag;
        boolean unknown;

        /** Una etiqueta sin nombre; la usan las subclases que lo ponen despues. */
        public Tag() {
        }

        /** Una etiqueta con ese nombre, que no arma bloque ni corta la linea. */
        protected Tag(String id) {
            name = id;
        }

        /** Una etiqueta con ese nombre y ese comportamiento. */
        protected Tag(String id, boolean causesBreak, boolean isBlock) {
            name = id;
            breakTag = causesBreak;
            blockTag = isBlock;
        }

        /** Si la etiqueta arma un bloque, como <code>p</code> o <code>div</code>. */
        public boolean isBlock() {
            return blockTag;
        }

        /** Si corta la linea. Un bloque siempre corta; <code>br</code> corta sin ser bloque. */
        public boolean breaksFlow() {
            return breakTag;
        }

        /**
         * Si adentro se respetan los espacios y los fines de linea.
         *
         * <p>Se pregunta por identidad y no por un campo: son exactamente dos etiquetas, y una
         * etiqueta que alguien defina con el mismo nombre no cambia como se muestra el texto.
         */
        public boolean isPreformatted() {
            return (this == PRE || this == TEXTAREA);
        }

        /** Si la etiqueta hace de parrafo en el arbol de elementos. */
        boolean isParagraph() {
            return (this == P || this == IMPLIED || this == DT || this == H1 || this == H2
                    || this == H3 || this == H4 || this == H5 || this == H6);
        }

        public String toString() {
            return name;
        }

        public static final Tag A = new Tag("a");
        public static final Tag ADDRESS = new Tag("address");
        public static final Tag APPLET = new Tag("applet");
        public static final Tag AREA = new Tag("area");
        public static final Tag B = new Tag("b");
        public static final Tag BASE = new Tag("base");
        public static final Tag BASEFONT = new Tag("basefont");
        public static final Tag BIG = new Tag("big");
        public static final Tag BLOCKQUOTE = new Tag("blockquote", true, true);
        public static final Tag BODY = new Tag("body", true, true);
        public static final Tag BR = new Tag("br", true, false);
        public static final Tag CAPTION = new Tag("caption");
        public static final Tag CENTER = new Tag("center", true, false);
        public static final Tag CITE = new Tag("cite");
        public static final Tag CODE = new Tag("code");
        public static final Tag DD = new Tag("dd", true, true);
        public static final Tag DFN = new Tag("dfn");
        public static final Tag DIR = new Tag("dir", true, true);
        public static final Tag DIV = new Tag("div", true, true);
        public static final Tag DL = new Tag("dl", true, true);
        public static final Tag DT = new Tag("dt", true, true);
        public static final Tag EM = new Tag("em");
        public static final Tag FONT = new Tag("font");
        public static final Tag FORM = new Tag("form", true, false);
        public static final Tag FRAME = new Tag("frame");
        public static final Tag FRAMESET = new Tag("frameset");
        public static final Tag H1 = new Tag("h1", true, true);
        public static final Tag H2 = new Tag("h2", true, true);
        public static final Tag H3 = new Tag("h3", true, true);
        public static final Tag H4 = new Tag("h4", true, true);
        public static final Tag H5 = new Tag("h5", true, true);
        public static final Tag H6 = new Tag("h6", true, true);
        public static final Tag HEAD = new Tag("head", true, true);
        public static final Tag HR = new Tag("hr", true, false);
        public static final Tag HTML = new Tag("html", true, false);
        public static final Tag I = new Tag("i");
        public static final Tag IMG = new Tag("img");
        public static final Tag INPUT = new Tag("input");
        public static final Tag ISINDEX = new Tag("isindex", true, false);
        public static final Tag KBD = new Tag("kbd");
        public static final Tag LI = new Tag("li", true, true);
        public static final Tag LINK = new Tag("link");
        public static final Tag MAP = new Tag("map");
        public static final Tag MENU = new Tag("menu", true, true);
        public static final Tag META = new Tag("meta");
        static final Tag NOBR = new Tag("nobr");
        public static final Tag NOFRAMES = new Tag("noframes", true, true);
        public static final Tag OBJECT = new Tag("object");
        public static final Tag OL = new Tag("ol", true, true);
        public static final Tag OPTION = new Tag("option");
        public static final Tag P = new Tag("p", true, true);
        public static final Tag PARAM = new Tag("param");
        public static final Tag PRE = new Tag("pre", true, true);
        public static final Tag SAMP = new Tag("samp");
        public static final Tag SCRIPT = new Tag("script");
        public static final Tag SELECT = new Tag("select");
        public static final Tag SMALL = new Tag("small");
        public static final Tag SPAN = new Tag("span");
        public static final Tag STRIKE = new Tag("strike");
        public static final Tag S = new Tag("s");
        public static final Tag STRONG = new Tag("strong");
        public static final Tag STYLE = new Tag("style");
        public static final Tag SUB = new Tag("sub");
        public static final Tag SUP = new Tag("sup");
        public static final Tag TABLE = new Tag("table", false, true);
        public static final Tag TD = new Tag("td", true, true);
        public static final Tag TEXTAREA = new Tag("textarea");
        public static final Tag TH = new Tag("th", true, true);
        public static final Tag TITLE = new Tag("title", true, true);
        public static final Tag TR = new Tag("tr", false, true);
        public static final Tag TT = new Tag("tt");
        public static final Tag U = new Tag("u");
        public static final Tag UL = new Tag("ul", true, true);
        public static final Tag VAR = new Tag("var");

        /** El parrafo que el analizador inventa cuando encuentra texto suelto. */
        public static final Tag IMPLIED = new Tag("p-implied");

        /** El texto mismo. */
        public static final Tag CONTENT = new Tag("content");

        /** Un comentario. */
        public static final Tag COMMENT = new Tag("comment");

        static final Tag[] allTags = {
            A, ADDRESS, APPLET, AREA, B, BASE, BASEFONT, BIG,
            BLOCKQUOTE, BODY, BR, CAPTION, CENTER, CITE, CODE,
            DD, DFN, DIR, DIV, DL, DT, EM, FONT, FORM,
            FRAME, FRAMESET, H1, H2, H3, H4, H5, H6, HEAD,
            HR, HTML, I, IMG, INPUT, ISINDEX, KBD, LI,
            LINK, MAP, MENU, META, NOBR, NOFRAMES, OBJECT, OL,
            OPTION, P, PARAM, PRE, SAMP, SCRIPT, SELECT,
            SMALL, SPAN, STRIKE, S, STRONG, STYLE, SUB,
            SUP, TABLE, TD, TEXTAREA, TH, TITLE, TR,
            TT, U, UL, VAR
        };
    }

    /**
     * Una etiqueta que no esta en la lista.
     *
     * <p>Se crea una por cada nombre desconocido que aparezca, asi que dos etiquetas desconocidas
     * con el mismo nombre son objetos distintos. Por eso define {@code equals} y {@code hashCode}:
     * sin ellos, la comparacion por identidad que sirve para las conocidas fallaria justo aca.
     */
    public static class UnknownTag extends Tag implements Serializable {

        /** Una etiqueta desconocida con ese nombre. */
        public UnknownTag(String id) {
            super(id);
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof UnknownTag) {
                return toString().equals(obj.toString());
            }
            return false;
        }
    }

    /**
     * El nombre de un atributo de HTML.
     *
     * <p>Es final y su constructor no es publico: la lista de atributos es cerrada. Un atributo que
     * no este aca se guarda con su nombre como cadena, no como uno de estos.
     */
    public static final class Attribute {

        private String name;

        Attribute(String id) {
            name = id;
        }

        public String toString() {
            return name;
        }

        public static final Attribute SIZE = new Attribute("size");
        public static final Attribute COLOR = new Attribute("color");
        public static final Attribute CLEAR = new Attribute("clear");
        public static final Attribute BACKGROUND = new Attribute("background");
        public static final Attribute BGCOLOR = new Attribute("bgcolor");
        public static final Attribute TEXT = new Attribute("text");
        public static final Attribute LINK = new Attribute("link");
        public static final Attribute VLINK = new Attribute("vlink");
        public static final Attribute ALINK = new Attribute("alink");
        public static final Attribute WIDTH = new Attribute("width");
        public static final Attribute HEIGHT = new Attribute("height");
        public static final Attribute ALIGN = new Attribute("align");
        public static final Attribute NAME = new Attribute("name");
        public static final Attribute HREF = new Attribute("href");
        public static final Attribute REL = new Attribute("rel");
        public static final Attribute REV = new Attribute("rev");
        public static final Attribute TITLE = new Attribute("title");
        public static final Attribute TARGET = new Attribute("target");
        public static final Attribute SHAPE = new Attribute("shape");
        public static final Attribute COORDS = new Attribute("coords");
        public static final Attribute ISMAP = new Attribute("ismap");
        public static final Attribute NOHREF = new Attribute("nohref");
        public static final Attribute ALT = new Attribute("alt");
        public static final Attribute ID = new Attribute("id");
        public static final Attribute SRC = new Attribute("src");
        public static final Attribute HSPACE = new Attribute("hspace");
        public static final Attribute VSPACE = new Attribute("vspace");
        public static final Attribute USEMAP = new Attribute("usemap");
        public static final Attribute LOWSRC = new Attribute("lowsrc");
        public static final Attribute CODEBASE = new Attribute("codebase");
        public static final Attribute CODE = new Attribute("code");
        public static final Attribute ARCHIVE = new Attribute("archive");
        public static final Attribute VALUE = new Attribute("value");
        public static final Attribute VALUETYPE = new Attribute("valuetype");
        public static final Attribute TYPE = new Attribute("type");
        public static final Attribute CLASS = new Attribute("class");
        public static final Attribute STYLE = new Attribute("style");
        public static final Attribute LANG = new Attribute("lang");
        public static final Attribute FACE = new Attribute("face");
        public static final Attribute DIR = new Attribute("dir");
        public static final Attribute DECLARE = new Attribute("declare");
        public static final Attribute CLASSID = new Attribute("classid");
        public static final Attribute DATA = new Attribute("data");
        public static final Attribute CODETYPE = new Attribute("codetype");
        public static final Attribute STANDBY = new Attribute("standby");
        public static final Attribute BORDER = new Attribute("border");
        public static final Attribute SHAPES = new Attribute("shapes");
        public static final Attribute NOSHADE = new Attribute("noshade");
        public static final Attribute COMPACT = new Attribute("compact");
        public static final Attribute START = new Attribute("start");
        public static final Attribute ACTION = new Attribute("action");
        public static final Attribute METHOD = new Attribute("method");
        public static final Attribute ENCTYPE = new Attribute("enctype");
        public static final Attribute CHECKED = new Attribute("checked");
        public static final Attribute MAXLENGTH = new Attribute("maxlength");
        public static final Attribute MULTIPLE = new Attribute("multiple");
        public static final Attribute SELECTED = new Attribute("selected");
        public static final Attribute ROWS = new Attribute("rows");
        public static final Attribute COLS = new Attribute("cols");
        public static final Attribute DUMMY = new Attribute("dummy");
        public static final Attribute CELLSPACING = new Attribute("cellspacing");
        public static final Attribute CELLPADDING = new Attribute("cellpadding");
        public static final Attribute VALIGN = new Attribute("valign");
        public static final Attribute HALIGN = new Attribute("halign");
        public static final Attribute NOWRAP = new Attribute("nowrap");
        public static final Attribute ROWSPAN = new Attribute("rowspan");
        public static final Attribute COLSPAN = new Attribute("colspan");
        public static final Attribute PROMPT = new Attribute("prompt");
        public static final Attribute HTTPEQUIV = new Attribute("http-equiv");
        public static final Attribute CONTENT = new Attribute("content");
        public static final Attribute LANGUAGE = new Attribute("language");
        public static final Attribute VERSION = new Attribute("version");
        public static final Attribute N = new Attribute("n");
        public static final Attribute FRAMEBORDER = new Attribute("frameborder");
        public static final Attribute MARGINWIDTH = new Attribute("marginwidth");
        public static final Attribute MARGINHEIGHT = new Attribute("marginheight");
        public static final Attribute SCROLLING = new Attribute("scrolling");
        public static final Attribute NORESIZE = new Attribute("noresize");
        public static final Attribute MEDIA = new Attribute("media");

        /** Marca que un elemento del documento es una etiqueta de cierre. */
        public static final Attribute ENDTAG = new Attribute("endtag");

        /** Guarda el texto de un comentario. */
        public static final Attribute COMMENT = new Attribute("comment");

        static final Attribute[] allAttributes = {
            FACE, COMMENT, SIZE, COLOR, CLEAR, BACKGROUND, BGCOLOR,
            TEXT, LINK, VLINK, ALINK, WIDTH, HEIGHT, ALIGN,
            NAME, HREF, REL, REV, TITLE, TARGET, SHAPE,
            COORDS, ISMAP, NOHREF, ALT, ID, SRC, HSPACE,
            VSPACE, USEMAP, LOWSRC, CODEBASE, CODE, ARCHIVE,
            VALUE, VALUETYPE, TYPE, CLASS, STYLE, LANG, DIR,
            DECLARE, CLASSID, DATA, CODETYPE, STANDBY, BORDER,
            SHAPES, NOSHADE, COMPACT, START, ACTION, METHOD,
            ENCTYPE, CHECKED, MAXLENGTH, MULTIPLE, SELECTED,
            ROWS, COLS, DUMMY, CELLSPACING, CELLPADDING,
            VALIGN, HALIGN, NOWRAP, ROWSPAN, COLSPAN, PROMPT,
            HTTPEQUIV, CONTENT, LANGUAGE, VERSION, N,
            FRAMEBORDER, MARGINWIDTH, MARGINHEIGHT, SCROLLING,
            NORESIZE, MEDIA, ENDTAG
        };
    }

    static {
        for (int i = 0; i < Tag.allTags.length; i++) {
            tagHashtable.put(Tag.allTags[i].toString(), Tag.allTags[i]);
        }
        for (int i = 0; i < Attribute.allAttributes.length; i++) {
            attHashtable.put(Attribute.allAttributes[i].toString(), Attribute.allAttributes[i]);
        }
    }
}
