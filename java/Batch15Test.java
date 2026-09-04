import java.awt.desktop.AppEvent;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.PrintFilesEvent;
import java.awt.desktop.QuitStrategy;
import java.awt.desktop.UserSessionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.xml.catalog.Catalog;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;

/**
 * Comportamiento de javax.management.remote, java.rmi, java.lang.management, java.awt.desktop,
 * javax.xml.catalog, javax.sound.sampled, javax.sound.midi y las tablas de imageio.
 *
 * <p>Cada caso vale contra el JDK 25 real y contra KajiJDK; {@link #runKaji} tiene lo que diverge a
 * proposito. {@link #run} devuelve -1 si pasan todos, o el indice del primero que falla.
 */
public class Batch15Test {

    public static int run() {
        int i = 0;

        // ---- javax.management.remote: JMXServiceURL --------------------------
        try {
            JMXServiceURL u = new JMXServiceURL("service:jmx:rmi://localhost:1099/jndi/x");
            if (!u.getProtocol().equals("rmi") || !u.getHost().equals("localhost")
                || u.getPort() != 1099 || !u.getURLPath().equals("/jndi/x")) {
                return i;
            }
            i++;
            // El protocolo se baja a minusculas; el host conserva las mayusculas.
            JMXServiceURL c = new JMXServiceURL("service:jmx:RMI://HOST:99");
            if (!c.getProtocol().equals("rmi") || !c.getHost().equals("HOST")) {
                return i;
            }
            i++;
            if (!c.toString().equals("service:jmx:rmi://HOST:99")) {
                return i;
            }
            i++;
            // El puerto 0 no se escribe.
            if (!new JMXServiceURL("service:jmx:rmi://h:0").toString()
                    .equals("service:jmx:rmi://h")) {
                return i;
            }
            i++;
            // Host vacio es legal y significa la maquina local sin nombrarla.
            JMXServiceURL e = new JMXServiceURL("service:jmx:rmi://");
            if (!e.getHost().equals("") || e.getPort() != 0 || !e.getURLPath().equals("")) {
                return i;
            }
            i++;
            // Un IPv6 sale entre corchetes y se guarda sin ellos.
            JMXServiceURL v6 = new JMXServiceURL("service:jmx:rmi://[::1]:9");
            if (!v6.getHost().equals("::1")
                || !v6.toString().equals("service:jmx:rmi://[::1]:9")) {
                return i;
            }
            i++;
            // El camino puede empezar con punto y coma.
            if (!new JMXServiceURL("service:jmx:rmi://h;q=1").getURLPath().equals(";q=1")) {
                return i;
            }
            i++;
            // Protocolo null significa jmxmp.
            if (!new JMXServiceURL(null, "h", 1).getProtocol().equals("jmxmp")) {
                return i;
            }
            i++;
            // equals ignora mayusculas en protocolo y host.
            if (!new JMXServiceURL("service:jmx:RMI://Host:9")
                    .equals(new JMXServiceURL("service:jmx:rmi://host:9"))) {
                return i;
            }
            i++;
        } catch (MalformedURLException e) {
            return i;
        }
        // La gramatica del host, que es mas estricta de lo que parece.
        if (!badHost("1.2.3") || !badHost("1.2.3.4.5") || !badHost("256.1.1.1")
            || !badHost("abc.123") || !badHost("a.1b") || !badHost("bad-") || !badHost("a..b")
            || !badHost("a.") || !badHost("a_b")) {
            return i;
        }
        i++;
        if (badHost("1.2.3.4") || badHost("255.255.255.255") || badHost("12a.b")
            || badHost("123.abc") || badHost("1234") || badHost("a-1") || badHost("A.B")
            || badHost("ab-cd.ef")) {
            return i;
        }
        i++;
        // Un host con dos puntos se valida como IPv6, valido o no.
        if (!badHost("h:bad") || !badHost("1:2") || !badHost("1::2::3") || !badHost("G::1")
            || !badHost(":::")) {
            return i;
        }
        i++;
        if (badHost("::") || badHost("1:2:3:4:5:6:7:8") || badHost("::ffff:1.2.3.4")
            || badHost("abcd:ef01::") || badHost("1:2:3:4:5:6:1.2.3.4")) {
            return i;
        }
        i++;
        if (!badUrl("nope") || !badUrl("service:jmx:") || !badUrl("service:jmx:9bad://h")) {
            return i;
        }
        i++;

        // ---- javax.management.remote: el resto -------------------------------
        JMXPrincipal p = new JMXPrincipal("bob");
        if (!p.getName().equals("bob") || !p.toString().equals("JMXPrincipal:  bob")) {
            return i;
        }
        i++;
        if (!p.equals(new JMXPrincipal("bob")) || p.hashCode() != "bob".hashCode()) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new JMXPrincipal(null);
            }
        })) {
            return i;
        }
        i++;
        javax.management.Notification n = new javax.management.Notification("t", "src", 1L);
        TargetedNotification tn = new TargetedNotification(n, Integer.valueOf(7));
        if (tn.getListenerID().intValue() != 7 || tn.getNotification() != n) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new TargetedNotification(null, Integer.valueOf(1));
            }
        })) {
            return i;
        }
        i++;
        NotificationResult nr = new NotificationResult(1L, 2L, new TargetedNotification[] { tn });
        if (!nr.toString().equals("NotificationResult: earliest=1; next=2; nnotifs=1")) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new NotificationResult(1L, 2L, null);
            }
        })) {
            return i;
        }
        i++;

        // ---- java.rmi --------------------------------------------------------
        RemoteException r0 = new RemoteException();
        if (r0.getMessage() != null || r0.getCause() != null || r0.detail != null) {
            return i;
        }
        i++;
        Exception cause = new Exception("c");
        RemoteException re = new RemoteException("m", cause);
        // El detalle y la causa son lo mismo, y el mensaje los pega.
        if (re.getCause() != cause || re.detail != cause
            || !re.getMessage().equals("m; nested exception is: \n\tjava.lang.Exception: c")) {
            return i;
        }
        i++;
        // El constructor ya fijo la causa, aunque haya sido a null.
        final RemoteException r1 = new RemoteException("solo");
        if (!ise(new Runnable() {
            public void run() {
                r1.initCause(new Exception("x"));
            }
        })) {
            return i;
        }
        i++;
        Error err = new Error("e");
        if (new ServerError("m", err).getCause() != err) {
            return i;
        }
        i++;
        try {
            MarshalledObject<String> mo = new MarshalledObject<String>("hola");
            // get() deserializa de nuevo cada vez.
            if (!"hola".equals(mo.get()) || mo.get() == mo.get()) {
                return i;
            }
            i++;
            MarshalledObject<String> same = new MarshalledObject<String>("hola");
            if (!mo.equals(same) || mo.hashCode() != same.hashCode()) {
                return i;
            }
            i++;
            if (mo.equals(new MarshalledObject<String>("chau"))) {
                return i;
            }
            i++;
            // El de null arranca en 13 y no avanza.
            MarshalledObject<String> nul = new MarshalledObject<String>(null);
            if (nul.get() != null || nul.hashCode() != 13
                || !nul.equals(new MarshalledObject<String>(null)) || mo.equals(nul)) {
                return i;
            }
            i++;
        } catch (Exception e) {
            return i;
        }

        // ---- java.lang.management: datos -------------------------------------
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);
        if (mu.getInit() != 1 || mu.getUsed() != 2 || mu.getCommitted() != 3 || mu.getMax() != 4) {
            return i;
        }
        i++;
        if (!mu.toString().equals("init = 1(0K) used = 2(0K) committed = 3(0K) max = 4(0K)")) {
            return i;
        }
        i++;
        if (!new MemoryUsage(-1, 2048, 4096, -1).toString()
                .equals("init = -1(-1K) used = 2048(2K) committed = 4096(4K) max = -1(-1K)")) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new MemoryUsage(1, 5, 3, 4);
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new MemoryUsage(1, 2, 5, 4);
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new MemoryUsage(-2, 2, 3, 4);
            }
        })) {
            return i;
        }
        i++;
        if (MemoryUsage.from(null) != null) {
            return i;
        }
        i++;
        // El toString del enum no es el nombre de la constante.
        if (!MemoryType.HEAP.toString().equals("Heap memory")
            || !MemoryType.NON_HEAP.toString().equals("Non-heap memory")
            || !MemoryType.HEAP.name().equals("HEAP")) {
            return i;
        }
        i++;
        LockInfo li = new LockInfo("java.lang.Object", 7);
        if (!li.toString().equals("java.lang.Object@7") || li.getIdentityHashCode() != 7) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new LockInfo(null, 1);
            }
        })) {
            return i;
        }
        i++;
        if (LockInfo.from(null) != null || MonitorInfo.from(null) != null) {
            return i;
        }
        i++;
        StackTraceElement frame = new StackTraceElement("A", "m", "A.java", 5);
        MonitorInfo mi = new MonitorInfo("C", 3, 2, frame);
        if (mi.getLockedStackDepth() != 2 || mi.getLockedStackFrame() != frame
            || !mi.toString().equals("C@3")) {
            return i;
        }
        i++;
        // Profundidad y marco tienen que concordar: -1 va con null y nada mas.
        if (!iae(new Runnable() {
            public void run() {
                new MonitorInfo("C", 3, 2, null);
            }
        })) {
            return i;
        }
        i++;
        MonitorInfo unknown = new MonitorInfo("C", 3, -1, null);
        if (unknown.getLockedStackDepth() != -1 || unknown.getLockedStackFrame() != null) {
            return i;
        }
        i++;
        if (!ManagementFactory.MEMORY_MXBEAN_NAME.equals("java.lang:type=Memory")
            || !ManagementFactory.THREAD_MXBEAN_NAME.equals("java.lang:type=Threading")
            || !ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                    .equals("java.lang:type=GarbageCollector")) {
            return i;
        }
        i++;
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os == null || os.getAvailableProcessors() <= 0 || os.getName() == null) {
            return i;
        }
        i++;
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        if (rt == null || rt.getUptime() < 0 || rt.getStartTime() <= 0
            || rt.getSystemProperties() == null || rt.getInputArguments() == null) {
            return i;
        }
        i++;
        if (rt.isBootClassPathSupported()) {
            return i;
        }
        i++;
        if (ManagementFactory.getPlatformManagementInterfaces().isEmpty()) {
            return i;
        }
        i++;

        // ---- java.awt.desktop -------------------------------------------------
        AppEvent ev = new java.awt.desktop.AboutEvent();
        if (ev.getSource() == null) {
            return i;
        }
        i++;
        List<File> files = new ArrayList<File>();
        files.add(new File("x"));
        OpenFilesEvent ofe = new OpenFilesEvent(files, "buscado");
        if (!"buscado".equals(ofe.getSearchTerm()) || ofe.getFiles().size() != 1) {
            return i;
        }
        i++;
        // getFiles devuelve una copia nueva cada vez, no la lista guardada.
        if (ofe.getFiles() == ofe.getFiles() || ofe.getFiles() == files) {
            return i;
        }
        i++;
        // Pero la copia se hace sobre la lista original, que no se copio al construir.
        files.add(new File("y"));
        if (ofe.getFiles().size() != 2) {
            return i;
        }
        i++;
        // Sin termino de busqueda queda la cadena vacia, no null.
        OpenFilesEvent bare = new OpenFilesEvent(null, null);
        if (bare.getFiles() != null || !"".equals(bare.getSearchTerm())) {
            return i;
        }
        i++;
        if (new PrintFilesEvent(files).getFiles().size() != 2) {
            return i;
        }
        i++;
        if (new OpenURIEvent(null).getURI() != null) {
            return i;
        }
        i++;
        if (UserSessionEvent.Reason.values().length != 4
            || new UserSessionEvent(UserSessionEvent.Reason.LOCK).getReason()
                != UserSessionEvent.Reason.LOCK) {
            return i;
        }
        i++;
        if (QuitStrategy.values().length != 2) {
            return i;
        }
        i++;

        // ---- javax.xml.catalog -------------------------------------------------
        CatalogFeatures d = CatalogFeatures.defaults();
        if (d.get(CatalogFeatures.Feature.FILES) != null
            || !"public".equals(d.get(CatalogFeatures.Feature.PREFER))
            || !"true".equals(d.get(CatalogFeatures.Feature.DEFER))
            || !"strict".equals(d.get(CatalogFeatures.Feature.RESOLVE))) {
            return i;
        }
        i++;
        if (!CatalogFeatures.Feature.FILES.getPropertyName().equals("javax.xml.catalog.files")
            || CatalogFeatures.Feature.FILES.defaultValue() != null
            || !CatalogFeatures.Feature.PREFER.defaultValue().equals("public")) {
            return i;
        }
        i++;
        // Los valores distinguen mayusculas y la lista es cerrada.
        if (!iae(new Runnable() {
            public void run() {
                CatalogFeatures.builder().with(CatalogFeatures.Feature.PREFER, "bogus");
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                CatalogFeatures.builder().with(CatalogFeatures.Feature.RESOLVE, "Strict");
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                CatalogFeatures.builder().with(CatalogFeatures.Feature.FILES, null);
            }
        })) {
            return i;
        }
        i++;
        if (!"system".equals(CatalogFeatures.builder()
                .with(CatalogFeatures.Feature.PREFER, "system").build()
                .get(CatalogFeatures.Feature.PREFER))) {
            return i;
        }
        i++;
        Catalog empty = CatalogManager.catalog(d);
        if (empty.matchSystem("x") != null || empty.matchPublic("x") != null
            || empty.matchURI("x") != null || empty.catalogs().count() != 0) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                CatalogManager.catalog(null);
            }
        })) {
            return i;
        }
        i++;
        // Sin coincidencia, la accion decide; estricto es el de omision.
        CatalogResolver strict = CatalogManager.catalogResolver(empty);
        if (!catalogFails(strict)) {
            return i;
        }
        i++;
        CatalogResolver cont = CatalogManager.catalogResolver(empty,
                                                              CatalogResolver.NotFoundAction.CONTINUE);
        if (cont.resolveEntity("p", "http://x/s.dtd") != null
            || cont.resolveResource("t", "ns", "p", "http://x/s", "http://x/") != null
            || cont.resolveEntity("p", "http://x/s", "http://x/", "ns") != null) {
            return i;
        }
        i++;
        // Salvo resolve(), que devuelve la direccion ya resuelta contra la base.
        javax.xml.transform.Source src = cont.resolve("h.xml", "http://x/");
        if (src == null || !"http://x/h.xml".equals(src.getSystemId())) {
            return i;
        }
        i++;
        CatalogResolver ign = CatalogManager.catalogResolver(empty,
                                                             CatalogResolver.NotFoundAction.IGNORE);
        org.xml.sax.InputSource ise = ign.resolveEntity("p", "http://x/s.dtd");
        if (ise == null || ise.getCharacterStream() == null) {
            return i;
        }
        i++;
        if (!CatalogResolver.NotFoundAction.STRICT.toString().equals("strict")
            || !CatalogResolver.NotFoundAction.CONTINUE.toString().equals("continue")
            || !CatalogResolver.NotFoundAction.IGNORE.toString().equals("ignore")) {
            return i;
        }
        i++;
        if (CatalogResolver.NotFoundAction.getType("strict")
            != CatalogResolver.NotFoundAction.STRICT) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                CatalogResolver.NotFoundAction.getType("nope");
            }
        })) {
            return i;
        }
        i++;

        // ---- javax.sound.sampled ----------------------------------------------
        if (AudioSystem.NOT_SPECIFIED != -1) {
            return i;
        }
        i++;
        AudioFormat f = new AudioFormat(44100f, 16, 2, true, false);
        // El constructor corto deduce codificacion, tamano de cuadro y tasa de cuadro.
        if (!f.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) || f.getFrameSize() != 4
            || f.getFrameRate() != 44100f || f.isBigEndian()) {
            return i;
        }
        i++;
        if (!f.toString().equals(
                "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian")) {
            return i;
        }
        i++;
        // Con 8 bits no se nombra el orden de bytes.
        if (!new AudioFormat(8000f, 8, 1, false, true).toString()
                .equals("PCM_UNSIGNED 8000.0 Hz, 8 bit, mono, 1 bytes/frame")) {
            return i;
        }
        i++;
        AudioFormat wild = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                           AudioSystem.NOT_SPECIFIED, 16, 2, 4,
                                           AudioSystem.NOT_SPECIFIED, false);
        if (!wild.toString().equals(
                "PCM_SIGNED unknown sample rate, 16 bit, stereo, 4 bytes/frame, little-endian")) {
            return i;
        }
        i++;
        // matches no es simetrico: los comodines son los del argumento.
        if (!f.matches(wild) || wild.matches(f)) {
            return i;
        }
        i++;
        // Las codificaciones se comparan por nombre.
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(new AudioFormat.Encoding("PCM_SIGNED"))) {
            return i;
        }
        i++;
        if (!AudioFileFormat.Type.AIFC.toString().equals("AIFF-C")
            || !AudioFileFormat.Type.AIFC.getExtension().equals("aifc")
            || !AudioFileFormat.Type.WAVE.getExtension().equals("wav")) {
            return i;
        }
        i++;
        AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.WAVE, f, 1000);
        if (aff.getByteLength() != AudioSystem.NOT_SPECIFIED
            || !aff.toString().equals("WAVE (.wav) file, data format: "
                + "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian, "
                + "frame length: 1000")) {
            return i;
        }
        i++;
        Line.Info li2 = new Line.Info(SourceDataLine.class);
        // El toString saca el paquete javax.sound.sampled.
        if (!li2.toString().equals("interface SourceDataLine")) {
            return i;
        }
        i++;
        // matches tampoco es simetrico aca: toda linea de salida es una linea.
        if (!new Line.Info(Line.class).matches(li2) || li2.matches(new Line.Info(Line.class))) {
            return i;
        }
        i++;
        DataLine.Info di = new DataLine.Info(SourceDataLine.class, f);
        if (di.getMinBufferSize() != AudioSystem.NOT_SPECIFIED || !di.isFormatSupported(f)
            || !di.toString().equals("interface SourceDataLine supporting format " + f)) {
            return i;
        }
        i++;
        DataLine.Info buf = new DataLine.Info(SourceDataLine.class, new AudioFormat[0], 100, 200);
        if (!buf.toString().equals("interface SourceDataLine, and buffers of 100 to 200 bytes")) {
            return i;
        }
        i++;
        if (!Port.Info.SPEAKER.toString().equals("SPEAKER target port")
            || !Port.Info.MICROPHONE.toString().equals("MICROPHONE source port")
            || Port.Info.SPEAKER.isSource() || !Port.Info.MICROPHONE.isSource()) {
            return i;
        }
        i++;
        // Sin proveedores, las busquedas devuelven vacio y no fallan.
        if (AudioSystem.getMixerInfo() == null || AudioSystem.getAudioFileTypes() == null) {
            return i;
        }
        i++;

        // ---- javax.sound.midi --------------------------------------------------
        try {
            ShortMessage sm = new ShortMessage();
            // El de omision es una nota central a maximo volumen.
            if (sm.getStatus() != 0x90 || sm.getData1() != 64 || sm.getData2() != 127
                || sm.getLength() != 3) {
                return i;
            }
            i++;
            sm.setMessage(ShortMessage.NOTE_ON, 3, 60, 100);
            if (sm.getStatus() != 0x93 || sm.getCommand() != 0x90 || sm.getChannel() != 3
                || sm.getData1() != 60 || sm.getData2() != 100) {
                return i;
            }
            i++;
            ShortMessage clock = new ShortMessage(ShortMessage.TIMING_CLOCK);
            if (clock.getLength() != 1 || clock.getStatus() != 0xF8) {
                return i;
            }
            i++;
            // En un mensaje de sistema el canal no significa nada, pero se calcula igual.
            if (clock.getChannel() != 8 || clock.getCommand() != 0xF0) {
                return i;
            }
            i++;
            MetaMessage mm = new MetaMessage();
            if (mm.getStatus() != 0xFF || mm.getType() != 0 || mm.getData().length != 0
                || mm.getLength() != 2) {
                return i;
            }
            i++;
            MetaMessage m2 = new MetaMessage(1, new byte[] { 104, 105 }, 2);
            // Los bytes son 0xFF, tipo, largo variable, datos.
            if (!Arrays.equals(m2.getMessage(), new byte[] { (byte) 0xFF, 1, 2, 104, 105 })
                || m2.getType() != 1 || m2.getData().length != 2) {
                return i;
            }
            i++;
            // Con 200 bytes el largo ocupa dos: 0x81 0x48.
            MetaMessage big = new MetaMessage(3, new byte[200], 200);
            byte[] raw = big.getMessage();
            if (raw.length != 204 || raw[2] != (byte) 0x81 || raw[3] != 0x48
                || big.getData().length != 200) {
                return i;
            }
            i++;
            SysexMessage sx = new SysexMessage();
            // El de omision es 0xF0 0xF7, y getData excluye solo el estado.
            if (sx.getStatus() != 0xF0 || sx.getData().length != 1
                || sx.getData()[0] != (byte) 0xF7) {
                return i;
            }
            i++;
            sx.setMessage(new byte[] { (byte) 0xF0, 1, 2, (byte) 0xF7 }, 4);
            if (!Arrays.equals(sx.getData(), new byte[] { 1, 2, (byte) 0xF7 })) {
                return i;
            }
            i++;
            Sequence sq = new Sequence(Sequence.PPQ, 480);
            if (sq.getTracks().length != 0 || sq.getTickLength() != 0
                || sq.getPatchList().length != 0) {
                return i;
            }
            i++;
            Track tr = sq.createTrack();
            // Una pista nueva ya trae el fin de pista.
            if (tr.size() != 1 || tr.ticks() != 0
                || !(tr.get(0).getMessage() instanceof MetaMessage)
                || ((MetaMessage) tr.get(0).getMessage()).getType() != 0x2F) {
                return i;
            }
            i++;
            tr.add(new MidiEvent(new ShortMessage(0x90, 60, 100), 200));
            tr.add(new MidiEvent(new ShortMessage(0x90, 62, 100), 50));
            tr.add(new MidiEvent(new ShortMessage(0x90, 64, 100), 100));
            // Se ordena por pulso y el fin de pista queda ultimo.
            if (tr.size() != 4 || tr.get(0).getTick() != 50 || tr.get(1).getTick() != 100
                || tr.get(2).getTick() != 200 || tr.get(3).getMessage().getStatus() != 0xFF
                || tr.ticks() != 200) {
                return i;
            }
            i++;
            MidiEvent dup = new MidiEvent(new ShortMessage(0x90, 60, 100), 200);
            if (!tr.add(dup) || tr.add(dup) || tr.add(null)) {
                return i;
            }
            i++;
            // 100 pulsos a 480 por negra y 120 negras por minuto.
            Sequence sq2 = new Sequence(Sequence.PPQ, 480);
            Track t2 = sq2.createTrack();
            t2.add(new MidiEvent(new ShortMessage(0x90, 60, 100), 100));
            if (sq2.getTickLength() != 100 || sq2.getMicrosecondLength() != 104166) {
                return i;
            }
            i++;
            // En SMPTE el pulso dura siempre lo mismo: 25 por 40 son mil por segundo.
            Sequence sm4 = new Sequence(Sequence.SMPTE_25, 40);
            Track t4 = sm4.createTrack();
            t4.add(new MidiEvent(new ShortMessage(0x90, 60, 100), 1000));
            if (sm4.getMicrosecondLength() != 1000000) {
                return i;
            }
            i++;
            if (new Sequence(Sequence.SMPTE_25, 40, 3).getTracks().length != 3) {
                return i;
            }
            i++;
        } catch (InvalidMidiDataException e) {
            return i;
        }
        if (!invalidMidi(0x70, 0, 0) || !invalidMidi(0x90, 128, 0)) {
            return i;
        }
        i++;
        if (!badSequence(3f)) {
            return i;
        }
        i++;
        if (!Sequencer.SyncMode.INTERNAL_CLOCK.toString().equals("Internal Clock")
            || !Sequencer.SyncMode.NO_SYNC.toString().equals("No Timing")
            || Sequencer.LOOP_CONTINUOUSLY != -1) {
            return i;
        }
        i++;

        // ---- javax.imageio: las dos tablas y el nucleo TIFF --------------------
        if (JPEGQTable.K1Luminance.getTable().length != 64
            || JPEGQTable.K1Luminance.getTable()[0] != 16
            || JPEGQTable.K1Div2Luminance.getTable()[0] != 8
            || JPEGQTable.K2Chrominance.getTable()[0] != 17) {
            return i;
        }
        i++;
        // Escalar recorta a 255 con el techo de linea base.
        int[] scaled = JPEGQTable.K1Luminance.getScaledInstance(2.0f, true).getTable();
        if (scaled[0] != 32 || scaled[5] != 80 || scaled[61] != 200) {
            return i;
        }
        i++;
        if (JPEGQTable.K1Luminance.getTable() == JPEGQTable.K1Luminance.getTable()) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new JPEGQTable(new int[3]);
            }
        })) {
            return i;
        }
        i++;
        if (JPEGHuffmanTable.StdDCLuminance.getLengths().length != 16
            || JPEGHuffmanTable.StdDCLuminance.getValues().length != 12
            || JPEGHuffmanTable.StdACLuminance.getValues().length != 162
            || JPEGHuffmanTable.StdACChrominance.getValues().length != 162) {
            return i;
        }
        i++;
        if (TIFFTag.getSizeOfType(TIFFTag.TIFF_SHORT) != 2
            || TIFFTag.getSizeOfType(TIFFTag.TIFF_RATIONAL) != 8
            || TIFFTag.getSizeOfType(TIFFTag.TIFF_DOUBLE) != 8) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                TIFFTag.getSizeOfType(0);
            }
        })) {
            return i;
        }
        i++;
        TIFFTag tag = new TIFFTag("Foo", 42,
                                  (1 << TIFFTag.TIFF_SHORT) | (1 << TIFFTag.TIFF_LONG), 2);
        if (tag.getDataTypes() != 24 || !tag.isDataTypeOK(TIFFTag.TIFF_SHORT)
            || tag.isDataTypeOK(TIFFTag.TIFF_ASCII) || tag.isIFDPointer()
            || tag.hasValueNames()) {
            return i;
        }
        i++;
        // Sin cantidad declarada queda -1, que significa cualquiera.
        if (new TIFFTag("Bar", 7, 1 << TIFFTag.TIFF_LONG).getCount() != -1) {
            return i;
        }
        i++;
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(tag);
        tags.add(new TIFFTag("Bar", 7, 1 << TIFFTag.TIFF_LONG));
        TIFFTagSet set = new TIFFTagSet(tags);
        if (!set.getTag(42).getName().equals("Foo") || set.getTag("Bar").getNumber() != 7
            || set.getTag(999) != null || set.getTagNumbers().size() != 2) {
            return i;
        }
        i++;
        TIFFTag ptr = new TIFFTag("P", 8, set);
        if (!ptr.isIFDPointer() || ptr.getTagSet() != set || ptr.getCount() != 1) {
            return i;
        }
        i++;
        if (!TIFFTag.UNKNOWN_TAG_NAME.equals("UnknownTag") || TIFFTag.MIN_DATATYPE != 1
            || TIFFTag.MAX_DATATYPE != 13) {
            return i;
        }
        i++;

        // ---- System.arraycopy con nulos ---------------------------------------
        // Es un NullPointerException y no un error de maquina (JLS 11.5). Se agrego aca porque
        // salio a la luz escribiendo las tablas de arriba, con una tabla que habia quedado en null.
        if (!npe(new Runnable() {
            public void run() {
                System.arraycopy(null, 0, new int[4], 0, 4);
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                System.arraycopy(new int[4], 0, null, 0, 4);
            }
        })) {
            return i;
        }
        i++;

        return -1;
    }

    /**
     * Lo que KajiJDK hace y el JDK 25 no.
     *
     * <p>Los tres casos son deliberados y estan documentados en la clase que los produce.
     */
    public static int runKaji() {
        int i = 0;
        // JMXServiceURL: el hash es coherente con equals, que ignora mayusculas en el host. El JDK
        // lo calcula sobre toString(), que las conserva, y por eso alla dos URL iguales pueden tener
        // hash distinto.
        try {
            JMXServiceURL a = new JMXServiceURL("service:jmx:RMI://Host:9");
            JMXServiceURL b = new JMXServiceURL("service:jmx:rmi://host:9");
            if (!a.equals(b) || a.hashCode() != b.hashCode()) {
                return i;
            }
            i++;
        } catch (MalformedURLException e) {
            return i;
        }
        // Los contadores que esta maquina virtual no lleva se declaran ausentes en lugar de
        // devolver ceros.
        if (!uoe(new Runnable() {
            public void run() {
                ManagementFactory.getThreadMXBean().getThreadCount();
            }
        })) {
            return i;
        }
        i++;
        if (!uoe(new Runnable() {
            public void run() {
                ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
            }
        })) {
            return i;
        }
        i++;
        // Pero el monton si se mide, con datos de Runtime.
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (heap == null || heap.getUsed() < 0 || heap.getCommitted() < heap.getUsed()) {
            return i;
        }
        i++;
        // Este es un interprete: no hay compilador que informar, y null es la respuesta correcta.
        if (ManagementFactory.getCompilationMXBean() != null) {
            return i;
        }
        i++;
        return -1;
    }

    /** Si ese host no pasa la validacion de JMXServiceURL. */
    private static boolean badHost(String host) {
        try {
            new JMXServiceURL("rmi", host, 1);
            return false;
        } catch (MalformedURLException e) {
            return true;
        }
    }

    /** Si esa URL entera no se puede analizar. */
    private static boolean badUrl(String url) {
        try {
            new JMXServiceURL(url);
            return false;
        } catch (MalformedURLException e) {
            return true;
        }
    }

    /** Si ese resolutor estricto falla en las cuatro APIs. */
    private static boolean catalogFails(CatalogResolver r) {
        int failures = 0;
        try {
            r.resolveEntity("p", "http://x/s.dtd");
        } catch (CatalogException e) {
            failures++;
        }
        try {
            r.resolve("h.xml", "http://x/");
        } catch (CatalogException e) {
            failures++;
        }
        try {
            r.resolveResource("t", "ns", "p", "http://x/s", "http://x/");
        } catch (CatalogException e) {
            failures++;
        }
        try {
            r.resolveEntity("p", "http://x/s", "http://x/", "ns");
        } catch (CatalogException e) {
            failures++;
        }
        return failures == 4;
    }

    /** Si ese mensaje corto es invalido. */
    private static boolean invalidMidi(int status, int data1, int data2) {
        try {
            new ShortMessage(status, data1, data2);
            return false;
        } catch (InvalidMidiDataException e) {
            return true;
        }
    }

    /** Si esa division no es una de las cinco. */
    private static boolean badSequence(float divisionType) {
        try {
            new Sequence(divisionType, 10);
            return false;
        } catch (InvalidMidiDataException e) {
            return true;
        }
    }

    private static boolean npe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean iae(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean ise(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean uoe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("kaji")) {
            System.out.println(runKaji());
            return;
        }
        System.out.println(run());
    }
}
