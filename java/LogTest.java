import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

/**
 * Prueba de comportamiento del cierre de `java.util.logging`.
 *
 * <p>`run()` devuelve -1 si todo pasa, o el indice de la primera comprobacion que falla. Se corre en
 * las dos VMs --la nuestra y el JDK real-- y tienen que dar lo mismo: el valor no dice "anda", dice
 * "las dos hacen lo mismo", que es lo unico verificable sin un oraculo aparte.
 *
 * <p>Nada de esto mira la consola. `PrintStream.println` es un intrinseco de esta VM que escribe a la
 * salida del proceso sin pasar por el `OutputStream` de abajo, asi que un `System.setOut` a un buffer
 * no captura nada; los manejadores se prueban contra un {@link Cap} o contra un archivo.
 */
public class LogTest {

    private static int n;
    private static int fallo = -1;

    private static void ok(boolean b) {
        if (fallo < 0 && !b) {
            fallo = n;
        }
        n = n + 1;
    }

    private static void eq(Object a, Object b) {
        ok(a == null ? b == null : a.equals(b));
    }

    /** Un manejador que acumula en un texto: la unica forma de mirar lo que sale sin tocar la consola. */
    static class Cap extends Handler {
        StringBuilder sb = new StringBuilder();

        Cap() {
            this.setLevel(Level.ALL);
            this.setFormatter(new SimpleFormatter());
        }

        public void publish(LogRecord r) {
            if (!this.isLoggable(r)) {
                return;
            }
            this.sb.append('[').append(r.getLoggerName()).append('|').append(r.getMessage())
                    .append('|').append(r.getResourceBundle() != null).append('|')
                    .append(r.getResourceBundleName()).append('|')
                    .append(this.getFormatter().formatMessage(r)).append(']');
        }

        public void flush() {
        }

        public void close() {
        }

        String texto() {
            return this.sb.toString();
        }

        void limpiar() {
            this.sb.setLength(0);
        }
    }

    /**
     * Un catalogo de prueba con nombre base propio.
     *
     * <p>Se sobreescribe `getBaseBundleName` en vez de cargarlo con `getBundle`: un catalogo cargado
     * por nombre depende de que el archivo `.class` este en el camino de clases de las dos VMs, y eso
     * es una diferencia de entorno que no tiene nada que ver con lo que se esta probando.
     */
    static class Cat extends java.util.ListResourceBundle {
        protected Object[][] getContents() {
            return new Object[][] {{"saludo", "hola {0}"}, {"chau", "adios"}};
        }

        public String getBaseBundleName() {
            return "LogTestCat";
        }
    }

    private static final Cat CAT = new Cat();

    private static String fm(String msg, Object[] params) {
        LogRecord r = new LogRecord(Level.INFO, msg);
        r.setParameters(params);
        return new SimpleFormatter().formatMessage(r);
    }

    public static int run() {
        n = 0;
        fallo = -1;
        formatMessage();
        registro();
        logger();
        memoria();
        xml();
        permiso();
        corriente();
        configuracion();
        archivo();
        administracion();
        return fallo;
    }

    // ---- Formatter.formatMessage ----------------------------------------------------------------

    private static void formatMessage() {
        Object[] seis = new Object[] {"X", "Y", "Z", "W", "V", "U"};
        eq(fm("a {5} b {0}", seis), "a U b X");
        eq(fm("solo {5}", seis), "solo U");
        eq(fm("dup {0} {0}", new Object[] {"X"}), "dup X X");
        // Un patron mal armado sale crudo en vez de tirar.
        eq(fm("llaves {} sueltas", new Object[] {"X"}), "llaves {} sueltas");
        eq(fm("{ 0 }", new Object[] {"X"}), "{ 0 }");
        // Sin parametros no se toca nada, aunque haya llaves.
        eq(fm("n {0} m", new Object[0]), "n {0} m");
        eq(fm("n {0} m", null), "n {0} m");
        // Las comillas son de MessageFormat: `''` es una comilla y `'...'` es literal.
        eq(fm("a ''b'' {0}", new Object[] {"X"}), "a 'b' X");
        eq(fm("esc '{0}' {1}", new Object[] {"X", "Y"}), "esc {0} Y");
        // Sin ninguna llave el texto no pasa por MessageFormat, asi que las comillas quedan.
        eq(fm("a ''b''", new Object[] {"X"}), "a ''b''");
        // Un indice fuera de rango se deja como estaba.
        eq(fm("out {9}", new Object[] {"X"}), "out {9}");
        eq(fm(null, new Object[] {"X"}), null);

        // Con catalogo, el mensaje es la CLAVE.
        LogRecord r = new LogRecord(Level.INFO, "saludo");
        r.setResourceBundle(CAT);
        eq(new SimpleFormatter().formatMessage(r), "hola {0}");
        r.setParameters(new Object[] {"mundo"});
        eq(new SimpleFormatter().formatMessage(r), "hola mundo");
        LogRecord r2 = new LogRecord(Level.INFO, "ausente");
        r2.setResourceBundle(CAT);
        eq(new SimpleFormatter().formatMessage(r2), "ausente");
    }

    // ---- LogRecord -------------------------------------------------------------------------------

    private static void registro() {
        LogRecord r = new LogRecord(Level.INFO, "x");
        r.setLongThreadID(77L);
        ok(r.getThreadID() == 77);
        ok(r.getLongThreadID() == 77L);
        r.setThreadID(42);
        ok(r.getThreadID() == 42);
        ok(r.getLongThreadID() == 42L);
        r.setLongThreadID((long) Integer.MAX_VALUE);
        ok(r.getThreadID() == Integer.MAX_VALUE);

        // El catalogo y su nombre son dos campos independientes: ninguno arrastra al otro.
        LogRecord r2 = new LogRecord(Level.INFO, "x");
        ok(r2.getResourceBundle() == null);
        ok(r2.getResourceBundleName() == null);
        r2.setResourceBundle(CAT);
        ok(r2.getResourceBundle() == CAT);
        ok(r2.getResourceBundleName() == null);
        r2.setResourceBundleName("otro.nombre");
        ok(r2.getResourceBundle() == CAT);
        eq(r2.getResourceBundleName(), "otro.nombre");
        r2.setResourceBundle(null);
        ok(r2.getResourceBundle() == null);
        eq(r2.getResourceBundleName(), "otro.nombre");
    }

    // ---- Logger ----------------------------------------------------------------------------------

    private static void logger() {
        ok(Logger.getGlobal() == Logger.global);
        eq(Logger.getGlobal().getName(), "global");

        Cap cap = new Cap();
        Logger l = Logger.getLogger("lt.a");
        l.setUseParentHandlers(false);
        l.addHandler(cap);
        l.setLevel(Level.ALL);

        // `log(LogRecord)` toma el registro tal como viene: no le pone el nombre del logger.
        l.log(new LogRecord(Level.INFO, "crudo"));
        eq(cap.texto(), "[null|crudo|false|null|crudo]");

        // Los metodos de conveniencia si se lo ponen.
        cap.limpiar();
        l.info("comodo");
        eq(cap.texto(), "[lt.a|comodo|false|null|comodo]");

        // El catalogo se hereda por el arbol de nombres, y el registro sale ya traducido.
        Logger padre = Logger.getLogger("lt.b");
        padre.setResourceBundle(CAT);
        eq(padre.getResourceBundleName(), "LogTestCat");
        ok(padre.getResourceBundle() == CAT);
        Logger hijo = Logger.getLogger("lt.b.hijo");
        ok(hijo.getResourceBundle() == null);
        ok(hijo.getResourceBundleName() == null);
        Cap cap2 = new Cap();
        hijo.setUseParentHandlers(false);
        hijo.addHandler(cap2);
        hijo.setLevel(Level.ALL);
        hijo.info("saludo");
        eq(cap2.texto(), "[lt.b.hijo|saludo|true|LogTestCat|hola {0}]");
        cap2.limpiar();
        hijo.log(Level.INFO, "saludo", new Object[] {"vos"});
        eq(cap2.texto(), "[lt.b.hijo|saludo|true|LogTestCat|hola vos]");

        // Poner el mismo catalogo dos veces esta bien; poner otro distinto no.
        padre.setResourceBundle(CAT);
        ok(otroCatalogoTira(padre));

        // `logrb` traduce con el catalogo que se le pasa, no con el del logger.
        cap.limpiar();
        l.logrb(Level.INFO, CAT, "chau");
        eq(cap.texto(), "[lt.a|chau|true|LogTestCat|adios]");
        cap.limpiar();
        l.logrb(Level.INFO, "C", "m", CAT, "saludo", new Object[] {"vos"});
        eq(cap.texto(), "[lt.a|saludo|true|LogTestCat|hola vos]");
        cap.limpiar();
        l.logrb(Level.INFO, CAT, "saludo", new RuntimeException("e"));
        eq(cap.texto(), "[lt.a|saludo|true|LogTestCat|hola {0}]");
        // Un catalogo pedido por nombre que no existe deja el nombre y no el catalogo.
        cap.limpiar();
        l.logrb(Level.INFO, "C", "m", "NoExisteEsteCatalogo", "saludo");
        eq(cap.texto(), "[lt.a|saludo|false|NoExisteEsteCatalogo|saludo]");

        // El nivel del logger corta antes que el del manejador.
        cap.limpiar();
        l.setLevel(Level.WARNING);
        l.info("no sale");
        l.warning("sale");
        eq(cap.texto(), "[lt.a|sale|false|null|sale]");
        l.setLevel(Level.ALL);
    }

    static class Otro extends java.util.ListResourceBundle {
        protected Object[][] getContents() {
            return new Object[][] {{"a", "b"}};
        }

        public String getBaseBundleName() {
            return "OtroCatalogo";
        }
    }

    private static boolean otroCatalogoTira(Logger l) {
        try {
            l.setResourceBundle(new Otro());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---- MemoryHandler ---------------------------------------------------------------------------

    private static void memoria() {
        Cap destino = new Cap();
        MemoryHandler m = new MemoryHandler(destino, 3, Level.SEVERE);
        ok(m.getPushLevel() == Level.SEVERE);
        ok(m.getLevel() == Level.ALL);

        // Cuatro en un anillo de tres: se pierde el mas viejo y nada sale todavia.
        m.publish(rec(Level.INFO, "1"));
        m.publish(rec(Level.INFO, "2"));
        m.publish(rec(Level.INFO, "3"));
        m.publish(rec(Level.INFO, "4"));
        eq(destino.texto(), "");
        m.push();
        eq(destino.texto(), "[null|2|false|null|2][null|3|false|null|3][null|4|false|null|4]");
        // El empuje vacia: dos empujes seguidos no escriben lo mismo dos veces.
        m.push();
        eq(destino.texto(), "[null|2|false|null|2][null|3|false|null|3][null|4|false|null|4]");
        // Un mensaje del nivel de empuje vuelca solo.
        destino.limpiar();
        m.publish(rec(Level.SEVERE, "S"));
        eq(destino.texto(), "[null|S|false|null|S]");

        // El nivel propio decide que ENTRA al anillo.
        destino.limpiar();
        m.setLevel(Level.WARNING);
        m.publish(rec(Level.INFO, "descartado"));
        m.push();
        eq(destino.texto(), "");

        // El de empuje se puede mover.
        m.setLevel(Level.ALL);
        m.setPushLevel(Level.INFO);
        ok(m.getPushLevel() == Level.INFO);
        destino.limpiar();
        m.publish(rec(Level.INFO, "ya"));
        eq(destino.texto(), "[null|ya|false|null|ya]");

        ok(tamanoInvalidoTira(destino, 0));
        ok(tamanoInvalidoTira(destino, -1));

        // Cerrar apaga el manejador y descarta lo que quedaba.
        destino.limpiar();
        m.setPushLevel(Level.SEVERE);
        m.publish(rec(Level.INFO, "perdido"));
        m.close();
        ok(m.getLevel() == Level.OFF);
        m.publish(rec(Level.SEVERE, "tarde"));
        eq(destino.texto(), "");

        // Sin configuracion no hay destino, y sin destino no hay manejador.
        ok(sinDestinoTira());
    }

    private static boolean sinDestinoTira() {
        try {
            new MemoryHandler();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static LogRecord rec(Level l, String m) {
        return new LogRecord(l, m);
    }

    private static boolean tamanoInvalidoTira(Handler destino, int tam) {
        try {
            new MemoryHandler(destino, tam, Level.SEVERE);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---- XMLFormatter ----------------------------------------------------------------------------

    private static void xml() {
        // El XML lleva LF y no el salto de la plataforma: lo lee un parser, no una persona.
        String nl = "\n";
        XMLFormatter x = new XMLFormatter();

        // La cabecera anuncia la codificacion del manejador cuando este la declara.
        StreamHandler h = new StreamHandler();
        try {
            h.setEncoding("UTF-8");
        } catch (Exception e) {
            ok(false);
        }
        eq(x.getHead(h), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + nl
                + "<!DOCTYPE log SYSTEM \"logger.dtd\">" + nl + "<log>" + nl);
        ok(x.getHead(null).startsWith("<?xml version=\"1.0\" encoding=\""));
        eq(x.getTail(null), "</log>" + nl);

        LogRecord r = new LogRecord(Level.WARNING, "un <mensaje> & \"otro\"");
        r.setLoggerName("a.b");
        r.setSequenceNumber(7);
        r.setMillis(0);
        r.setLongThreadID(3);
        r.setSourceClassName("C<x>");
        r.setSourceMethodName("m");
        eq(x.format(r), "<record>" + nl
                + "  <date>1970-01-01T00:00:00Z</date>" + nl
                + "  <millis>0</millis>" + nl
                + "  <sequence>7</sequence>" + nl
                + "  <logger>a.b</logger>" + nl
                + "  <level>WARNING</level>" + nl
                + "  <class>C&lt;x&gt;</class>" + nl
                + "  <method>m</method>" + nl
                + "  <thread>3</thread>" + nl
                + "  <message>un &lt;mensaje&gt; &amp; \"otro\"</message>" + nl
                + "</record>" + nl);

        // Sin llaves en el mensaje, los parametros salen aparte; el que no se puede convertir queda
        // marcado en vez de tumbar el registro.
        LogRecord r2 = new LogRecord(Level.INFO, "sin llaves");
        r2.setSequenceNumber(8);
        r2.setMillis(0);
        r2.setLongThreadID(3);
        r2.setParameters(new Object[] {"p1", null});
        eq(x.format(r2), "<record>" + nl
                + "  <date>1970-01-01T00:00:00Z</date>" + nl
                + "  <millis>0</millis>" + nl
                + "  <sequence>8</sequence>" + nl
                + "  <level>INFO</level>" + nl
                + "  <thread>3</thread>" + nl
                + "  <message>sin llaves</message>" + nl
                + "  <param>p1</param>" + nl
                + "  <param>???</param>" + nl
                + "</record>" + nl);

        // Con llaves, los parametros ya estan dentro del mensaje y no se repiten.
        LogRecord r3 = new LogRecord(Level.INFO, "con {0}");
        r3.setSequenceNumber(9);
        r3.setMillis(0);
        r3.setLongThreadID(3);
        r3.setParameters(new Object[] {"p1"});
        ok(x.format(r3).indexOf("<param>") < 0);
        ok(x.format(r3).indexOf("<message>con p1</message>") > 0);

        // Los nanosegundos que los milisegundos no alcanzan a contar van aparte.
        LogRecord r4 = new LogRecord(Level.INFO, "m");
        r4.setInstant(java.time.Instant.ofEpochSecond(1000, 123456));
        r4.setSequenceNumber(1);
        r4.setLongThreadID(1);
        eq(x.format(r4), "<record>" + nl
                + "  <date>1970-01-01T00:16:40.000123456Z</date>" + nl
                + "  <millis>1000000</millis>" + nl
                + "  <nanos>123456</nanos>" + nl
                + "  <sequence>1</sequence>" + nl
                + "  <level>INFO</level>" + nl
                + "  <thread>1</thread>" + nl
                + "  <message>m</message>" + nl
                + "</record>" + nl);

        // Con catalogo aparecen la clave y el nombre del catalogo. Aca el nombre no se fijo, y el
        // escape de un nulo es literalmente `&lt;null&gt;`.
        LogRecord r5 = new LogRecord(Level.INFO, "saludo");
        r5.setResourceBundle(CAT);
        r5.setSequenceNumber(0);
        r5.setMillis(0);
        r5.setLongThreadID(1);
        eq(x.format(r5), "<record>" + nl
                + "  <date>1970-01-01T00:00:00Z</date>" + nl
                + "  <millis>0</millis>" + nl
                + "  <sequence>0</sequence>" + nl
                + "  <level>INFO</level>" + nl
                + "  <thread>1</thread>" + nl
                + "  <message>hola {0}</message>" + nl
                + "  <key>saludo</key>" + nl
                + "  <catalog>&lt;null&gt;</catalog>" + nl
                + "</record>" + nl);

        // Una clave que el catalogo no define no produce `<key>`: no hay traduccion que declarar.
        LogRecord r6 = new LogRecord(Level.INFO, "ausente");
        r6.setResourceBundle(CAT);
        r6.setSequenceNumber(0);
        r6.setMillis(0);
        r6.setLongThreadID(1);
        ok(x.format(r6).indexOf("<key>") < 0);
    }

    // ---- LoggingPermission -----------------------------------------------------------------------

    private static void permiso() {
        LoggingPermission p = new LoggingPermission("control", null);
        eq(p.getName(), "control");
        eq(p.getActions(), "");
        eq(p.toString(), "(\"java.util.logging.LoggingPermission\" \"control\")");
        ok(p.implies(new LoggingPermission("control", "")));
        ok(nombreMaloTira("otro", null));
        ok(nombreMaloTira("control", "read"));
    }

    private static boolean nombreMaloTira(String nombre, String acciones) {
        try {
            new LoggingPermission(nombre, acciones);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---- StreamHandler ---------------------------------------------------------------------------

    private static void corriente() {
        String nl = "\n";
        StreamHandler s = new StreamHandler();
        // Por omision es INFO, no ALL: escribir a un flujo cuesta.
        ok(s.getLevel() == Level.INFO);
        ok(s.getFormatter() instanceof SimpleFormatter);
        ok(s.getEncoding() == null);
        ok(new StreamHandler(new java.io.ByteArrayOutputStream(), new SimpleFormatter())
                .getLevel() == Level.INFO);

        // La cabecera sale con el primer registro, no al abrir.
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        StreamHandler x = new StreamHandler(bo, new XMLFormatter());
        x.setLevel(Level.ALL);
        x.flush();
        ok(bo.size() == 0);
        LogRecord r = new LogRecord(Level.INFO, "m");
        r.setMillis(0);
        r.setSequenceNumber(0);
        r.setLongThreadID(1);
        x.publish(r);
        x.close();
        eq(texto(bo), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + nl
                + "<!DOCTYPE log SYSTEM \"logger.dtd\">" + nl + "<log>" + nl
                + "<record>" + nl
                + "  <date>1970-01-01T00:00:00Z</date>" + nl
                + "  <millis>0</millis>" + nl
                + "  <sequence>0</sequence>" + nl
                + "  <level>INFO</level>" + nl
                + "  <thread>1</thread>" + nl
                + "  <message>m</message>" + nl
                + "</record>" + nl
                + "</log>" + nl);

        // Y si no hubo ningun registro, la cabecera sale igual al cerrar: un documento sin raiz no se
        // puede parsear, y eso es peor que un archivo con un `<log></log>` adentro.
        java.io.ByteArrayOutputStream bo2 = new java.io.ByteArrayOutputStream();
        StreamHandler y = new StreamHandler(bo2, new XMLFormatter());
        y.close();
        eq(texto(bo2), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + nl
                + "<!DOCTYPE log SYSTEM \"logger.dtd\">" + nl + "<log>" + nl + "</log>" + nl);
    }

    private static String texto(java.io.ByteArrayOutputStream bo) {
        try {
            return bo.toString("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    // ---- LogManager ------------------------------------------------------------------------------
    //
    // Va ultimo a proposito: `reset` y `readConfiguration` le sacan los manejadores a todos los
    // loggers, incluidos los que arman las otras secciones.

    static class Cuenta implements Runnable {
        int veces;

        public void run() {
            this.veces = this.veces + 1;
        }
    }

    private static final String CFG =
            "handlers=java.util.logging.MemoryHandler\n"
            + ".level=WARNING\n"
            + "lt.cfg.level=FINE\n"
            + "lt.cfg2.useParentHandlers=false\n"
            + "java.util.logging.MemoryHandler.target=java.util.logging.ConsoleHandler\n"
            + "java.util.logging.MemoryHandler.size=5\n"
            + "java.util.logging.MemoryHandler.push=INFO\n";

    private static void configuracion() {
        java.util.logging.LogManager m = java.util.logging.LogManager.getLogManager();
        // La configuracion por omision es la misma que la que el JDK distribuye.
        eq(m.getProperty("handlers"), "java.util.logging.ConsoleHandler");
        eq(m.getProperty(".level"), "INFO");
        m.checkAccess();

        Logger raiz = Logger.getLogger("");
        Logger previo = Logger.getLogger("lt.a");
        m.reset();
        ok(m.getProperty("handlers") == null);
        ok(raiz.getHandlers().length == 0);
        // La raiz queda en INFO y no en `null`: es la unica que no tiene de quien heredar.
        ok(raiz.getLevel() == Level.INFO);
        ok(previo.getLevel() == null);
        ok(previo.getHandlers().length == 0);

        leer(m, CFG);
        ok(raiz.getLevel() == Level.WARNING);
        ok(raiz.getHandlers().length == 1);
        eq(raiz.getHandlers()[0].getClass().getName(), "java.util.logging.MemoryHandler");
        eq(m.getProperty("handlers"), "java.util.logging.MemoryHandler");
        ok(m.getProperty("nada.de.nada") == null);
        // Lo importante: el nivel configurado alcanza a un logger que todavia no existia.
        ok(Logger.getLogger("lt.cfg").getLevel() == Level.FINE);
        ok(!Logger.getLogger("lt.cfg2").getUseParentHandlers());

        MemoryHandler mh = new MemoryHandler();
        ok(mh.getPushLevel() == Level.INFO);
        ok(mh.getLevel() == Level.ALL);

        Cuenta c = new Cuenta();
        ok(m.addConfigurationListener(c) == m);
        leer(m, CFG);
        ok(c.veces == 1);
        m.removeConfigurationListener(c);
        leer(m, CFG);
        ok(c.veces == 1);
        ok(oyenteNuloTira(m));
        ok(flujoNuloTira(m));

        // `updateConfiguration` mezcla en vez de resetear.
        Logger p = Logger.getLogger("lt.upd");
        ok(p.getLevel() == null);
        actualizar(m, ".level=WARNING\nlt.upd.level=FINE\n");
        ok(p.getLevel() == Level.FINE);
        // Los manejadores si se van cuando la propiedad desaparece.
        ok(raiz.getHandlers().length == 0);
        ok(m.getProperty("handlers") == null);
        // Que el nivel desaparezca NO lo devuelve a heredar.
        actualizar(m, ".level=WARNING\n");
        ok(p.getLevel() == Level.FINE);
        // Ni pisa uno puesto por codigo.
        p.setLevel(Level.SEVERE);
        actualizar(m, ".level=WARNING\n");
        ok(p.getLevel() == Level.SEVERE);
        // Un `handlers` que aparece se aplica, y repetirlo no lo duplica.
        actualizar(m, ".level=WARNING\nhandlers=java.util.logging.ConsoleHandler\n");
        ok(raiz.getHandlers().length == 1);
        actualizar(m, ".level=WARNING\nhandlers=java.util.logging.ConsoleHandler\n");
        ok(raiz.getHandlers().length == 1);

        // Y volver a la configuracion por omision la deja como al arrancar.
        try {
            m.readConfiguration();
        } catch (Exception e) {
            ok(false);
        }
        ok(raiz.getLevel() == Level.INFO);
        ok(raiz.getHandlers().length == 1);
        eq(raiz.getHandlers()[0].getClass().getName(), "java.util.logging.ConsoleHandler");
        eq(m.getProperty("handlers"), "java.util.logging.ConsoleHandler");
    }

    private static void leer(java.util.logging.LogManager m, String cfg) {
        try {
            m.readConfiguration(new java.io.ByteArrayInputStream(cfg.getBytes("UTF-8")));
        } catch (Exception e) {
            ok(false);
        }
    }

    private static void actualizar(java.util.logging.LogManager m, String cfg) {
        try {
            m.updateConfiguration(new java.io.ByteArrayInputStream(cfg.getBytes("UTF-8")), null);
        } catch (Exception e) {
            ok(false);
        }
    }

    private static boolean oyenteNuloTira(java.util.logging.LogManager m) {
        try {
            m.addConfigurationListener(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean flujoNuloTira(java.util.logging.LogManager m) {
        try {
            m.readConfiguration(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- FileHandler -----------------------------------------------------------------------------
    //
    // Va despues de `configuracion()` porque el patron, el limite y el formateador por omision de un
    // `FileHandler` salen de la configuracion, y esa seccion la deja como al arrancar.

    /** Un formateador de tamano previsible: con `SimpleFormatter` el corte por bytes no seria estable. */
    static class Fijo extends Formatter {
        public String format(LogRecord r) {
            return r.getMessage() + ";";
        }
    }

    private static void archivo() {
        java.io.File dir = dirPrueba();
        ok(dir != null && dir.isDirectory());
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        // `%t` y `%h` apuntan al directorio de prueba: asi el patron se ejercita de verdad en las dos
        // VMs, en vez de depender de propiedades que una de ellas no define.
        System.setProperty("java.io.tmpdir", dir.getPath());
        System.setProperty("user.home", dir.getPath());

        // Lo que sale de la configuracion: todo, y el formateador XML.
        java.util.logging.FileHandler h = abrirArchivo("%t/a.log", 0, 1, false);
        ok(h != null);
        if (h == null) {
            return;
        }
        ok(h.getLevel() == Level.ALL);
        ok(h.getFormatter() instanceof XMLFormatter);
        h.publish(rec0("uno"));
        h.close();
        eq(leer(new java.io.File(dir, "a.log")), xmlDe("uno"));

        // Con `append` el documento nuevo se agrega entero, con su cabecera y su cola.
        java.util.logging.FileHandler h2 = abrirArchivo("%t/a.log", 0, 1, true);
        h2.publish(rec0("dos"));
        h2.close();
        eq(leer(new java.io.File(dir, "a.log")), xmlDe("uno") + xmlDe("dos"));

        // Sin `append` se empieza de cero.
        java.util.logging.FileHandler h3 = abrirArchivo("%t/a.log", 0, 1, false);
        h3.publish(rec0("tres"));
        h3.close();
        eq(leer(new java.io.File(dir, "a.log")), xmlDe("tres"));

        // `%h` y `%%`.
        java.util.logging.FileHandler h4 = abrirArchivo("%h/b%%c.log", 0, 1, false);
        h4.publish(rec0("x"));
        h4.close();
        ok(new java.io.File(dir, "b%c.log").exists());

        // La rotacion. Tres bytes por registro y un limite de diez: el corte cae despues del cuarto.
        java.util.logging.FileHandler r = abrirArchivo("%t/rot%g.log", 10, 3, false);
        r.setFormatter(new Fijo());
        int i = 0;
        while (i < 6) {
            r.publish(rec0("m" + i));
            i = i + 1;
        }
        eq(leer(new java.io.File(dir, "rot0.log")), "m4;m5;");
        eq(leer(new java.io.File(dir, "rot1.log")), "m0;m1;m2;m3;");
        ok(!new java.io.File(dir, "rot2.log").exists());
        // La segunda rotacion corre a los dos: el mas viejo baja al 2.
        while (i < 8) {
            r.publish(rec0("m" + i));
            i = i + 1;
        }
        r.close();
        eq(leer(new java.io.File(dir, "rot0.log")), "");
        eq(leer(new java.io.File(dir, "rot1.log")), "m4;m5;m6;m7;");
        eq(leer(new java.io.File(dir, "rot2.log")), "m0;m1;m2;m3;");

        // Con `count` mayor que uno y sin `%g` en el patron, la generacion se agrega al final.
        java.util.logging.FileHandler s = abrirArchivo("%t/sing.log", 10, 2, false);
        s.setFormatter(new Fijo());
        i = 0;
        while (i < 6) {
            s.publish(rec0("m" + i));
            i = i + 1;
        }
        s.close();
        ok(new java.io.File(dir, "sing.log.0").exists());
        ok(new java.io.File(dir, "sing.log.1").exists());
        ok(!new java.io.File(dir, "sing.log").exists());

        // `limit` en cero es sin limite: un solo archivo por mas que se escriba.
        java.io.File[] previos = dir.listFiles();
        java.util.logging.FileHandler z = abrirArchivo("%t/z%g.log", 0, 3, false);
        z.setFormatter(new Fijo());
        i = 0;
        while (i < 40) {
            z.publish(rec0("mensaje bastante largo numero " + i));
            i = i + 1;
        }
        z.close();
        ok(new java.io.File(dir, "z0.log").exists());
        ok(!new java.io.File(dir, "z1.log").exists());

        // `%u` distingue a dos manejadores que pidieron el mismo nombre.
        java.util.logging.FileHandler u1 = abrirArchivo("%t/u%u.log", 0, 1, false);
        java.util.logging.FileHandler u2 = abrirArchivo("%t/u%u.log", 0, 1, false);
        u1.publish(rec0("p"));
        u2.publish(rec0("q"));
        ok(new java.io.File(dir, "u0.log").exists());
        ok(new java.io.File(dir, "u1.log").exists());
        u1.close();
        // Y cerrar suelta el nombre: el siguiente vuelve a tomar el cero.
        java.util.logging.FileHandler u3 = abrirArchivo("%t/u%u.log", 0, 1, false);
        u3.publish(rec0("s"));
        u3.close();
        u2.close();
        ok(!new java.io.File(dir, "u2.log").exists());

        // Cerrar dos veces no hace nada, y publicar despues de cerrar tampoco.
        java.util.logging.FileHandler q = abrirArchivo("%t/q.log", 0, 1, false);
        q.close();
        q.close();
        q.publish(rec0("tarde"));
        eq(leer(new java.io.File(dir, "q.log")), CABECERA_XML + COLA_XML);

        ok(argumentoMaloTira("%t/e.log", -1, 1));
        ok(argumentoMaloTira("%t/e.log", 1, 0));
        ok(argumentoMaloTira("", 0, 1));
        ok(patronNuloTira());
        ok(directorioAusenteTira(dir));

        vaciar(dir);
        dir.delete();
    }

    private static java.io.File dirPrueba() {
        String base = System.getProperty("java.io.tmpdir");
        if (base == null) {
            base = System.getenv("TEMP");
        }
        if (base == null) {
            base = System.getenv("TMP");
        }
        if (base == null) {
            // Esta VM no define ninguna de las tres. El directorio de trabajo es lo unico que queda, y
            // el directorio se borra al terminar.
            base = ".";
        }
        java.io.File d = new java.io.File(base, "kaji-logtest");
        d.mkdirs();
        vaciar(d);
        return d;
    }

    private static void vaciar(java.io.File d) {
        java.io.File[] fs = d.listFiles();
        if (fs == null) {
            return;
        }
        int i = 0;
        while (i < fs.length) {
            fs[i].delete();
            i = i + 1;
        }
    }

    private static java.util.logging.FileHandler abrirArchivo(String patron, long limite, int cuenta,
            boolean agregar) {
        try {
            return new java.util.logging.FileHandler(patron, limite, cuenta, agregar);
        } catch (Exception e) {
            ok(false);
            return null;
        }
    }

    private static LogRecord rec0(String msg) {
        LogRecord r = new LogRecord(Level.INFO, msg);
        r.setMillis(0);
        r.setSequenceNumber(0);
        r.setLongThreadID(1);
        return r;
    }

    private static final String CABECERA_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<!DOCTYPE log SYSTEM \"logger.dtd\">\n<log>\n";

    private static final String COLA_XML = "</log>\n";

    private static String xmlDe(String msg) {
        return CABECERA_XML
                + "<record>\n"
                + "  <date>1970-01-01T00:00:00Z</date>\n"
                + "  <millis>0</millis>\n"
                + "  <sequence>0</sequence>\n"
                + "  <level>INFO</level>\n"
                + "  <thread>1</thread>\n"
                + "  <message>" + msg + "</message>\n"
                + "</record>\n"
                + COLA_XML;
    }

    private static String leer(java.io.File f) {
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(f.getPath());
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n = in.read(buf);
            while (n > 0) {
                bo.write(buf, 0, n);
                n = in.read(buf);
            }
            return bo.toString("UTF-8");
        } catch (java.io.IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException e) {
                    // Nada que hacer.
                }
            }
        }
    }

    private static boolean argumentoMaloTira(String patron, long limite, int cuenta) {
        try {
            new java.util.logging.FileHandler(patron, limite, cuenta, false).close();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean patronNuloTira() {
        try {
            new java.util.logging.FileHandler(null).close();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean directorioAusenteTira(java.io.File dir) {
        try {
            new java.util.logging.FileHandler(dir.getPath() + "/noexiste/x.log").close();
            return false;
        } catch (java.io.IOException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- LoggingMXBean -------------------------------------------------------------------------

    /**
     * La vista de administracion. Lo unico dificil de esta interfaz son los **tres** estados que
     * puede devolver una consulta de nivel --`null`, `""` y un nombre--, que se parecen de a dos y
     * significan cosas distintas; casi todo lo de aca es separarlos.
     */
    private static void administracion() {
        java.util.logging.LoggingMXBean b = java.util.logging.LogManager.getLoggingMXBean();
        ok(b != null);
        // Siempre la misma instancia: hay codigo que la compara con `==` para saber si ya la tenia.
        ok(java.util.logging.LogManager.getLoggingMXBean() == b);
        eq(java.util.logging.LogManager.LOGGING_MXBEAN_NAME, "java.util.logging:type=Logging");

        // Referencias fuertes a proposito: en el JDK el registro guarda los loggers debilmente y uno
        // sin dueno puede desaparecer entre dos lineas de esta prueba.
        Logger uno = Logger.getLogger("kaji.mx.uno");
        Logger dos = Logger.getLogger("kaji.mx.uno.dos");
        uno.setLevel(Level.FINE);

        // Los tres estados de `getLoggerLevel`, que es la razon de ser de este bloque.
        eq(b.getLoggerLevel("kaji.mx.uno"), "FINE");     // tiene nivel propio
        eq(b.getLoggerLevel("kaji.mx.uno.dos"), "");     // existe y hereda
        ok(b.getLoggerLevel("kaji.mx.no.existe") == null);  // no existe
        eq(b.getLoggerLevel(""), "INFO");                // la raiz existe y tiene nivel propio

        // Y los mismos tres en `getParentLoggerName`: `""` es la raiz, `null` es "no hay tal logger".
        eq(b.getParentLoggerName("kaji.mx.uno.dos"), "kaji.mx.uno");
        eq(b.getParentLoggerName(""), "");
        ok(b.getParentLoggerName("kaji.mx.no.existe") == null);

        // Mover el nivel en caliente, que es para lo que existe la interfaz.
        b.setLoggerLevel("kaji.mx.uno", "WARNING");
        eq(b.getLoggerLevel("kaji.mx.uno"), "WARNING");
        ok(uno.getLevel() == Level.WARNING);

        // `null` no es un error: es como se le saca el nivel propio y vuelve a heredar del padre.
        // Sin esto se podria subir el detalle de un servicio y no devolverlo a como estaba.
        b.setLoggerLevel("kaji.mx.uno", null);
        eq(b.getLoggerLevel("kaji.mx.uno"), "");
        ok(uno.getLevel() == null);

        ok(tiraIlegal(b, "kaji.mx.no.existe", "FINE"));      // logger desconocido
        ok(tiraIlegal(b, "kaji.mx.uno", "NO_ES_UN_NIVEL"));  // nivel desconocido
        // Y no lo creo al fallar: preguntar por algo que no esta no puede dejarlo estando.
        ok(b.getLoggerLevel("kaji.mx.no.existe") == null);

        java.util.List<String> nombres = b.getLoggerNames();
        ok(nombres.contains("kaji.mx.uno"));
        ok(nombres.contains("kaji.mx.uno.dos"));
        ok(nombres.contains(""));
        // Una foto y no una vista viva: crear un logger no cambia la lista ya entregada. Es lo que
        // corresponde --cualquier clase crea loggers al cargarse-- y sin esto recorrerla seria una
        // carrera.
        int antes = nombres.size();
        Logger tres = Logger.getLogger("kaji.mx.tres");
        ok(nombres.size() == antes);
        ok(b.getLoggerNames().contains("kaji.mx.tres"));

        // Se miran al final para que el registro no pierda las referencias antes de tiempo.
        ok(dos != null && tres != null);
    }

    private static boolean tiraIlegal(java.util.logging.LoggingMXBean b, String logger, String nivel) {
        try {
            b.setLoggerLevel(logger, nivel);
            return false;
        } catch (IllegalArgumentException esperada) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
