package java.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Un arbol de elementos XML. No es una clase del JDK ni pretende serlo: alla el decodificador de
// beans se apoya en un parser SAX del sistema, y en este arbol `org.xml.sax` son interfaces sin
// implementacion —no hay parser—. Esto es lo minimo que hace falta para leer lo que escribe
// XMLEncoder, y vive en `java.beans` porque es un detalle interno de XMLDecoder.
//
// Deliberadamente NO es un parser XML de proposito general: no hay DTD, ni espacios de nombres, ni
// entidades definidas por el documento. Lee el dialecto que produce XMLEncoder mas lo que la
// especificacion del formato de beans permite escribir a mano. Si algo no encaja, se queja en vez
// de adivinar.
final class NodoXml {

    final String nombre;
    final Map<String, String> atributos = new HashMap<String, String>();
    final List<NodoXml> hijos = new ArrayList<NodoXml>();
    final StringBuilder texto = new StringBuilder();

    // Texto e hijos otra vez, pero EN ORDEN y mezclados: cada entrada es una String o un NodoXml.
    // `texto` junta todo el texto y `hijos` todos los elementos, y esas dos vistas pierden donde
    // estaba cada cosa. Para `<string>a<int>9</int>b</string>` la diferencia es entre "a9b" y
    // "ab9", asi que el decodificador lee por aca.
    final List<Object> content = new ArrayList<Object>();

    NodoXml(String nombre) {
        this.nombre = nombre;
    }

    String atributo(String clave) {
        return this.atributos.get(clave);
    }
}

// Analizador descendente sobre el texto completo del documento.
//
// Trabaja sobre una String y no sobre un Reader a proposito: el documento de un XMLDecoder es
// chico —es la descripcion de un grafo, no un flujo— y tenerlo entero permite que los errores digan
// la posicion exacta. La decodificacion de bytes a caracteres se hace aca mismo, en UTF-8, porque
// `java.io.InputStreamReader` ignora el charset en este arbol (ver el encabezado de XMLEncoder).
final class AnalizadorXml {

    private final String s;
    private int i;

    private AnalizadorXml(String s) {
        this.s = s;
        this.i = 0;
    }

    // Lee el flujo entero y devuelve el elemento raiz.
    static NodoXml analizar(InputStream entrada) throws IOException {
        return new AnalizadorXml(leerUtf8(entrada)).raiz();
    }

    // La misma lectura sobre un documento que ya esta en memoria. Es por donde entra un
    // `org.xml.sax.InputSource` que trae un Reader: ahi la decodificacion ya la hizo otro.
    static NodoXml parseText(String document) {
        return new AnalizadorXml(document).raiz();
    }

    static String readAll(java.io.Reader source) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int read = source.read(buf);
        while (read > 0) {
            sb.append(buf, 0, read);
            read = source.read(buf);
        }
        if (sb.length() > 0 && sb.charAt(0) == '﻿') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    static String leerUtf8(InputStream entrada) throws IOException {
        byte[] datos = leerTodo(entrada);
        StringBuilder sb = new StringBuilder(datos.length);
        int n = datos.length;
        int p = 0;
        while (p < n) {
            int b0 = datos[p] & 0xFF;
            int cp;
            int largo;
            if (b0 < 0x80) { cp = b0; largo = 1; }
            else if ((b0 & 0xE0) == 0xC0) { cp = b0 & 0x1F; largo = 2; }
            else if ((b0 & 0xF0) == 0xE0) { cp = b0 & 0x0F; largo = 3; }
            else if ((b0 & 0xF8) == 0xF0) { cp = b0 & 0x07; largo = 4; }
            else { cp = 0xFFFD; largo = 1; }
            if (p + largo > n) {
                cp = 0xFFFD;
                largo = n - p;
            } else {
                for (int k = 1; k < largo; k++) {
                    cp = (cp << 6) | (datos[p + k] & 0x3F);
                }
            }
            p += largo;
            if (cp > 0xFFFF) {
                cp -= 0x10000;
                sb.append((char) (0xD800 + (cp >> 10)));
                sb.append((char) (0xDC00 + (cp & 0x3FF)));
            } else {
                sb.append((char) cp);
            }
        }
        // La marca de orden de bytes, si vino, no es parte del documento.
        if (sb.length() > 0 && sb.charAt(0) == '﻿') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    private static byte[] leerTodo(InputStream entrada) throws IOException {
        java.io.ByteArrayOutputStream acum = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int leidos = entrada.read(buf);
        while (leidos > 0) {
            acum.write(buf, 0, leidos);
            leidos = entrada.read(buf);
        }
        return acum.toByteArray();
    }

    private NodoXml raiz() {
        this.saltarPreambulo();
        NodoXml r = this.elemento();
        return r;
    }

    // Declaracion XML, comentarios, instrucciones de proceso y DOCTYPE antes de la raiz.
    private void saltarPreambulo() {
        boolean sigo = true;
        while (sigo) {
            this.saltarBlancos();
            if (this.miraA("<?")) {
                this.hasta("?>");
            } else if (this.miraA("<!--")) {
                this.hasta("-->");
            } else if (this.miraA("<!")) {
                this.hasta(">");
            } else {
                sigo = false;
            }
        }
    }

    private NodoXml elemento() {
        this.exigir("<");
        String nombre = this.nombreXml();
        NodoXml nodo = new NodoXml(nombre);
        boolean vacio = false;
        boolean cerrado = false;
        while (!cerrado) {
            this.saltarBlancos();
            if (this.miraA("/>")) {
                this.i += 2;
                vacio = true;
                cerrado = true;
            } else if (this.miraA(">")) {
                this.i += 1;
                cerrado = true;
            } else {
                String clave = this.nombreXml();
                this.saltarBlancos();
                this.exigir("=");
                this.saltarBlancos();
                nodo.atributos.put(clave, this.valorEntreComillas());
            }
        }
        if (!vacio) {
            this.contenido(nodo);
            this.exigir("</");
            String cierre = this.nombreXml();
            if (!cierre.equals(nombre)) {
                throw this.error("cierra </" + cierre + "> lo que abrio <" + nombre + ">");
            }
            this.saltarBlancos();
            this.exigir(">");
        }
        return nodo;
    }

    private void contenido(NodoXml nodo) {
        boolean sigo = true;
        while (sigo) {
            if (this.i >= this.s.length()) {
                throw this.error("el documento termina dentro de <" + nodo.nombre + ">");
            }
            if (this.miraA("</")) {
                sigo = false;
            } else if (this.miraA("<!--")) {
                this.hasta("-->");
            } else if (this.miraA("<![CDATA[")) {
                int fin = this.s.indexOf("]]>", this.i);
                if (fin < 0) {
                    throw this.error("CDATA sin cerrar");
                }
                String cdata = this.s.substring(this.i + 9, fin);
                nodo.texto.append(cdata);
                nodo.content.add(cdata);
                this.i = fin + 3;
            } else if (this.miraA("<?")) {
                this.hasta("?>");
            } else if (this.miraA("<")) {
                NodoXml child = this.elemento();
                nodo.hijos.add(child);
                nodo.content.add(child);
            } else {
                String chunk = this.textoHastaMarca();
                nodo.texto.append(chunk);
                nodo.content.add(chunk);
            }
        }
    }

    private String textoHastaMarca() {
        StringBuilder sb = new StringBuilder();
        while (this.i < this.s.length() && this.s.charAt(this.i) != '<') {
            char c = this.s.charAt(this.i);
            if (c == '&') {
                sb.append(this.entidad());
            } else {
                sb.append(c);
                this.i++;
            }
        }
        return sb.toString();
    }

    // Las cinco entidades que XML define de fabrica, mas las referencias numericas. No hay
    // entidades del documento: sin DTD no hay donde declararlas.
    private String entidad() {
        int fin = this.s.indexOf(';', this.i);
        if (fin < 0) {
            throw this.error("entidad sin `;`");
        }
        String cuerpo = this.s.substring(this.i + 1, fin);
        this.i = fin + 1;
        String r;
        if (cuerpo.equals("amp")) { r = "&"; }
        else if (cuerpo.equals("lt")) { r = "<"; }
        else if (cuerpo.equals("gt")) { r = ">"; }
        else if (cuerpo.equals("quot")) { r = "\""; }
        else if (cuerpo.equals("apos")) { r = "'"; }
        else if (cuerpo.length() > 1 && cuerpo.charAt(0) == '#') {
            int cp;
            if (cuerpo.charAt(1) == 'x' || cuerpo.charAt(1) == 'X') {
                cp = Integer.parseInt(cuerpo.substring(2), 16);
            } else {
                cp = Integer.parseInt(cuerpo.substring(1));
            }
            if (cp > 0xFFFF) {
                cp -= 0x10000;
                r = new String(new char[] { (char) (0xD800 + (cp >> 10)), (char) (0xDC00 + (cp & 0x3FF)) });
            } else {
                r = String.valueOf((char) cp);
            }
        } else {
            throw this.error("entidad desconocida &" + cuerpo + ";");
        }
        return r;
    }

    private String valorEntreComillas() {
        char comilla = this.actual();
        if (comilla != '"' && comilla != '\'') {
            throw this.error("el valor de un atributo va entre comillas");
        }
        this.i++;
        StringBuilder sb = new StringBuilder();
        while (this.i < this.s.length() && this.s.charAt(this.i) != comilla) {
            char c = this.s.charAt(this.i);
            if (c == '&') {
                sb.append(this.entidad());
            } else {
                sb.append(c);
                this.i++;
            }
        }
        if (this.i >= this.s.length()) {
            throw this.error("atributo sin cerrar");
        }
        this.i++;
        return sb.toString();
    }

    private String nombreXml() {
        int inicio = this.i;
        while (this.i < this.s.length() && esNombre(this.s.charAt(this.i))) {
            this.i++;
        }
        if (inicio == this.i) {
            throw this.error("se esperaba un nombre");
        }
        return this.s.substring(inicio, this.i);
    }

    private static boolean esNombre(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
            || c == '_' || c == '-' || c == '.' || c == ':' || c > 127;
    }

    private void saltarBlancos() {
        while (this.i < this.s.length() && esBlanco(this.s.charAt(this.i))) {
            this.i++;
        }
    }

    private static boolean esBlanco(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private char actual() {
        if (this.i >= this.s.length()) {
            throw this.error("el documento termina antes de tiempo");
        }
        return this.s.charAt(this.i);
    }

    private boolean miraA(String t) {
        return this.s.startsWith(t, this.i);
    }

    private void exigir(String t) {
        if (!this.miraA(t)) {
            throw this.error("se esperaba `" + t + "`");
        }
        this.i += t.length();
    }

    private void hasta(String t) {
        int fin = this.s.indexOf(t, this.i);
        if (fin < 0) {
            throw this.error("falta `" + t + "`");
        }
        this.i = fin + t.length();
    }

    private RuntimeException error(String queja) {
        int linea = 1;
        for (int k = 0; k < this.i && k < this.s.length(); k++) {
            if (this.s.charAt(k) == '\n') {
                linea++;
            }
        }
        return new IllegalArgumentException("XML linea " + linea + ": " + queja);
    }
}
