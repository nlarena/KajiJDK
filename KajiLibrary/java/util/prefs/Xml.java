package java.util.prefs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

// El formato de intercambio: lo que escriben `exportNode`/`exportSubtree` y lo que lee
// `importPreferences`.
//
// POR QUE HAY UN ANALIZADOR DE XML ACA ADENTRO. Escribir XML es concatenar cadenas; leerlo no. En
// este arbol no hay `org.w3c.dom`, no hay `org.xml.sax` y no hay `javax.xml.parsers` --lo unico de
// XML que existe es `javax.xml.transform`, que son interfaces sin implementacion de XSLT-- asi que
// `importPreferences` o traia su propio analizador o quedaba afuera.
//
// Trae el suyo, y se puede porque **el DTD de preferencias no tiene contenido de texto**:
//
//     <!ELEMENT preferences (root)>      <!ATTLIST preferences EXTERNAL_XML_VERSION CDATA "0.0">
//     <!ELEMENT root (map, node*)>       <!ATTLIST root type (system|user) #REQUIRED>
//     <!ELEMENT node (map, node*)>       <!ATTLIST node name CDATA #REQUIRED>
//     <!ELEMENT map (entry*)>
//     <!ELEMENT entry EMPTY>             <!ATTLIST entry key CDATA #REQUIRED value CDATA #REQUIRED>
//
// Todos los elementos son de contenido *solo elementos* o vacios, no hay declaraciones de entidades
// propias y no hay espacios de nombres. Eso deja la gramatica que hay que cubrir en: declaracion,
// DOCTYPE, comentarios, instrucciones de proceso, etiquetas con atributos, las cinco entidades
// predefinidas y las referencias numericas. Es una lista cerrada, y por eso el analizador de abajo
// es **completo** para este DTD y no una aproximacion que anda con los archivos que escribimos
// nosotros. Cualquier cosa que caiga fuera --texto suelto donde no puede haberlo, una etiqueta sin
// cerrar, una entidad desconocida-- sale por `InvalidPreferencesFormatException`, que es
// exactamente lo que el contrato pide.
//
// LO UNICO QUE NO SE PUEDE EXPORTAR son los valores con caracteres de control C0 distintos de
// tabulador, salto de linea y retorno: XML 1.0 no los admite **ni siquiera** como referencia
// numerica, asi que no hay documento valido que los contenga. La implementacion de referencia los
// escribe igual y produce un archivo que despues nadie puede leer; aca se tira
// `IllegalArgumentException` al exportar, porque un archivo que no se puede volver a importar es
// peor que una excepcion.
//
// Los otros tres si se escriben, pero **como referencia numerica** y no crudos: un tabulador o un
// salto de linea literales dentro de un atributo los normaliza a espacio cualquier analizador que
// cumpla la norma, y el valor volveria distinto de como salio.
final class Xml {

    private static final String VERSION = "1.0";
    private static final String DTD = "http://java.sun.com/dtd/preferences.dtd";

    private Xml() {
    }

    // ---- escribir ---------------------------------------------------------------------------

    static void exportar(OutputStream os, Preferences p, boolean subarbol)
            throws IOException, BackingStoreException {
        if (((AbstractPreferences) p).isRemoved()) {
            throw new IllegalStateException("Node has been removed");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        sb.append("<!DOCTYPE preferences SYSTEM \"").append(DTD).append("\">\n");
        sb.append("<preferences EXTERNAL_XML_VERSION=\"").append(VERSION).append("\">\n");
        sb.append("  <root type=\"").append(p.isUserNode() ? "user" : "system").append("\">\n");

        // La cadena de ancestros desde la raiz hasta `p` se escribe entera, con el `<map/>` vacio de
        // cada uno. Es lo que hace que el documento sea autosuficiente: quien lo importa recrea la
        // ruta completa sin tener que saber de donde salio.
        ArrayList<Preferences> ancestros = new ArrayList<Preferences>();
        // Un `while` y no el `for` de dos variables que pedia el caso: nuestro javac no deja en
        // alcance las variables de un `for` con varios declaradores (ver
        // scratchpad/zzprefs/ForVariosDeclaradores.java).
        Preferences trepando = p;
        while (trepando.parent() != null) {
            ancestros.add(trepando);
            trepando = trepando.parent();
        }
        int sangria = 2;
        for (int i = ancestros.size() - 1; i >= 0; i--) {
            indentar(sb, sangria).append("<map/>\n");
            indentar(sb, sangria).append("<node name=\"");
            escapar(sb, ancestros.get(i).name());
            sb.append("\">\n");
            sangria++;
        }
        volcar(sb, p, subarbol, sangria);
        while (sangria > 2) {
            sangria--;
            indentar(sb, sangria).append("</node>\n");
        }
        sb.append("  </root>\n");
        sb.append("</preferences>\n");
        os.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        os.flush();
    }

    private static void volcar(StringBuilder sb, Preferences p, boolean subarbol, int sangria)
            throws BackingStoreException {
        String[] claves;
        String[] nombresDeHijos = null;
        Preferences[] hijos = null;
        // Las claves y la lista de hijos se sacan bajo el candado del nodo para que el documento sea
        // una foto y no una mezcla de dos momentos; el recorrido recursivo se hace despues, fuera.
        synchronized (((AbstractPreferences) p).lock) {
            if (((AbstractPreferences) p).isRemoved()) {
                return;
            }
            claves = p.keys();
            if (subarbol) {
                nombresDeHijos = p.childrenNames();
                hijos = new Preferences[nombresDeHijos.length];
                for (int i = 0; i < nombresDeHijos.length; i++) {
                    hijos[i] = p.node(nombresDeHijos[i]);
                }
            }
        }
        if (claves.length == 0) {
            indentar(sb, sangria).append("<map/>\n");
        } else {
            indentar(sb, sangria).append("<map>\n");
            for (int i = 0; i < claves.length; i++) {
                indentar(sb, sangria + 1).append("<entry key=\"");
                escapar(sb, claves[i]);
                sb.append("\" value=\"");
                escapar(sb, p.get(claves[i], ""));
                sb.append("\"/>\n");
            }
            indentar(sb, sangria).append("</map>\n");
        }
        if (subarbol) {
            for (int i = 0; i < nombresDeHijos.length; i++) {
                indentar(sb, sangria).append("<node name=\"");
                escapar(sb, nombresDeHijos[i]);
                sb.append("\">\n");
                volcar(sb, hijos[i], true, sangria + 1);
                indentar(sb, sangria).append("</node>\n");
            }
        }
    }

    private static StringBuilder indentar(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append("  ");
        }
        return sb;
    }

    private static void escapar(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (c == '\t' || c == '\n' || c == '\r') {
                sb.append("&#").append((int) c).append(';');
            } else if (c < 0x20) {
                throw new IllegalArgumentException(
                        "XML 1.0 no admite el caracter de control U+"
                                + Integer.toHexString(c) + ", ni siquiera escapado");
            } else {
                sb.append(c);
            }
        }
    }

    // ---- leer -------------------------------------------------------------------------------

    static void importar(InputStream is) throws IOException, InvalidPreferencesFormatException {
        String texto = decodificar(leerTodo(is));
        Elem doc;
        try {
            doc = new Analizador(texto).documento();
        } catch (InvalidPreferencesFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            // Un indice fuera de rango del analizador es un documento roto, no un error nuestro
            // que el programa deba distinguir.
            throw new InvalidPreferencesFormatException(e);
        }
        if (!doc.tag.equals("preferences")) {
            throw new InvalidPreferencesFormatException(
                    "el elemento raiz es <" + doc.tag + "> y no <preferences>");
        }
        String v = doc.attr("EXTERNAL_XML_VERSION");
        if (v.length() != 0 && v.compareTo(VERSION) > 0) {
            throw new InvalidPreferencesFormatException(
                    "Exported preferences file format version " + v
                            + " is not supported. This java installation can read versions "
                            + VERSION + " or older.");
        }
        if (doc.hijos.size() != 1 || !doc.hijos.get(0).tag.equals("root")) {
            throw new InvalidPreferencesFormatException("<preferences> debe tener un unico <root>");
        }
        Elem raiz = doc.hijos.get(0);
        String tipo = raiz.attr("type");
        Preferences destino;
        if (tipo.equals("user")) {
            destino = Preferences.userRoot();
        } else if (tipo.equals("system")) {
            destino = Preferences.systemRoot();
        } else {
            throw new InvalidPreferencesFormatException(
                    "<root type> vale \"" + tipo + "\" y tiene que ser \"user\" o \"system\"");
        }
        importarSubarbol(destino, raiz);
    }

    private static void importarSubarbol(Preferences destino, Elem xml)
            throws InvalidPreferencesFormatException {
        if (xml.hijos.isEmpty() || !xml.hijos.get(0).tag.equals("map")) {
            throw new InvalidPreferencesFormatException(
                    "<" + xml.tag + "> tiene que empezar con <map>");
        }
        Elem mapa = xml.hijos.get(0);
        for (int i = 0; i < mapa.hijos.size(); i++) {
            Elem e = mapa.hijos.get(i);
            if (!e.tag.equals("entry")) {
                throw new InvalidPreferencesFormatException(
                        "<map> solo admite <entry>, no <" + e.tag + ">");
            }
            if (!e.attrs.containsKey("key") || !e.attrs.containsKey("value")) {
                throw new InvalidPreferencesFormatException("<entry> necesita `key` y `value`");
            }
            destino.put(e.attr("key"), e.attr("value"));
        }
        for (int i = 1; i < xml.hijos.size(); i++) {
            Elem e = xml.hijos.get(i);
            if (!e.tag.equals("node")) {
                throw new InvalidPreferencesFormatException(
                        "despues del <map> solo van <node>, no <" + e.tag + ">");
            }
            if (!e.attrs.containsKey("name")) {
                throw new InvalidPreferencesFormatException("<node> necesita `name`");
            }
            importarSubarbol(destino.node(e.attr("name")), e);
        }
    }

    private static byte[] leerTodo(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // El documento dice en que codificacion esta, pero para leer esa declaracion ya hay que
    // decodificarlo. La salida del circulo es que toda codificacion admitida por XML es o compatible
    // con ASCII --y entonces la declaracion se lee bien decodificando como UTF-8-- o UTF-16, que se
    // reconoce por la marca de orden de bytes.
    private static String decodificar(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) {
            return new String(b, java.nio.charset.StandardCharsets.UTF_16BE).substring(1);
        }
        if (b.length >= 2 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xfe) {
            return new String(b, java.nio.charset.StandardCharsets.UTF_16LE).substring(1);
        }
        String s = new String(b, java.nio.charset.StandardCharsets.UTF_8);
        if (s.length() > 0 && s.charAt(0) == '﻿') {
            s = s.substring(1);
        }
        String enc = codificacionDeclarada(s);
        if (enc != null && !enc.equalsIgnoreCase("UTF-8") && !enc.equalsIgnoreCase("UTF8")) {
            try {
                return new String(b, enc);
            } catch (Exception e) {
                // Codificacion que la VM no conoce: se sigue con UTF-8 y el analizador dira que el
                // documento no se entiende, que es la verdad.
            }
        }
        return s;
    }

    private static String codificacionDeclarada(String s) {
        if (!s.startsWith("<?xml")) {
            return null;
        }
        int fin = s.indexOf("?>");
        if (fin < 0) {
            return null;
        }
        String decl = s.substring(0, fin);
        int i = decl.indexOf("encoding");
        if (i < 0) {
            return null;
        }
        int comilla = -1;
        for (int j = i + 8; j < decl.length(); j++) {
            char c = decl.charAt(j);
            if (c == '"' || c == '\'') {
                comilla = j;
                break;
            }
        }
        if (comilla < 0) {
            return null;
        }
        int cierre = decl.indexOf(decl.charAt(comilla), comilla + 1);
        return cierre < 0 ? null : decl.substring(comilla + 1, cierre);
    }

    // ---- el arbol que sale del analizador ----------------------------------------------------

    private static final class Elem {
        final String tag;
        final HashMap<String, String> attrs = new HashMap<String, String>();
        final ArrayList<Elem> hijos = new ArrayList<Elem>();

        Elem(String tag) {
            this.tag = tag;
        }

        String attr(String n) {
            String v = attrs.get(n);
            return v == null ? "" : v;
        }
    }

    // ---- el analizador -----------------------------------------------------------------------

    private static final class Analizador {

        private final String s;
        private int i;

        Analizador(String s) {
            this.s = s;
        }

        Elem documento() throws InvalidPreferencesFormatException {
            prologo();
            Elem raiz = elemento();
            prologo(); // comentarios y espacios despues del elemento raiz
            if (i < s.length()) {
                error("sobra texto despues del elemento raiz");
            }
            return raiz;
        }

        // Todo lo que puede haber alrededor del elemento raiz: espacios, la declaracion XML,
        // el DOCTYPE, comentarios e instrucciones de proceso.
        private void prologo() throws InvalidPreferencesFormatException {
            while (true) {
                espacios();
                if (i + 1 >= s.length() || s.charAt(i) != '<') {
                    return;
                }
                char c = s.charAt(i + 1);
                if (c == '?') {
                    saltarHasta("?>");
                } else if (c == '!') {
                    if (s.startsWith("<!--", i)) {
                        saltarHasta("-->");
                    } else if (s.startsWith("<!DOCTYPE", i)) {
                        doctype();
                    } else {
                        error("no se esperaba `<!` aca");
                    }
                } else {
                    return;
                }
            }
        }

        // El DOCTYPE se saltea entero: el analizador no valida contra el DTD --de eso se encarga
        // `importarSubarbol`, que exige la forma exacta que el DTD describe-- pero **si** tiene que
        // saber donde termina, y eso no es "el proximo `>`": el subconjunto interno entre corchetes
        // puede tener todos los que quiera.
        private void doctype() throws InvalidPreferencesFormatException {
            i += "<!DOCTYPE".length();
            int corchetes = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '"' || c == '\'') {
                    int fin = s.indexOf(c, i + 1);
                    if (fin < 0) {
                        error("comilla sin cerrar en el DOCTYPE");
                    }
                    i = fin + 1;
                    continue;
                }
                if (c == '[') {
                    corchetes++;
                } else if (c == ']') {
                    corchetes--;
                } else if (c == '>' && corchetes == 0) {
                    i++;
                    return;
                }
                i++;
            }
            error("DOCTYPE sin cerrar");
        }

        private Elem elemento() throws InvalidPreferencesFormatException {
            esperar('<');
            String tag = nombre();
            Elem e = new Elem(tag);
            while (true) {
                espacios();
                if (i >= s.length()) {
                    error("etiqueta <" + tag + "> sin cerrar");
                }
                char c = s.charAt(i);
                if (c == '>') {
                    i++;
                    break;
                }
                if (c == '/') {
                    i++;
                    esperar('>');
                    return e; // <tag/>
                }
                String n = nombre();
                espacios();
                esperar('=');
                espacios();
                if (e.attrs.put(n, valorDeAtributo()) != null) {
                    error("el atributo `" + n + "` esta dos veces en <" + tag + ">");
                }
            }
            contenido(e);
            return e;
        }

        private void contenido(Elem e) throws InvalidPreferencesFormatException {
            while (true) {
                if (i >= s.length()) {
                    error("falta </" + e.tag + ">");
                }
                char c = s.charAt(i);
                if (c == '<') {
                    if (s.startsWith("<!--", i)) {
                        saltarHasta("-->");
                    } else if (s.startsWith("<![CDATA[", i)) {
                        int fin = s.indexOf("]]>", i);
                        if (fin < 0) {
                            error("CDATA sin cerrar");
                        }
                        exigirEnBlanco(s.substring(i + 9, fin), e.tag);
                        i = fin + 3;
                    } else if (s.startsWith("<?", i)) {
                        saltarHasta("?>");
                    } else if (s.startsWith("</", i)) {
                        i += 2;
                        String cierre = nombre();
                        if (!cierre.equals(e.tag)) {
                            error("se abrio <" + e.tag + "> y se cerro </" + cierre + ">");
                        }
                        espacios();
                        esperar('>');
                        return;
                    } else {
                        e.hijos.add(elemento());
                    }
                } else if (c == '&') {
                    // Una referencia en el contenido tiene que resolver a espacio en blanco: el DTD
                    // no admite texto en ningun elemento.
                    StringBuilder sb = new StringBuilder();
                    referencia(sb);
                    exigirEnBlanco(sb.toString(), e.tag);
                } else if (esEspacio(c)) {
                    i++;
                } else {
                    error("<" + e.tag + "> no admite texto, y hay `" + c + "`");
                }
            }
        }

        private void exigirEnBlanco(String t, String tag) throws InvalidPreferencesFormatException {
            for (int k = 0; k < t.length(); k++) {
                if (!esEspacio(t.charAt(k))) {
                    error("<" + tag + "> no admite texto, y hay `" + t.trim() + "`");
                }
            }
        }

        private String valorDeAtributo() throws InvalidPreferencesFormatException {
            if (i >= s.length()) {
                error("falta el valor del atributo");
            }
            char comilla = s.charAt(i);
            if (comilla != '"' && comilla != '\'') {
                error("el valor de un atributo va entre comillas");
            }
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) {
                    error("valor de atributo sin cerrar");
                }
                char c = s.charAt(i);
                if (c == comilla) {
                    i++;
                    return sb.toString();
                }
                if (c == '<') {
                    error("un `<` crudo no puede estar en el valor de un atributo");
                }
                if (c == '&') {
                    referencia(sb);
                } else if (c == '\t' || c == '\n' || c == '\r') {
                    // Normalizacion de valores de atributo, tal cual la pide XML 1.0. Por eso el
                    // exportador escribe estos tres como referencia numerica: crudos volverian
                    // convertidos en espacio.
                    sb.append(' ');
                    i++;
                } else {
                    sb.append(c);
                    i++;
                }
            }
        }

        private void referencia(StringBuilder sb) throws InvalidPreferencesFormatException {
            int fin = s.indexOf(';', i);
            if (fin < 0) {
                error("referencia sin `;`");
            }
            String r = s.substring(i + 1, fin);
            i = fin + 1;
            if (r.length() == 0) {
                error("referencia vacia");
            }
            if (r.charAt(0) == '#') {
                int cp;
                try {
                    cp = (r.length() > 1 && (r.charAt(1) == 'x' || r.charAt(1) == 'X'))
                            ? Integer.parseInt(r.substring(2), 16)
                            : Integer.parseInt(r.substring(1));
                } catch (NumberFormatException e) {
                    error("referencia numerica invalida `&" + r + ";`");
                    return;
                }
                if (cp < 0 || cp > 0x10ffff) {
                    error("referencia numerica fuera de rango `&" + r + ";`");
                }
                sb.appendCodePoint(cp);
                return;
            }
            if (r.equals("lt")) {
                sb.append('<');
            } else if (r.equals("gt")) {
                sb.append('>');
            } else if (r.equals("amp")) {
                sb.append('&');
            } else if (r.equals("quot")) {
                sb.append('"');
            } else if (r.equals("apos")) {
                sb.append('\'');
            } else {
                // Sin declaraciones de entidades no hay forma de saber que vale: inventar un valor
                // seria peor que decir que no se entiende.
                error("entidad desconocida `&" + r + ";`");
            }
        }

        private String nombre() throws InvalidPreferencesFormatException {
            int inicio = i;
            while (i < s.length() && esDeNombre(s.charAt(i))) {
                i++;
            }
            if (i == inicio) {
                error("se esperaba un nombre");
            }
            return s.substring(inicio, i);
        }

        private static boolean esDeNombre(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == ':' || c > 0x7f;
        }

        private static boolean esEspacio(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r';
        }

        private void espacios() {
            while (i < s.length() && esEspacio(s.charAt(i))) {
                i++;
            }
        }

        private void esperar(char c) throws InvalidPreferencesFormatException {
            if (i >= s.length() || s.charAt(i) != c) {
                error("se esperaba `" + c + "`");
            }
            i++;
        }

        private void saltarHasta(String cierre) throws InvalidPreferencesFormatException {
            int fin = s.indexOf(cierre, i);
            if (fin < 0) {
                error("falta `" + cierre + "`");
            }
            i = fin + cierre.length();
        }

        private void error(String m) throws InvalidPreferencesFormatException {
            throw new InvalidPreferencesFormatException(m + " (posicion " + i + ")");
        }
    }
}
