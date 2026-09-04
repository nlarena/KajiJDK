package java.net;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// Un cargador de clases que busca en una lista de URLs.
//
// ===========================================================================================
// ESTA CLASE CARGA CLASES DE VERDAD
// ===========================================================================================
//
// Es una de las pocas de `java.net` que **no** es configuracion: `findClass` lee bytes y llama a
// `defineClass`, y de ahi sale una `Class` viva. Los dos lados de eso existen en KajiJDK -- leer un
// archivo es `java.io`, y `ClassLoader.defineClass` es un nativo real de esta VM-- asi que la
// clase funciona.
//
// **El alcance, dicho derecho:** solo se buscan las URLs `file:` que apunten a un **directorio**.
// Eso cubre el uso clasico --un classpath de directorios de clases-- y deja afuera las URLs `jar:`
// y `http:`. Las de `jar:` porque leer un ZIP anidado necesita `java.util.jar`, que no esta en este
// arbol; las de `http:` porque no hay cliente HTTP.
//
// Y una URL que no se puede leer **no miente**: `findResource` devuelve null y `findClass` tira
// `ClassNotFoundException`, que son literalmente las respuestas que el contrato define para "no lo
// encontre". No es un stub disfrazado: la clase pedida efectivamente no se pudo cargar desde las
// URLs dadas, y eso es lo que se dice.
//
// (`definePackage(String, java.util.jar.Manifest, URL)` estaba aca, como lo unico que no entraba
// "porque `java.util.jar.Manifest` no existe en este arbol". Ya existe, asi que el metodo se
// declaro y esta escrito. Es lo que pasa con las notas de lo que falta cuando lo que falta deja de
// faltar.)
public class URLClassLoader extends SecureClassLoader implements Closeable {

    private final List<URL> urls = new ArrayList<URL>();
    private volatile boolean cerrado;

    /** Un cargador sobre {@code urls}, delegando en {@code parent}. */
    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        this.agregarTodas(urls);
    }

    /** Un cargador sobre {@code urls}, delegando en el cargador del sistema. */
    public URLClassLoader(URL[] urls) {
        super();
        this.agregarTodas(urls);
    }

    /**
     * Como {@link #URLClassLoader(URL[], ClassLoader)}, con una factoria de manejadores propia.
     *
     * <p>La factoria se acepta y se guarda pero **no cambia nada aca**: sirve para que un protocolo
     * ajeno resuelva sus URLs, y este cargador solo lee `file:`. Se declara porque la firma es
     * parte de la API y construir con ella tiene que compilar; ignorar una factoria que no hace
     * falta no promete nada que despues no se cumpla.
     */
    public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(parent);
        this.agregarTodas(urls);
    }

    /** Un cargador con nombre. El nombre sirve para diagnosticos y para los modulos. */
    public URLClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, parent);
        this.agregarTodas(urls);
    }

    /** Como el anterior, con factoria propia. Ver {@link #URLClassLoader(URL[], ClassLoader, URLStreamHandlerFactory)}. */
    public URLClassLoader(String name, URL[] urls, ClassLoader parent,
            URLStreamHandlerFactory factory) {
        super(name, parent);
        this.agregarTodas(urls);
    }

    private void agregarTodas(URL[] us) {
        if (us == null) {
            throw new NullPointerException("urls");
        }
        int i = 0;
        while (i < us.length) {
            this.urls.add(us[i]);
            i = i + 1;
        }
    }

    /** Una URL mas al final de la lista de busqueda. */
    protected void addURL(URL url) {
        if (this.cerrado || url == null) {
            return;
        }
        synchronized (this.urls) {
            this.urls.add(url);
        }
    }

    /** Las URLs donde busca, en orden. Es una copia: modificarla no cambia el cargador. */
    public URL[] getURLs() {
        synchronized (this.urls) {
            return this.urls.toArray(new URL[this.urls.size()]);
        }
    }

    /**
     * Busca la clase {@code name} en las URLs de este cargador.
     *
     * @throws ClassNotFoundException si no esta en ninguna, o si este cargador ya se cerro
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("null");
        }
        if (this.cerrado) {
            throw new ClassNotFoundException(name + " (cargador cerrado)");
        }
        String ruta = name.replace('.', '/') + ".class";
        URL[] us = this.getURLs();
        int i = 0;
        while (i < us.length) {
            File f = this.archivoDe(us[i], ruta);
            if (f != null && f.exists()) {
                try {
                    byte[] b = leer(f);
                    return this.defineClass(name, b, 0, b.length,
                            new CodeSource(us[i], (java.security.cert.Certificate[]) null));
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            i = i + 1;
        }
        throw new ClassNotFoundException(name);
    }

    /** La primera URL que tenga el recurso {@code name}, o null. */
    public URL findResource(String name) {
        if (name == null || this.cerrado) {
            return null;
        }
        URL[] us = this.getURLs();
        int i = 0;
        while (i < us.length) {
            File f = this.archivoDe(us[i], name);
            if (f != null && f.exists()) {
                URL u = urlDe(f);
                if (u != null) {
                    return u;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** Todas las URLs que tengan el recurso {@code name}, en el orden de busqueda. */
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> out = new ArrayList<URL>();
        if (name != null && !this.cerrado) {
            URL[] us = this.getURLs();
            int i = 0;
            while (i < us.length) {
                File f = this.archivoDe(us[i], name);
                if (f != null && f.exists()) {
                    URL u = urlDe(f);
                    if (u != null) {
                        out.add(u);
                    }
                }
                i = i + 1;
            }
        }
        return Collections.enumeration(out);
    }

    /** El recurso {@code name} abierto para leer, o null si no esta. */
    public InputStream getResourceAsStream(String name) {
        URL u = this.getResource(name);
        if (u == null) {
            return null;
        }
        try {
            return u.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Los permisos que le corresponden al codigo cargado de {@code codesource}.
     *
     * <p>Sobre los de la base agrega, para una URL con host, el permiso de conectarse a ese host:
     * es lo que hace el JDK, y la razon es que el codigo que vino de ahi va a querer volver a
     * hablar con su origen.
     *
     * <p>Para una URL {@code file:} el JDK agrega ademas un `java.io.FilePermission` de lectura
     * sobre el directorio. Esa clase no existe en este arbol, asi que ese permiso no se agrega -- lo
     * que deja la coleccion **mas chica** que la del JDK, nunca mas grande. Un permiso de menos se
     * nota; uno de mas, no.
     */
    protected PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection pc = super.getPermissions(codesource);
        URL u = codesource == null ? null : codesource.getLocation();
        if (u != null && u.getHost() != null && u.getHost().length() > 0) {
            pc.add(new SocketPermission(u.getHost(), "connect,accept"));
        }
        return pc;
    }

    /**
     * Cierra el cargador: no busca mas.
     *
     * <p>Las clases ya cargadas siguen vivas -- cerrar un cargador nunca descarga nada, ni aca ni en
     * el JDK. Lo que hace es soltar los recursos abiertos y dejar de servir pedidos nuevos.
     */
    public void close() throws java.io.IOException {
        this.cerrado = true;
    }

    /** Un cargador nuevo sobre {@code urls}, delegando en {@code parent}. */
    public static URLClassLoader newInstance(URL[] urls, ClassLoader parent) {
        return new URLClassLoader(urls, parent);
    }

    /** Un cargador nuevo sobre {@code urls}. */
    public static URLClassLoader newInstance(URL[] urls) {
        return new URLClassLoader(urls);
    }

    // El archivo que le corresponde a `ruta` bajo `base`, o null si `base` no es un directorio
    // `file:` (una URL `jar:` o `http:` cae por aca y no aporta nada; ver la cabecera).
    private File archivoDe(URL base, String ruta) {
        if (base == null || !"file".equals(base.getProtocol())) {
            return null;
        }
        String dir = base.getPath();
        if (dir == null || dir.length() == 0) {
            return null;
        }
        // Una ruta de Windows llega como `/C:/x`: la barra de mas es del formato de la URL, no del
        // sistema de archivos.
        if (dir.length() > 2 && dir.charAt(0) == '/' && dir.charAt(2) == ':') {
            dir = dir.substring(1);
        }
        File d = new File(dir);
        if (!d.isDirectory()) {
            return null;
        }
        return new File(d, ruta);
    }

    private static URL urlDe(File f) {
        try {
            String p = f.getAbsolutePath().replace('\\', '/');
            return new URL(p.startsWith("/") ? "file:" + p : "file:/" + p);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static byte[] leer(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n > 0) {
                out.write(buf, 0, n);
                n = in.read(buf);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Define un paquete leyendole los datos al manifiesto del `.jar` de donde salio.
     *
     * <p>Los seis atributos --titulo, version y proveedor, de especificacion y de implementacion--
     * se buscan **primero en la seccion del paquete y despues en la principal**, y ese orden es el
     * contrato: un `.jar` declara lo general una vez arriba y lo particular por paquete cuando hace
     * falta. Buscar al reves haria que lo general pisara a lo particular.
     *
     * <p>El nombre de la seccion es el del paquete con puntos cambiados por barras y una barra al
     * final (`com/foo/`), que es como el formato del manifiesto los escribe.
     *
     * <p>`Sealed` decide si se pasa la `URL` de sellado o `null`: un paquete sellado exige que todas
     * sus clases vengan de ese mismo origen. Tambien se busca en las dos secciones, por lo mismo.
     *
     * @param name el nombre del paquete
     * @param man el manifiesto, o `null` si el origen no tiene
     * @param url el origen, para el sellado
     * @throws IllegalArgumentException si el paquete ya estaba definido
     */
    protected Package definePackage(String name, java.util.jar.Manifest man, URL url) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (man == null) {
            return super.definePackage(name, null, null, null, null, null, null, null);
        }
        String seccion = name.replace('.', '/') + "/";
        java.util.jar.Attributes propias = man.getAttributes(seccion);
        java.util.jar.Attributes generales = man.getMainAttributes();
        String specTitle = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.SPECIFICATION_TITLE);
        String specVersion = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.SPECIFICATION_VERSION);
        String specVendor = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.SPECIFICATION_VENDOR);
        String implTitle = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE);
        String implVersion = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION);
        String implVendor = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR);
        String sellado = URLClassLoader.atributo(propias, generales,
                java.util.jar.Attributes.Name.SEALED);
        URL base = "true".equalsIgnoreCase(sellado) ? url : null;
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle,
                implVersion, implVendor, base);
    }

    // El atributo de la seccion del paquete, y si no esta, el de la principal. Ver el javadoc sobre
    // por que ese orden y no el otro.
    private static String atributo(java.util.jar.Attributes propias,
            java.util.jar.Attributes generales, java.util.jar.Attributes.Name clave) {
        if (propias != null) {
            String v = propias.getValue(clave);
            if (v != null) {
                return v;
            }
        }
        return generales == null ? null : generales.getValue(clave);
    }
}
