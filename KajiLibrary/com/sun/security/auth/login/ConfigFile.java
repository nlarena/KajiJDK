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
 * La configuracion de JAAS leida de un archivo de texto.
 *
 * <h2>Que dice un archivo de estos</h2>
 *
 * <p>Que modulos de login usa cada aplicacion y como se combinan:
 *
 * <pre>{@code
 * MiApp {
 *     com.ejemplo.ModuloLDAP  REQUIRED   servidor="ldap://x" puerto=389;
 *     com.ejemplo.ModuloLocal SUFFICIENT debug=true;
 * };
 * }</pre>
 *
 * <p>El punto de todo el mecanismo esta en la palabra del medio. {@code REQUIRED} tiene que dar bien
 * y si falla se sigue preguntando igual —para no revelar <em>cual</em> de los modulos rechazo—;
 * {@code REQUISITE} corta en el acto; {@code SUFFICIENT} alcanza por si solo y termina;
 * {@code OPTIONAL} no decide nada. Encadenarlos es lo que permite armar una politica sin escribir
 * codigo, que es la razon de que esto sea un archivo y no una clase.
 *
 * <h2>De donde sale el archivo</h2>
 *
 * <p>Del {@link URI} que se le pase, y si no se pasa ninguno, de la propiedad
 * {@code java.security.auth.login.config}. No encontrar nada <strong>no es un error</strong>:
 * {@link #getAppConfigurationEntry} devuelve {@code null} para toda aplicacion, que es lo que
 * corresponde cuando no hay politica configurada.
 */
public class ConfigFile extends Configuration {

    private final URI url;
    private Map<String, List<AppConfigurationEntry>> configuracion;

    /**
     * Del lugar por omision.
     *
     * @throws SecurityException si el archivo existe y no se puede leer o no se entiende
     */
    public ConfigFile() {
        this.url = null;
        refresh();
    }

    /**
     * De {@code uri}.
     *
     * @throws NullPointerException si {@code uri} es {@code null}
     * @throws SecurityException si no se puede leer o no se entiende
     */
    public ConfigFile(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.url = uri;
        refresh();
    }

    /**
     * Los modulos configurados para {@code applicationName}, o {@code null} si no hay ninguno.
     *
     * <p>{@code null} y no un arreglo vacio, y la diferencia importa: vacio seria "esta configurada
     * y no usa modulos", que dejaria entrar a cualquiera. {@code null} es "no esta configurada", y
     * quien llama tiene que decidir que hacer con eso — normalmente, negar.
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
        if (applicationName == null) {
            return null;
        }
        List<AppConfigurationEntry> entradas;
        synchronized (this) {
            entradas = this.configuracion.get(applicationName);
        }
        if (entradas == null || entradas.isEmpty()) {
            return null;
        }
        return entradas.toArray(new AppConfigurationEntry[entradas.size()]);
    }

    /**
     * Relee el archivo.
     *
     * <p>Se llama desde los dos constructores: el estado de este objeto es el archivo, asi que
     * construirlo y releerlo son la misma operacion.
     */
    public void refresh() {
        Map<String, List<AppConfigurationEntry>> nueva =
                new HashMap<String, List<AppConfigurationEntry>>();
        String ubicacion = ubicacion();
        if (ubicacion != null) {
            try {
                cargar(ubicacion, nueva);
            } catch (IOException e) {
                throw new SecurityException("no se pudo leer la configuracion de login: "
                        + ubicacion, e);
            }
        }
        synchronized (this) {
            this.configuracion = nueva;
        }
    }

    private String ubicacion() {
        if (this.url != null) {
            return this.url.toString();
        }
        return System.getProperty("java.security.auth.login.config");
    }

    private void cargar(String ubicacion, Map<String, List<AppConfigurationEntry>> destino)
            throws IOException {
        InputStream in;
        try {
            in = new URL(ubicacion).openStream();
        } catch (IOException e) {
            // No era una URL: se prueba como ruta de archivo. El JDK acepta las dos formas y
            // distinguirlas de antemano es adivinar.
            in = new java.io.FileInputStream(ubicacion);
        }
        BufferedReader lector = new BufferedReader(new InputStreamReader(in));
        try {
            parsear(texto(lector), destino);
        } finally {
            lector.close();
        }
    }

    private String texto(BufferedReader lector) throws IOException {
        StringBuilder sb = new StringBuilder();
        String linea = lector.readLine();
        while (linea != null) {
            // Los comentarios se sacan aca y no en el parser: adentro de un valor entre comillas un
            // `//` es texto, pero este formato no admite `//` dentro de comillas, asi que cortar por
            // linea es correcto y mucho mas simple.
            int corte = linea.indexOf("//");
            if (corte >= 0) {
                linea = linea.substring(0, corte);
            }
            sb.append(linea).append('\n');
            linea = lector.readLine();
        }
        return sb.toString();
    }

    private void parsear(String texto, Map<String, List<AppConfigurationEntry>> destino) {
        Lexico lex = new Lexico(texto);
        String nombre = lex.palabra();
        while (nombre != null) {
            lex.esperar("{");
            List<AppConfigurationEntry> entradas = new ArrayList<AppConfigurationEntry>();
            String modulo = lex.palabra();
            while (modulo != null && !modulo.equals("}")) {
                String bandera = lex.palabra();
                Map<String, Object> opciones = new HashMap<String, Object>();
                String t = lex.palabra();
                while (t != null && !t.equals(";")) {
                    int igual = t.indexOf('=');
                    if (igual > 0) {
                        opciones.put(t.substring(0, igual), sinComillas(t.substring(igual + 1)));
                    }
                    t = lex.palabra();
                }
                entradas.add(new AppConfigurationEntry(modulo, banderaDe(bandera), opciones));
                modulo = lex.palabra();
            }
            lex.esperar(";");
            destino.put(nombre, entradas);
            nombre = lex.palabra();
        }
    }

    private static String sinComillas(String v) {
        if (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static AppConfigurationEntry.LoginModuleControlFlag banderaDe(String s) {
        if (s == null) {
            throw new SecurityException("falta la bandera de control");
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
        throw new SecurityException("bandera de control desconocida: " + s);
    }

    /**
     * El lexico del formato: palabras separadas por espacios, con {@code {}} y {@code ;} como
     * simbolos propios y comillas que agrupan.
     */
    private static final class Lexico {

        private final String texto;
        private int i;

        Lexico(String texto) {
            this.texto = texto;
        }

        void esperar(String simbolo) {
            String t = palabra();
            if (t == null || !t.equals(simbolo)) {
                throw new SecurityException("se esperaba " + simbolo + " y vino "
                        + String.valueOf(t));
            }
        }

        String palabra() {
            while (this.i < this.texto.length()
                    && Character.isWhitespace(this.texto.charAt(this.i))) {
                this.i++;
            }
            if (this.i >= this.texto.length()) {
                return null;
            }
            char c = this.texto.charAt(this.i);
            if (c == '{' || c == '}' || c == ';') {
                this.i++;
                return String.valueOf(c);
            }
            int desde = this.i;
            boolean enComillas = false;
            while (this.i < this.texto.length()) {
                char d = this.texto.charAt(this.i);
                if (d == '"') {
                    enComillas = !enComillas;
                } else if (!enComillas
                        && (Character.isWhitespace(d) || d == '{' || d == '}' || d == ';')) {
                    break;
                }
                this.i++;
            }
            return this.texto.substring(desde, this.i);
        }
    }
}
