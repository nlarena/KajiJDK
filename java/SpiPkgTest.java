// Prueba de comportamiento de los paquetes nuevos: java.net.spi, javax.management.timer,
// javax.management.loading, javax.security.auth.login, javax.xml.parsers y javax.security.cert.
//
// `run()` devuelve -1 si pasa todo, o el indice del primer caso que falla. Cada caso esta numerado
// en orden y los numeros no se reciclan: si se agrega uno, va al final.
//
// Todo lo de aca corre igual en el JDK y en Kaji. Lo que difiere a proposito --`newDefaultInstance`
// de las fabricas XML, que aca lanza y alla no; `X509Certificate.getInstance`, que alla encuentra un
// parser-- esta afuera de esta prueba y anotado en la documentacion de cada clase.

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.timer.Timer;
import javax.management.timer.TimerNotification;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import java.net.spi.InetAddressResolver.LookupPolicy;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXNotRecognizedException;

public class SpiPkgTest {

    // ---- ayudantes ------------------------------------------------------------------------

    /**
     * Junta lo que llega del Timer, para poder afirmar sobre los envios.
     *
     * <p>Sincronizado: los avisos llegan por el hilo del reloj y se leen desde el de la prueba.
     */
    static class Collector implements NotificationListener {
        private final List<String> seen = new ArrayList<String>();

        public synchronized void handleNotification(Notification n, Object handback) {
            TimerNotification tn = (TimerNotification) n;
            seen.add(n.getType() + "#" + tn.getNotificationID());
        }

        synchronized int size() {
            return this.seen.size();
        }

        synchronized String first() {
            return this.seen.isEmpty() ? null : this.seen.get(0);
        }

        synchronized void clear() {
            this.seen.clear();
        }
    }

    /** Un modulo de login de juguete, con el resultado decidido de antemano. */
    public static class FakeModule implements LoginModule {
        static final List<String> trace = new ArrayList<String>();
        private String label;
        private boolean loginResult;
        private boolean loginThrows;

        public void initialize(Subject subject, CallbackHandler handler,
                               Map<String, ?> sharedState, Map<String, ?> options) {
            this.label = String.valueOf(options.get("label"));
            this.loginResult = "true".equals(String.valueOf(options.get("result")));
            this.loginThrows = "true".equals(String.valueOf(options.get("throws")));
        }

        public boolean login() throws LoginException {
            trace.add("login:" + this.label);
            if (this.loginThrows) {
                throw new FailedLoginException(this.label);
            }
            return this.loginResult;
        }

        public boolean commit() {
            trace.add("commit:" + this.label);
            return true;
        }

        public boolean abort() {
            trace.add("abort:" + this.label);
            return true;
        }

        public boolean logout() {
            trace.add("logout:" + this.label);
            return true;
        }
    }

    /** Una configuracion armada en memoria, sin archivo. */
    static class MemoryConfiguration extends Configuration {
        private final Map<String, AppConfigurationEntry[]> byName =
            new HashMap<String, AppConfigurationEntry[]>();

        void put(String name, AppConfigurationEntry[] entries) {
            this.byName.put(name, entries);
        }

        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return this.byName.get(name);
        }
    }

    /** Una entrada de configuracion con las opciones que lee {@link FakeModule}. */
    static AppConfigurationEntry entry(String label, LoginModuleControlFlag flag, boolean result,
                                       boolean throwing) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("label", label);
        options.put("result", result ? "true" : "false");
        options.put("throws", throwing ? "true" : "false");
        return new AppConfigurationEntry("SpiPkgTest$FakeModule", flag, options);
    }

    // ---- la prueba ------------------------------------------------------------------------

    public static int run() {
        // --- java.net.spi.InetAddressResolver.LookupPolicy ---
        if (LookupPolicy.IPV4 != 1) return 0;
        if (LookupPolicy.IPV6 != 2) return 1;
        if (LookupPolicy.IPV4_FIRST != 4) return 2;
        if (LookupPolicy.IPV6_FIRST != 8) return 3;
        if (LookupPolicy.of(1).characteristics() != 1) return 4;
        if (LookupPolicy.of(3).characteristics() != 3) return 5;
        // Bits desconocidos pasan tal cual mientras las reglas basicas se cumplan.
        if (LookupPolicy.of(17).characteristics() != 17) return 6;
        if (!policyRejects(0)) return 7;
        if (!policyRejects(4)) return 8;
        if (!policyRejects(8)) return 9;
        // IPV4_FIRST sin IPV4.
        if (!policyRejects(6)) return 10;
        // IPV6_FIRST sin IPV6.
        if (!policyRejects(9)) return 11;
        // Los dos ordenes a la vez.
        if (!policyRejects(15)) return 12;
        if (LookupPolicy.of(5).characteristics() != 5) return 13;
        if (LookupPolicy.of(10).characteristics() != 10) return 14;

        int timerResult = timerCases();
        if (timerResult >= 0) {
            return timerResult;
        }

        // --- javax.security.auth.login ---
        if (!"LoginModuleControlFlag: required"
                .equals(LoginModuleControlFlag.REQUIRED.toString())) return 66;
        if (!"LoginModuleControlFlag: requisite"
                .equals(LoginModuleControlFlag.REQUISITE.toString())) return 67;
        if (!"LoginModuleControlFlag: sufficient"
                .equals(LoginModuleControlFlag.SUFFICIENT.toString())) return 68;
        if (!"LoginModuleControlFlag: optional"
                .equals(LoginModuleControlFlag.OPTIONAL.toString())) return 69;

        Map<String, Object> options = new HashMap<String, Object>();
        if (!entryRejects(null, LoginModuleControlFlag.REQUIRED, options)) return 70;
        if (!entryRejects("", LoginModuleControlFlag.REQUIRED, options)) return 71;
        if (!entryRejects("m", null, options)) return 72;
        if (!entryRejects("m", LoginModuleControlFlag.REQUIRED, null)) return 73;

        AppConfigurationEntry one = new AppConfigurationEntry(
            "m", LoginModuleControlFlag.OPTIONAL, options);
        if (!"m".equals(one.getLoginModuleName())) return 74;
        if (one.getControlFlag() != LoginModuleControlFlag.OPTIONAL) return 75;
        // Vista y no copia: lo que se agregue despues se ve.
        options.put("k", "v");
        if (one.getOptions().size() != 1) return 76;
        try {
            ((Map) one.getOptions()).put("x", "y");
            return 77;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }

        MemoryConfiguration config = new MemoryConfiguration();
        // Un nombre sin modulos configurados falla al construir el contexto, no al entrar.
        try {
            new LoginContext("sin-nada", null, null, config);
            return 78;
        } catch (LoginException expected) {
            // asi tiene que ser
        }

        // Dos REQUIRED que andan: los dos hacen commit, en orden.
        config.put("dos", new AppConfigurationEntry[] {
            entry("a", LoginModuleControlFlag.REQUIRED, true, false),
            entry("b", LoginModuleControlFlag.REQUIRED, true, false)});
        FakeModule.trace.clear();
        try {
            LoginContext context = new LoginContext("dos", null, null, config);
            context.login();
            if (context.getSubject() == null) return 79;
        } catch (LoginException e) {
            return 80;
        }
        if (!"login:a,login:b,commit:a,commit:b".equals(joined())) return 81;

        // Un SUFFICIENT que anda corta: el segundo no se llega a correr.
        config.put("corta", new AppConfigurationEntry[] {
            entry("a", LoginModuleControlFlag.SUFFICIENT, true, false),
            entry("b", LoginModuleControlFlag.REQUIRED, true, false)});
        FakeModule.trace.clear();
        try {
            new LoginContext("corta", null, null, config).login();
        } catch (LoginException e) {
            return 82;
        }
        if (!"login:a,commit:a".equals(joined())) return 83;

        // Un REQUIRED que falla no corta, pero el login se pierde y todos abortan.
        config.put("sigue", new AppConfigurationEntry[] {
            entry("a", LoginModuleControlFlag.REQUIRED, false, true),
            entry("b", LoginModuleControlFlag.OPTIONAL, true, false)});
        FakeModule.trace.clear();
        try {
            new LoginContext("sigue", null, null, config).login();
            return 84;
        } catch (LoginException expected) {
            if (!"a".equals(expected.getMessage())) return 85;
        }
        if (!"login:a,login:b,abort:a,abort:b".equals(joined())) return 86;

        // Un REQUISITE que falla corta ahi mismo.
        config.put("frena", new AppConfigurationEntry[] {
            entry("a", LoginModuleControlFlag.REQUISITE, false, true),
            entry("b", LoginModuleControlFlag.REQUIRED, true, false)});
        FakeModule.trace.clear();
        try {
            new LoginContext("frena", null, null, config).login();
            return 87;
        } catch (LoginException expected) {
            // asi tiene que ser
        }
        // El aborto llega a `b` aunque nunca haya corrido: ver la nota de LoginContext.
        if (!"login:a,abort:a,abort:b".equals(joined())) return 88;

        // Una cadena entera de opcionales que se desentienden no es un login exitoso.
        config.put("nadie", new AppConfigurationEntry[] {
            entry("a", LoginModuleControlFlag.OPTIONAL, false, false),
            entry("b", LoginModuleControlFlag.OPTIONAL, false, false)});
        FakeModule.trace.clear();
        try {
            new LoginContext("nadie", null, null, config).login();
            return 89;
        } catch (LoginException expected) {
            if (!"Login Failure: all modules ignored".equals(expected.getMessage())) return 90;
        }

        // Logout despues de un login exitoso.
        FakeModule.trace.clear();
        try {
            LoginContext context = new LoginContext("dos", null, null, config);
            context.login();
            context.logout();
        } catch (LoginException e) {
            return 91;
        }
        if (!"login:a,login:b,commit:a,commit:b,logout:a,logout:b".equals(joined())) return 92;

        // Un sujeto propio se respeta y se devuelve el mismo.
        Subject mine = new Subject();
        try {
            LoginContext context = new LoginContext("dos", mine, null, config);
            context.login();
            if (context.getSubject() != mine) return 93;
        } catch (LoginException e) {
            return 94;
        }

        // --- javax.xml.parsers ---
        FactoryConfigurationError plain = new FactoryConfigurationError("boom");
        if (!"boom".equals(plain.getMessage())) return 95;
        if (plain.getException() != null) return 96;
        if (plain.getCause() != null) return 97;
        Exception inner = new IllegalStateException("dentro");
        // El constructor de una sola causa arma el mensaje con el `toString` de la causa, no con su
        // mensaje: sin eso, un error de configuracion se imprimiria sin decir de que tipo fue.
        FactoryConfigurationError wrapped = new FactoryConfigurationError(inner);
        if (!inner.toString().equals(wrapped.getMessage())) return 98;
        if (wrapped.getException() != inner) return 99;
        if (wrapped.getCause() != inner) return 100;
        FactoryConfigurationError both = new FactoryConfigurationError(inner, "afuera");
        if (!"afuera".equals(both.getMessage())) return 101;
        if (both.getCause() != inner) return 102;
        // Con mensaje null y causa, contesta el mensaje de la causa.
        FactoryConfigurationError fallback = new FactoryConfigurationError(inner, null);
        if (!"dentro".equals(fallback.getMessage())) return 123;

        // Una fabrica que no existe es un error de configuracion, no una excepcion.
        try {
            DocumentBuilderFactory.newInstance("no.hay.Tal", null);
            return 103;
        } catch (FactoryConfigurationError expected) {
            // asi tiene que ser
        }
        try {
            SAXParserFactory.newInstance("no.hay.Tal", null);
            return 104;
        } catch (FactoryConfigurationError expected) {
            // asi tiene que ser
        }

        // Los valores por omision de las banderas: todas false salvo expandEntityReferences.
        DocumentBuilderFactory dbf = new StubDocumentBuilderFactory();
        if (dbf.isNamespaceAware()) return 105;
        if (dbf.isValidating()) return 106;
        if (dbf.isIgnoringElementContentWhitespace()) return 107;
        if (!dbf.isExpandEntityReferences()) return 108;
        if (dbf.isIgnoringComments()) return 109;
        if (dbf.isCoalescing()) return 110;
        dbf.setNamespaceAware(true);
        if (!dbf.isNamespaceAware()) return 111;
        // Pedir XInclude en false no hace nada; en true lanza.
        dbf.setXIncludeAware(false);
        try {
            dbf.setXIncludeAware(true);
            return 112;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }
        try {
            dbf.isXIncludeAware();
            return 113;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }

        SAXParserFactory spf = new StubSAXParserFactory();
        if (spf.isNamespaceAware()) return 114;
        if (spf.isValidating()) return 115;
        spf.setValidating(true);
        if (!spf.isValidating()) return 116;
        spf.setXIncludeAware(false);
        try {
            spf.setXIncludeAware(true);
            return 117;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }

        // --- javax.xml.validation, y los getSchema/setSchema que destraba ---
        //
        // Los defaults de las tres clases contestan igual: NullPointerException con ese mensaje si
        // el nombre es null, y SAXNotRecognizedException con el nombre si no lo conocen. Que el
        // orden sea ese importa -- preguntar por null es un error de quien llama, no una bandera que
        // no se reconoce.
        StubValidator validator = new StubValidator();
        if (!recognizesNothing(validator)) return 124;
        if (!rejectsNullName(validator)) return 125;
        StubValidatorHandler handler = new StubValidatorHandler();
        if (!recognizesNothing(handler)) return 126;
        if (!rejectsNullName(handler)) return 127;

        // Un lenguaje de esquema que nadie soporta es un error del pedido, no de configuracion.
        try {
            SchemaFactory.newInstance("http://no.hay/tal-lenguaje");
            return 128;
        } catch (IllegalArgumentException expected) {
            // asi tiene que ser
        }
        // Null es distinto: ni siquiera es un lenguaje.
        try {
            SchemaFactory.newInstance((String) null);
            return 129;
        } catch (NullPointerException expected) {
            // asi tiene que ser
        }

        // Los cuatro miembros que javax.xml.validation destraba en javax.xml.parsers.
        try {
            dbf.getSchema();
            return 130;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }
        try {
            dbf.setSchema(null);
            return 131;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }
        try {
            spf.getSchema();
            return 132;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }
        try {
            spf.setSchema(null);
            return 133;
        } catch (UnsupportedOperationException expected) {
            // asi tiene que ser
        }

        // --- org.w3c.dom.ls ---
        LSException parseErr = new LSException(LSException.PARSE_ERR, "roto");
        if (parseErr.code != 81) return 134;
        if (!"roto".equals(parseErr.getMessage())) return 135;
        if (LSException.SERIALIZE_ERR != 82) return 136;
        // El codigo es un campo publico, no un getter: se puede cambiar desde afuera.
        parseErr.code = LSException.SERIALIZE_ERR;
        if (parseErr.code != 82) return 137;
        if (org.w3c.dom.ls.DOMImplementationLS.MODE_SYNCHRONOUS != 1) return 138;
        if (org.w3c.dom.ls.DOMImplementationLS.MODE_ASYNCHRONOUS != 2) return 139;
        if (org.w3c.dom.ls.LSParserFilter.FILTER_ACCEPT != 1) return 140;
        if (org.w3c.dom.ls.LSParserFilter.FILTER_REJECT != 2) return 141;
        if (org.w3c.dom.ls.LSParserFilter.FILTER_SKIP != 3) return 142;
        if (org.w3c.dom.ls.LSParserFilter.FILTER_INTERRUPT != 4) return 143;
        if (org.w3c.dom.ls.LSParser.ACTION_APPEND_AS_CHILDREN != 1) return 144;
        if (org.w3c.dom.ls.LSParser.ACTION_REPLACE_CHILDREN != 2) return 145;
        if (org.w3c.dom.ls.LSParser.ACTION_INSERT_BEFORE != 3) return 146;
        if (org.w3c.dom.ls.LSParser.ACTION_INSERT_AFTER != 4) return 147;
        if (org.w3c.dom.ls.LSParser.ACTION_REPLACE != 5) return 148;

        // --- javax.security.cert ---
        javax.security.cert.CertificateException certError =
            new javax.security.cert.CertificateEncodingException("x");
        if (!(certError instanceof javax.security.cert.CertificateException)) return 118;
        javax.security.cert.CertificateException expired =
            new javax.security.cert.CertificateExpiredException();
        if (expired.getMessage() != null) return 119;
        javax.security.cert.CertificateException parsing =
            new javax.security.cert.CertificateParsingException("mal");
        if (!"mal".equals(parsing.getMessage())) return 120;
        // Vencido y todavia-no-valido son ramas distintas y no se implican.
        javax.security.cert.CertificateException notYet =
            new javax.security.cert.CertificateNotYetValidException();
        if (notYet instanceof javax.security.cert.CertificateExpiredException) return 121;

        return -1;
    }


    /**
     * Los casos del Timer, aparte para poder pararlo pase lo que pase.
     *
     * <p>El hilo del reloj no es demonio: si una afirmacion falla y se vuelve sin parar el reloj, la
     * maquina virtual no termina nunca y la prueba queda colgada en vez de fallar.
     *
     * @return el indice del primer caso que falla, o -1
     */
    static int timerCases() {
        Timer timer = new Timer();
        try {
            return timerBody(timer);
        } finally {
            timer.stop();
        }
    }

    /** Espera hasta que lleguen {@code wanted} avisos, o se cansa. */
    static boolean await(Collector collector, int wanted) {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            if (collector.size() >= wanted) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return collector.size() >= wanted;
    }

    /** Espera un rato corto y confirma que <b>no</b> llego nada. */
    static boolean awaitNothing(Collector collector) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return collector.size() == 0;
    }

    /** Los casos del Timer. El reloj lo para {@link #timerCases}. */
    static int timerBody(Timer timer) {
        if (Timer.ONE_SECOND != 1000L) return 15;
        if (Timer.ONE_MINUTE != 60000L) return 16;
        if (Timer.ONE_HOUR != 3600000L) return 17;
        if (Timer.ONE_DAY != 86400000L) return 18;
        if (Timer.ONE_WEEK != 604800000L) return 19;
        if (timer.isActive()) return 20;
        if (!timer.isEmpty()) return 21;
        if (timer.getNbNotifications() != 0) return 22;

        Date future = new Date(System.currentTimeMillis() + 3600000L);
        Integer first = timer.addNotification("zeta", "m1", "u1", future);
        Integer second = timer.addNotification("alfa", "m2", null, future, 1000L, 3L, true);
        if (first.intValue() != 1) return 23;
        if (second.intValue() != 2) return 24;
        if (timer.getNbNotifications() != 2) return 25;
        if (!"zeta".equals(timer.getNotificationType(first))) return 26;
        if (!"m1".equals(timer.getNotificationMessage(first))) return 27;
        if (!"u1".equals(timer.getNotificationUserData(first))) return 28;
        if (timer.getPeriod(first).longValue() != 0L) return 29;
        if (timer.getNbOccurences(first).longValue() != 0L) return 30;
        if (timer.getFixedRate(first).booleanValue()) return 31;
        if (timer.getPeriod(second).longValue() != 1000L) return 32;
        if (timer.getNbOccurences(second).longValue() != 3L) return 33;
        if (!timer.getFixedRate(second).booleanValue()) return 34;
        // Un identificador que no existe se contesta con null y no con una excepcion.
        if (timer.getNotificationType(Integer.valueOf(999)) != null) return 35;
        if (timer.getDate(Integer.valueOf(999)) != null) return 36;
        if (timer.getPeriod(Integer.valueOf(999)) != null) return 37;

        Vector<Integer> all = timer.getAllNotificationIDs();
        if (all.size() != 2) return 38;
        if (timer.getNotificationIDs("alfa").size() != 1) return 39;
        // Un tipo que nadie inscribio da vacio, que tampoco es un error.
        if (timer.getNotificationIDs("nada").size() != 0) return 40;

        // Los tipos declarados salen de las inscripciones vivas, ordenados.
        MBeanNotificationInfo[] info = timer.getNotificationInfo();
        if (info.length != 1) return 41;
        String[] declared = info[0].getNotifTypes();
        if (declared.length != 2) return 42;
        if (!"alfa".equals(declared[0])) return 43;
        if (!"zeta".equals(declared[1])) return 44;
        if (!"javax.management.timer.TimerNotification".equals(info[0].getName())) return 45;

        // Dar de baja algo que no existe es un error; algo que existe, no.
        try {
            timer.removeNotification(Integer.valueOf(999));
            return 46;
        } catch (InstanceNotFoundException expected) {
            // asi tiene que ser
        }
        try {
            timer.removeNotifications("nada");
            return 47;
        } catch (InstanceNotFoundException expected) {
            // asi tiene que ser
        }
        try {
            timer.removeNotification(first);
        } catch (InstanceNotFoundException e) {
            return 48;
        }
        if (timer.getNbNotifications() != 1) return 49;
        timer.removeAllNotifications();
        if (!timer.isEmpty()) return 50;
        // Con la tabla vacia, los identificadores vuelven a empezar en 1.
        Integer restarted = timer.addNotification("otra", "m", null, future);
        if (restarted.intValue() != 1) return 51;
        timer.removeAllNotifications();

        // Los argumentos invalidos de addNotification.
        if (!timerRejects(timer, null, 0L, 0L)) return 63;
        if (!timerRejects(timer, future, -1L, 0L)) return 64;
        if (!timerRejects(timer, future, 10L, -1L)) return 65;

        return -1;
    }


    /**
     * Los casos que <b>no</b> corren en el JDK, y por que.
     *
     * <p>Todos son del mismo lugar: que hace un {@link Timer} al arrancar con una notificacion cuya
     * fecha ya paso. La regla esta especificada, pero el JDK la implementa con una carrera --programa
     * la atrasada para ya y recien despues decide si darla de baja-- asi que lo que se observa
     * depende de cuan rapido arranque el hilo del reloj. Medido: en una maquina virtual recien
     * arrancada da lo que dice la especificacion y en la misma, caliente, da lo contrario.
     *
     * <p>KajiLibrary no tiene esa carrera --la baja pasa antes de programar nada-- asi que aca si se
     * puede afirmar. Los numeros siguen la misma serie que {@link #run}.
     *
     * @return el indice del primer caso que falla, o -1
     */
    public static int runKaji() {
        Timer timer = new Timer();
        try {
            return kajiTimerBody(timer);
        } finally {
            timer.stop();
        }
    }

    /** El cuerpo de {@link #runKaji}; el reloj lo para el que llama. */
    static int kajiTimerBody(Timer timer) {
        Date future = new Date(System.currentTimeMillis() + 3600000L);
        // Las que quedaron atrasadas: la de una sola vez se descarta y sale de la tabla.
        //
        // Los avisos salen por el hilo del reloj y no adentro de `start`, asi que donde se espera un
        // aviso hay que esperarlo de verdad y no mirar en el acto.
        //
        Collector collector = new Collector();
        timer.addNotificationListener(collector, null, null);
        Date past = new Date(System.currentTimeMillis() - 5000L);
        timer.addNotification("late.once", "m", null, past);
        timer.start();
        if (!timer.isActive()) return 52;
        if (!awaitNothing(collector)) return 53;
        if (!timer.isEmpty()) return 54;
        timer.stop();
        if (timer.isActive()) return 55;

        // Con sendPastNotifications se manda una vez y despues se descarta.
        timer.setSendPastNotifications(true);
        if (!timer.getSendPastNotifications()) return 56;
        Integer late = timer.addNotification("late.sent", "m", null, past);
        timer.start();
        if (!await(collector, 1)) return 57;
        if (!("late.sent#" + late).equals(collector.first())) return 58;
        if (!timer.isEmpty()) return 59;
        timer.stop();

        // La periodica atrasada no se descarta: se corre al futuro.
        //
        // El periodo es de una hora a proposito. Con uno corto, la fecha corrida cae a menos de un
        // periodo de ahora y el aviso puede salir de verdad mientras la prueba espera: estaria
        // midiendo la duracion de la espera y no la regla.
        collector.clear();
        timer.setSendPastNotifications(false);
        Integer periodic =
            timer.addNotification("late.periodic", "m", null, past, Timer.ONE_HOUR);
        timer.start();
        if (timer.isEmpty()) return 60;
        if (!awaitNothing(collector)) return 61;
        // Corrida de a periodos enteros desde la original, no puesta en "ahora".
        Date moved = timer.getDate(periodic);
        if (moved == null || moved.getTime() <= System.currentTimeMillis()) return 62;
        if ((moved.getTime() - past.getTime()) % Timer.ONE_HOUR != 0L) return 122;
        timer.stop();
        timer.removeAllNotifications();

        return -1;
    }

    /** Los rastros de {@link FakeModule}, en una sola cadena comparable. */
    static String joined() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FakeModule.trace.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(FakeModule.trace.get(i));
        }
        return sb.toString();
    }

    /** Si esa combinacion de bits es rechazada. */
    static boolean policyRejects(int characteristics) {
        try {
            LookupPolicy.of(characteristics);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /** Si el Timer rechaza esos argumentos. */
    static boolean timerRejects(Timer timer, Date date, long period, long occurrences) {
        try {
            timer.addNotification("t", "m", null, date, period, occurrences);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /** Si AppConfigurationEntry rechaza esos argumentos. */
    static boolean entryRejects(String name, LoginModuleControlFlag flag,
                                Map<String, Object> options) {
        try {
            new AppConfigurationEntry(name, flag, options);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }


    /** Si las cuatro banderas y propiedades del validador contestan que no conocen el nombre. */
    static boolean recognizesNothing(Validator v) {
        try {
            v.getFeature("x");
            return false;
        } catch (SAXNotRecognizedException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Idem para el manejador, que no comparte tipo con el validador. */
    static boolean recognizesNothing(ValidatorHandler v) {
        try {
            v.getFeature("x");
            return false;
        } catch (SAXNotRecognizedException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Si preguntar por un nombre null da NullPointerException y no la de SAX. */
    static boolean rejectsNullName(Validator v) {
        try {
            v.getFeature(null);
            return false;
        } catch (NullPointerException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Idem para el manejador. */
    static boolean rejectsNullName(ValidatorHandler v) {
        try {
            v.getFeature(null);
            return false;
        } catch (NullPointerException expected) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Lo minimo para poder probar los defaults de {@link Validator}. */
    static class StubValidator extends Validator {
        public void reset() {
        }

        public void validate(javax.xml.transform.Source source,
                             javax.xml.transform.Result result) {
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
        }

        public ErrorHandler getErrorHandler() {
            return null;
        }

        public void setResourceResolver(LSResourceResolver resolver) {
        }

        public LSResourceResolver getResourceResolver() {
            return null;
        }
    }

    /** Idem para {@link ValidatorHandler}, que ademas es un ContentHandler entero. */
    static class StubValidatorHandler extends ValidatorHandler {
        public void setContentHandler(ContentHandler receiver) {
        }

        public ContentHandler getContentHandler() {
            return null;
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
        }

        public ErrorHandler getErrorHandler() {
            return null;
        }

        public void setResourceResolver(LSResourceResolver resolver) {
        }

        public LSResourceResolver getResourceResolver() {
            return null;
        }

        public TypeInfoProvider getTypeInfoProvider() {
            return null;
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() {
        }

        public void endDocument() {
        }

        public void startPrefixMapping(String prefix, String uri) {
        }

        public void endPrefixMapping(String prefix) {
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) {
        }

        public void endElement(String uri, String localName, String qName) {
        }

        public void characters(char[] ch, int start, int length) {
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
        }

        public void processingInstruction(String target, String data) {
        }

        public void skippedEntity(String name) {
        }
    }

    /** Lo minimo para poder probar las banderas de la clase base. */
    static class StubDocumentBuilderFactory extends DocumentBuilderFactory {
        public javax.xml.parsers.DocumentBuilder newDocumentBuilder() {
            return null;
        }

        public void setAttribute(String name, Object value) {
        }

        public Object getAttribute(String name) {
            return null;
        }

        public void setFeature(String name, boolean value) {
        }

        public boolean getFeature(String name) {
            return false;
        }
    }

    /** Idem para SAX. */
    static class StubSAXParserFactory extends SAXParserFactory {
        public javax.xml.parsers.SAXParser newSAXParser() {
            return null;
        }

        public void setFeature(String name, boolean value) {
        }

        public boolean getFeature(String name) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }

    /** Para correr a mano solo la parte que no comparte el JDK. */
    public static void kaji(String[] args) {
        System.out.println(runKaji());
    }
}
