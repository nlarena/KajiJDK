package java.util.logging;

/**
 * KajiLibrary's java.util.logging.FileHandler -- escribe la traza a uno o varios archivos.
 *
 * <p>Es un {@link StreamHandler} sobre un archivo mas dos cosas que un archivo necesita y un flujo
 * no: **un nombre que se calcula** y **un limite de tamano**.
 *
 * <p><strong>El patron.</strong> El nombre no se da hecho sino como una plantilla, porque el nombre
 * bueno depende de cosas que solo se saben al arrancar:
 *
 * <ul>
 * <li>`/` es el separador de directorios, siempre, se escriba en el sistema que se escriba.
 * <li>`%t` es el directorio temporal y `%h` el del usuario.
 * <li>`%g` es el numero de generacion: distingue el archivo actual de los que ya rotaron.
 * <li>`%u` es un numero unico: distingue a dos manejadores que pidieron el mismo nombre.
 * <li>`%%` es un `%`.
 * </ul>
 *
 * <p>Si `count` es mayor que uno y el patron no tiene `%g`, se le agrega `.%g` al final. Tiene que
 * ser asi: sin generacion en el nombre, rotar seria escribir siempre encima del mismo archivo.
 *
 * <p><strong>La rotacion.</strong> Cuando el archivo pasa de `limit` bytes se cierra, los que ya
 * estaban se corren un lugar --el 0 pasa a ser el 1, el 1 el 2-- y se abre uno nuevo en el 0. El mas
 * viejo se pierde. `limit` en cero significa sin limite, que es lo unico razonable para "no rotes".
 * Notar que el corte se mira **despues** de escribir cada registro: un registro nunca queda partido
 * entre dos archivos, y por eso el archivo puede pasarse un poco del limite.
 *
 * <p>Y cada archivo rotado es un documento **completo**: al cerrarlo se le escribe la cola del
 * formateador y al abrir el siguiente su cabecera. Con {@link XMLFormatter} eso es la diferencia
 * entre tres archivos que se pueden parsear y tres pedazos que no.
 *
 * <p><strong>Dos cosas que este arbol no puede dar y hay que decir.</strong>
 *
 * <ol>
 * <li>`%t` y `%h` salen de `java.io.tmpdir` y `user.home`, y esta VM **no define ninguna de las
 *     dos**. Un patron que las use falla al construir con un {@link java.io.IOException} que lo dice
 *     -- que es mejor que inventar una ruta y escribir la traza en un lugar que nadie pidio. El
 *     programa que fije esas propiedades con `System.setProperty` las hace funcionar.
 * <li>`%u` distingue manejadores **de esta VM**. El JDK lo hace con un archivo `.lck` y un cerrojo
 *     del sistema de archivos, asi que ahi tambien distingue procesos distintos; aca no hay cerrojos
 *     de archivo, asi que la garantia es mas chica y es esa. Dos procesos que pidan el mismo patron
 *     se pisan.
 * </ol>
 */
public class FileHandler extends StreamHandler {

    // Los archivos que algun `FileHandler` de esta VM tiene abiertos, por su ruta. Es lo que hace
    // que `%u` distinga: ver la nota (2) de arriba.
    private static final java.util.HashSet<String> ABIERTOS = new java.util.HashSet<String>();

    private String pattern;
    private long limit;
    private int count;
    private boolean append;
    private java.io.File[] files;
    private Medidor medidor;
    private String reservado;

    /** El que sale de la configuracion. */
    public FileHandler() throws java.io.IOException, SecurityException {
        this.configurar();
        this.abrirArchivos();
    }

    /**
     * @throws IllegalArgumentException si el patron es vacio
     * @throws NullPointerException si el patron es `null`
     */
    public FileHandler(String pattern) throws java.io.IOException, SecurityException {
        this(pattern, 0L, 1, false);
    }

    public FileHandler(String pattern, boolean append)
            throws java.io.IOException, SecurityException {
        this(pattern, 0L, 1, append);
    }

    public FileHandler(String pattern, int limit, int count)
            throws java.io.IOException, SecurityException {
        this(pattern, (long) limit, count, false);
    }

    public FileHandler(String pattern, int limit, int count, boolean append)
            throws java.io.IOException, SecurityException {
        this(pattern, (long) limit, count, append);
    }

    /**
     * @throws IllegalArgumentException si `limit` es negativo, si `count` es menor que uno, o si el
     *         patron es vacio
     */
    public FileHandler(String pattern, long limit, int count, boolean append)
            throws java.io.IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        this.configurar();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
        this.abrirArchivos();
    }

    // Lo que este manejador lee de la configuracion. Se llama SIEMPRE, tambien desde los
    // constructores que reciben el patron: el nivel, el filtro, el formateador y la codificacion
    // salen de la configuracion aunque el patron venga por codigo.
    private void configurar() {
        LogManager m = LogManager.getLogManager();
        String cname = this.getClass().getName();
        this.pattern = m.getStringProperty(cname + ".pattern", "%h/java%u.log");
        this.limit = m.getLongProperty(cname + ".limit", 0);
        if (this.limit < 0) {
            this.limit = 0;
        }
        this.count = m.getIntProperty(cname + ".count", 1);
        if (this.count <= 0) {
            this.count = 1;
        }
        this.append = m.getBooleanProperty(cname + ".append", false);
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new XMLFormatter()));
        try {
            this.setEncoding(m.getStringProperty(cname + ".encoding", null));
        } catch (Exception e) {
            try {
                this.setEncoding(null);
            } catch (Exception e2) {
                // No puede pasar: `null` siempre se acepta.
            }
        }
    }

    private void abrirArchivos() throws java.io.IOException {
        int unico = 0;
        while (true) {
            java.io.File[] fs = new java.io.File[this.count];
            int i = 0;
            while (i < this.count) {
                fs[i] = this.generar(this.pattern, i, unico);
                i = i + 1;
            }
            String clave = fs[0].getPath();
            synchronized (ABIERTOS) {
                if (!ABIERTOS.contains(clave)) {
                    ABIERTOS.add(clave);
                    this.files = fs;
                    this.reservado = clave;
                    break;
                }
            }
            unico = unico + 1;
        }
        try {
            if (!this.append) {
                // Sin `append`, lo que ya estaba tambien corre un lugar: el archivo 0 es siempre el de
                // esta ejecucion, y la anterior queda en el 1 en vez de perderse.
                this.correr();
            }
            this.abrir(this.files[0], this.append);
        } catch (java.io.IOException e) {
            this.liberar();
            throw e;
        } catch (RuntimeException e) {
            this.liberar();
            throw e;
        }
    }

    private void liberar() {
        if (this.reservado == null) {
            return;
        }
        synchronized (ABIERTOS) {
            ABIERTOS.remove(this.reservado);
        }
        this.reservado = null;
    }

    // El nombre para esa generacion y ese numero unico. Ver la lista de sustituciones en la cabecera.
    private java.io.File generar(String patron, int generacion, int unico)
            throws java.io.IOException {
        java.io.File dir = null;
        StringBuilder palabra = new StringBuilder();
        boolean vioG = false;
        boolean vioU = false;
        int i = 0;
        while (i < patron.length()) {
            char c = patron.charAt(i);
            i = i + 1;
            char c2 = 0;
            if (i < patron.length()) {
                c2 = Character.toLowerCase(patron.charAt(i));
            }
            if (c == '/') {
                dir = dir == null ? new java.io.File(palabra.toString())
                        : new java.io.File(dir, palabra.toString());
                palabra.setLength(0);
                continue;
            }
            if (c == '%') {
                if (c2 == 't') {
                    dir = new java.io.File(propiedadDeRuta("java.io.tmpdir", "user.home", "%t"));
                    i = i + 1;
                    palabra.setLength(0);
                    continue;
                }
                if (c2 == 'h') {
                    dir = new java.io.File(propiedadDeRuta("user.home", null, "%h"));
                    i = i + 1;
                    palabra.setLength(0);
                    continue;
                }
                if (c2 == 'g') {
                    palabra.append(generacion);
                    vioG = true;
                    i = i + 1;
                    continue;
                }
                if (c2 == 'u') {
                    palabra.append(unico);
                    vioU = true;
                    i = i + 1;
                    continue;
                }
                if (c2 == '%') {
                    palabra.append('%');
                    i = i + 1;
                    continue;
                }
            }
            palabra.append(c);
        }
        if (this.count > 1 && !vioG) {
            palabra.append('.').append(generacion);
        }
        if (unico > 0 && !vioU) {
            palabra.append('.').append(unico);
        }
        if (palabra.length() > 0) {
            dir = dir == null ? new java.io.File(palabra.toString())
                    : new java.io.File(dir, palabra.toString());
        }
        return dir;
    }

    // La propiedad que nombra un directorio, o un fallo que dice cual falta. Ver la nota (1) de la
    // cabecera: inventar una ruta pondria la traza en un lugar que nadie pidio.
    private static String propiedadDeRuta(String clave, String alternativa, String marca)
            throws java.io.IOException {
        String v = System.getProperty(clave);
        if (v == null && alternativa != null) {
            v = System.getProperty(alternativa);
        }
        if (v == null) {
            throw new java.io.IOException("can't use " + marca + ": the system property \"" + clave
                    + "\" is not defined");
        }
        return v;
    }

    private void abrir(java.io.File f, boolean append) throws java.io.IOException {
        long yaEscrito = append ? f.length() : 0;
        java.io.OutputStream salida = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(f.getPath(), append));
        this.medidor = new Medidor(salida, yaEscrito);
        this.setOutputStream(this.medidor);
    }

    /**
     * Escribe el registro, y rota si con el se paso del limite.
     *
     * <p>Vacia despues de cada uno, como {@link ConsoleHandler} y por la misma razon: el ultimo
     * mensaje antes de una caida es el que importa, y es justo el que se quedaria en el buffer.
     */
    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record)) {
            return;
        }
        super.publish(record);
        this.flush();
        if (this.limit > 0 && this.medidor != null && this.medidor.escritos() >= this.limit) {
            this.rotar();
        }
    }

    /**
     * Cierra el archivo actual y suelta el nombre.
     *
     * <p>Soltar el nombre es lo que permite que otro manejador lo vuelva a tomar sin correr el `%u`.
     */
    public synchronized void close() throws SecurityException {
        super.close();
        this.liberar();
    }

    // Cierra el actual, corre los que estaban y abre uno nuevo en la generacion cero.
    //
    // El nivel se pone en OFF mientras dura: la apertura puede fallar y reportar el fallo emite un
    // mensaje, y un mensaje que entrara a este mismo manejador a mitad de la rotacion volveria a
    // encontrarlo sin archivo.
    private void rotar() {
        Level viejo = this.getLevel();
        this.setLevel(Level.OFF);
        super.close();
        this.correr();
        try {
            this.abrir(this.files[0], false);
        } catch (java.io.IOException e) {
            this.reportError(null, e, ErrorManager.OPEN_FAILURE);
        }
        this.setLevel(viejo);
    }

    // Corre cada archivo un lugar hacia arriba, del anteultimo al primero. De atras para adelante a
    // proposito: al reves cada paso pisaria al que todavia falta mover.
    private void correr() {
        int i = this.count - 2;
        while (i >= 0) {
            java.io.File f1 = this.files[i];
            java.io.File f2 = this.files[i + 1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                mover(f1, f2);
            }
            i = i - 1;
        }
    }

    /**
     * Renombra, y si la plataforma no sabe renombrar, copia y borra.
     *
     * <p>La copia no es un capricho: en este arbol `File.renameTo` devuelve `false` siempre porque no
     * hay nativo de renombrado. El resultado observable es el mismo --el contenido termina en el
     * archivo de destino y el de origen deja de existir--, cuesta el tamano del archivo, y es lo que
     * permite que la rotacion funcione de verdad en vez de no funcionar.
     */
    private static void mover(java.io.File origen, java.io.File destino) {
        if (origen.renameTo(destino)) {
            return;
        }
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = new java.io.FileInputStream(origen.getPath());
            out = new java.io.FileOutputStream(destino.getPath(), false);
            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n > 0) {
                out.write(buf, 0, n);
                n = in.read(buf);
            }
        } catch (java.io.IOException e) {
            // Una rotacion que falla no puede tumbar al programa: se pierde el archivo viejo y la
            // traza sigue en el nuevo, que es lo que importa.
            return;
        } finally {
            cerrar(in);
            cerrar(out);
        }
        origen.delete();
    }

    private static void cerrar(java.io.Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (java.io.IOException e) {
            // Ver `mover`.
        }
    }

    /**
     * Un flujo que cuenta lo que pasa por el.
     *
     * <p>Hace falta porque el limite es de **bytes escritos** y el manejador escribe caracteres: con
     * una codificacion de ancho variable, contar caracteres daria un limite que no es el que se pidio.
     * Contar aca, del lado de los bytes, es contar lo que el archivo va a ocupar.
     */
    private static class Medidor extends java.io.OutputStream {

        private final java.io.OutputStream destino;
        private long escritos;

        Medidor(java.io.OutputStream destino, long yaEscritos) {
            this.destino = destino;
            this.escritos = yaEscritos;
        }

        long escritos() {
            return this.escritos;
        }

        public void write(int b) throws java.io.IOException {
            this.destino.write(b);
            this.escritos = this.escritos + 1;
        }

        public void write(byte[] b, int off, int len) throws java.io.IOException {
            this.destino.write(b, off, len);
            this.escritos = this.escritos + len;
        }

        public void flush() throws java.io.IOException {
            this.destino.flush();
        }

        public void close() throws java.io.IOException {
            this.destino.close();
        }
    }
}
