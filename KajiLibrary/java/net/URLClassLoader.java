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

// A class loader that searches a list of URLs.
//
// ===========================================================================================
// THIS CLASS REALLY LOADS CLASSES
// ===========================================================================================
//
// It is one of the few in `java.net` that is **not** configuration: `findClass` reads bytes and calls
// `defineClass`, and out of that comes a live `Class`. Both sides of that exist in KajiJDK -- reading
// a file is `java.io`, and `ClassLoader.defineClass` is a real native of this VM -- so the class
// works.
//
// **The scope, said straight:** only the `file:` URLs pointing at a **directory** are searched. That
// covers the classic use --a classpath of class directories-- and leaves out `jar:` and `http:` URLs.
// The `jar:` ones because reading a nested ZIP needs `java.util.jar`, which was not in this tree when
// this was written; the `http:` ones because there is no HTTP client.
//
// And a URL that cannot be read **does not lie**: `findResource` returns null and `findClass` throws
// `ClassNotFoundException`, which are literally the answers the contract defines for "I did not find
// it". It is not a disguised stub: the requested class really could not be loaded from the given
// URLs, and that is what is said.
//
// (`definePackage(String, java.util.jar.Manifest, URL)` used to be listed here as the one thing that
// did not go in "because `java.util.jar.Manifest` is not in this tree". It exists now, so the method
// is declared and written. That is what happens to the notes about what is missing when what was
// missing stops being missing.)
public class URLClassLoader extends SecureClassLoader implements Closeable {

    private final List<URL> urls = new ArrayList<URL>();
    private volatile boolean closed;

    /** A loader over {@code urls}, delegating to {@code parent}. */
    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        this.addAll(urls);
    }

    /** A loader over {@code urls}, delegating to the system loader. */
    public URLClassLoader(URL[] urls) {
        super();
        this.addAll(urls);
    }

    /**
     * Like {@link #URLClassLoader(URL[], ClassLoader)}, with a handler factory of its own.
     *
     * <p>The factory is accepted and kept but it **changes nothing here**: it serves to let a foreign
     * protocol resolve its URLs, and this loader only reads `file:`. It is declared because the
     * signature is part of the API and constructing with it has to compile; ignoring a factory that
     * is not needed promises nothing that later goes unfulfilled.
     */
    public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(parent);
        this.addAll(urls);
    }

    /** A named loader. The name serves for diagnostics and for the modules. */
    public URLClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, parent);
        this.addAll(urls);
    }

    /** Like the previous one, with its own factory. See {@link #URLClassLoader(URL[], ClassLoader, URLStreamHandlerFactory)}. */
    public URLClassLoader(String name, URL[] urls, ClassLoader parent,
            URLStreamHandlerFactory factory) {
        super(name, parent);
        this.addAll(urls);
    }

    private void addAll(URL[] us) {
        if (us == null) {
            throw new NullPointerException("urls");
        }
        int i = 0;
        while (i < us.length) {
            this.urls.add(us[i]);
            i = i + 1;
        }
    }

    /** One more URL at the end of the search list. */
    protected void addURL(URL url) {
        if (this.closed || url == null) {
            return;
        }
        synchronized (this.urls) {
            this.urls.add(url);
        }
    }

    /** The URLs it searches, in order. It is a copy: modifying it does not change the loader. */
    public URL[] getURLs() {
        synchronized (this.urls) {
            return this.urls.toArray(new URL[this.urls.size()]);
        }
    }

    /**
     * Looks for the class {@code name} in this loader's URLs.
     *
     * @throws ClassNotFoundException if it is in none of them, or if this loader is already closed
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("null");
        }
        if (this.closed) {
            throw new ClassNotFoundException(name + " (cargador cerrado)");
        }
        String path = name.replace('.', '/') + ".class";
        URL[] us = this.getURLs();
        int i = 0;
        while (i < us.length) {
            File f = this.fileFor(us[i], path);
            if (f != null && f.exists()) {
                try {
                    byte[] b = readAll(f);
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

    /** The first URL that has the resource {@code name}, or null. */
    public URL findResource(String name) {
        if (name == null || this.closed) {
            return null;
        }
        URL[] us = this.getURLs();
        int i = 0;
        while (i < us.length) {
            File f = this.fileFor(us[i], name);
            if (f != null && f.exists()) {
                URL u = urlFor(f);
                if (u != null) {
                    return u;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** Every URL that has the resource {@code name}, in search order. */
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> out = new ArrayList<URL>();
        if (name != null && !this.closed) {
            URL[] us = this.getURLs();
            int i = 0;
            while (i < us.length) {
                File f = this.fileFor(us[i], name);
                if (f != null && f.exists()) {
                    URL u = urlFor(f);
                    if (u != null) {
                        out.add(u);
                    }
                }
                i = i + 1;
            }
        }
        return Collections.enumeration(out);
    }

    /** The resource {@code name} opened for reading, or null if it is not there. */
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
     * The permissions that go with the code loaded from {@code codesource}.
     *
     * <p>On top of the base's it adds, for a URL with a host, permission to connect to that host: it
     * is what the JDK does, and the reason is that code which came from there is going to want to
     * talk to its origin again.
     *
     * <p>For a {@code file:} URL the JDK also adds a `java.io.FilePermission` to read the directory.
     * That class is not in this tree, so that permission is not added -- which leaves the collection
     * **smaller** than the JDK's, never larger. A missing permission gets noticed; an extra one does
     * not.
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
     * Closes the loader: it searches no more.
     *
     * <p>The classes already loaded stay alive -- closing a loader never unloads anything, here or in
     * the JDK. What it does is release the open resources and stop serving new requests.
     */
    public void close() throws java.io.IOException {
        this.closed = true;
    }

    /** A new loader over {@code urls}, delegating to {@code parent}. */
    public static URLClassLoader newInstance(URL[] urls, ClassLoader parent) {
        return new URLClassLoader(urls, parent);
    }

    /** A new loader over {@code urls}. */
    public static URLClassLoader newInstance(URL[] urls) {
        return new URLClassLoader(urls);
    }

    // The file corresponding to `path` under `base`, or null if `base` is not a `file:` directory (a
    // `jar:` or `http:` URL lands here and contributes nothing; see the header).
    private File fileFor(URL base, String path) {
        if (base == null || !"file".equals(base.getProtocol())) {
            return null;
        }
        String dir = base.getPath();
        if (dir == null || dir.length() == 0) {
            return null;
        }
        // A Windows path arrives as `/C:/x`: the extra slash belongs to the URL's format, not to the
        // file system.
        if (dir.length() > 2 && dir.charAt(0) == '/' && dir.charAt(2) == ':') {
            dir = dir.substring(1);
        }
        File d = new File(dir);
        if (!d.isDirectory()) {
            return null;
        }
        return new File(d, path);
    }

    private static URL urlFor(File f) {
        try {
            String p = f.getAbsolutePath().replace('\\', '/');
            return new URL(p.startsWith("/") ? "file:" + p : "file:/" + p);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static byte[] readAll(File f) throws IOException {
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
     * Defines a package by reading the data off the manifest of the `.jar` it came from.
     *
     * <p>The six attributes --title, version and vendor, of specification and of implementation-- are
     * looked up **first in the package's section and then in the main one**, and that order is the
     * contract: a `.jar` declares the general once at the top and the particular per package when it
     * needs to. Looking the other way round would let the general override the particular.
     *
     * <p>The section's name is the package's with dots changed to slashes and a trailing slash
     * (`com/foo/`), which is how the manifest format writes them.
     *
     * <p>`Sealed` decides whether the sealing `URL` or `null` is passed: a sealed package requires all
     * its classes to come from that same origin. It is looked up in both sections too, for the same
     * reason.
     *
     * @param name the package's name
     * @param man the manifest, or `null` if the origin has none
     * @param url the origin, for the sealing
     * @throws IllegalArgumentException if the package was already defined
     */
    protected Package definePackage(String name, java.util.jar.Manifest man, URL url) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (man == null) {
            return super.definePackage(name, null, null, null, null, null, null, null);
        }
        String section = name.replace('.', '/') + "/";
        java.util.jar.Attributes own = man.getAttributes(section);
        java.util.jar.Attributes main = man.getMainAttributes();
        String specTitle = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.SPECIFICATION_TITLE);
        String specVersion = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.SPECIFICATION_VERSION);
        String specVendor = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.SPECIFICATION_VENDOR);
        String implTitle = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE);
        String implVersion = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION);
        String implVendor = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR);
        String sealed = URLClassLoader.attribute(own, main,
                java.util.jar.Attributes.Name.SEALED);
        URL base = "true".equalsIgnoreCase(sealed) ? url : null;
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle,
                implVersion, implVendor, base);
    }

    // The attribute from the package's section, and failing that, the main one's. See the javadoc on
    // why that order and not the other.
    private static String attribute(java.util.jar.Attributes own,
            java.util.jar.Attributes main, java.util.jar.Attributes.Name key) {
        if (own != null) {
            String v = own.getValue(key);
            if (v != null) {
                return v;
            }
        }
        return main == null ? null : main.getValue(key);
    }
}
