//! Pruebas de que **la biblioteca que la VM carga** cumple las reglas del lenguaje.
//!
//! Es una categoría propia y vale separarla. Los tests de `gc.rs` preguntan si el intérprete hace
//! lo que el bytecode dice; éstos preguntan algo distinto: si `KajiLibrary` —que es lo que la VM
//! carga de verdad, porque gana en el bootclasspath— se comporta como la JLS obliga a que se
//! comporte una biblioteca estándar. Un defecto de esta clase no rompe ninguna instrucción: rompe
//! una **garantía**, y por eso pasa desapercibido hasta que alguien la usa.
//!
//! El caso que motivó el módulo (#275) es exactamente esa forma: las cachés de wrapper habían
//! estado bien —vivían en `boot/java/lang/Integer.class`— y se perdieron cuando el `Integer` de
//! KajiLibrary tomó su lugar. Nada lo notó, porque **ninguna prueba comparaba dos boxeos con
//! `==`**. El arreglo sin la prueba se vuelve a perder por el mismo camino.
//!
//! Cada prueba corre un probe de `java/` con `KajiLibrary` **primero** en el bootclasspath, que es
//! el orden real de `run-headless`, y compara contra un número que el JDK real produce con la
//! misma fuente. Ese cotejo es lo que hace al probe un oráculo y no una opinión.

#[cfg(test)]
mod tests {
    use crate::jvm::class_file::ClassFile;
    use crate::jvm::interpreter::bytecode_interpreter::execute;
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;
    use std::path::PathBuf;

    /// Corre `run()I` de un probe de `java/` con **KajiLibrary primero** en el bootclasspath.
    ///
    /// El orden importa y es el del `run-headless`: KajiLibrary es la biblioteca que se desarrolla
    /// y la que tiene que correr, con `boot/` sólo de relleno para lo que todavía vive nada más
    /// que ahí. Un probe de conformidad que se midiera contra `boot/` mediría la biblioteca
    /// equivocada — que es, literalmente, el finding #246.
    fn run_probe(class_file: &str) -> i32 {
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![PathBuf::from("java")],
        );
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `Class.getAnnotation` **materialises** a working annotation object: the VM spins a class
    /// implementing the @interface whose element methods return the values written at the use site,
    /// falling back to the @interface's defaults. The probe annotates itself with a RUNTIME
    /// `@Marker`, reads it back reflectively, and adds up its elements so a single int pins every
    /// path: a use-site `boolean` (1) and `int` (42), a defaulted `String` "x" (100), a use-site
    /// enum constant compared by identity (10000), and a genuinely-absent annotation returning null
    /// (1000) → 11143.
    #[test]
    fn get_annotation_materialises_a_working_annotation() {
        let src = r#"
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            @interface Marker {
                int num() default 7;
                String tag() default "x";
                boolean flag();
                RetentionPolicy pol() default RetentionPolicy.CLASS;
            }

            @Marker(flag = true, num = 42, pol = RetentionPolicy.RUNTIME)
            public class AnnoProbe {
                public int run() {
                    Marker m = AnnoProbe.class.getAnnotation(Marker.class);
                    if (m == null) {
                        return -1;
                    }
                    int acc = 0;
                    if (m.flag()) { acc += 1; }
                    acc += m.num();
                    if (m.tag().equals("x")) { acc += 100; }
                    if (m.pol() == RetentionPolicy.RUNTIME) { acc += 10000; }
                    if (AnnoProbe.class.getAnnotation(Deprecated.class) == null) { acc += 1000; }
                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("AnnoProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 11143, "los valores de los elementos no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `Class.accessFlags()` returns the class's flags as a `Set<AccessFlag>`, and the `AccessFlag`
    /// / nested `Location` enums work end to end: the flag masks, the CLASS-location flag set, and
    /// `ClassFileFormatVersion.latest()` all read back correctly. Also exercises the delicate
    /// co-initialisation of the two enums (an `AccessFlag` constant's location set is built from
    /// `Location` constants at class-init time). The probe adds up five independent checks → 11111.
    #[test]
    fn access_flags_resolve_end_to_end() {
        let src = r#"
            import java.lang.reflect.AccessFlag;
            import java.lang.reflect.ClassFileFormatVersion;
            import java.util.Set;

            public class AfProbe {
                public int run() {
                    Set<AccessFlag> fs = AfProbe.class.accessFlags();
                    int acc = 0;
                    if (fs.contains(AccessFlag.PUBLIC)) { acc += 1; }
                    if (!fs.contains(AccessFlag.PRIVATE)) { acc += 10; }
                    if (AccessFlag.Location.CLASS.flags().contains(AccessFlag.ABSTRACT)) { acc += 100; }
                    if (AccessFlag.PUBLIC.mask() == 1) { acc += 1000; }
                    if (ClassFileFormatVersion.latest().major() == 69) { acc += 10000; }
                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("AfProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 11111, "accessFlags/AccessFlag no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `Thread`'s fuller surface works end to end against the substrate. The probe pins the pieces
    /// that are new beyond the bare scheduler seams: the **timed** `join(long)` (a VM intrinsic that
    /// arms *both* a termination wake and a deadline) in its three shapes — completes (1), times out
    /// leaving the target alive (2), and rejects a negative timeout with `IllegalArgumentException`
    /// (4); `join(Duration)` reporting termination (8); the platform builder's counting name +
    /// daemon flag (16); the virtual builder's `isVirtual()` (32); and the `toString` shape (64) →
    /// 127. The timeout arithmetic is opcode-tick-based under the green scheduler, so the long
    /// sleeper (1_000_000 ticks) is still parked when the 10-tick join returns.
    #[test]
    fn thread_timed_join_builders_and_surface() {
        let src = r#"
            import java.time.Duration;

            public class ThreadProbe2 {
                static volatile int shared = 0;
                static final class Quick implements Runnable {
                    public void run() { shared = 1; }
                }
                static final class Sleeper implements Runnable {
                    public void run() {
                        try { Thread.sleep(1000000L); } catch (InterruptedException e) { }
                    }
                }

                public int run() {
                    int acc = 0;

                    // (1) a timed join that completes before the deadline.
                    shared = 0;
                    Thread q = new Thread(new Quick());
                    q.start();
                    try { q.join(100000L); } catch (InterruptedException e) { }
                    if (shared == 1 && !q.isAlive()) { acc += 1; }

                    // (2) a timed join that lapses, leaving the target alive.
                    Thread s = new Thread(new Sleeper());
                    s.start();
                    try { s.join(10L); } catch (InterruptedException e) { }
                    if (s.isAlive()) { acc += 2; }
                    s.interrupt();
                    try { s.join(); } catch (InterruptedException e) { }

                    // (4) a negative timeout is illegal.
                    Thread n = new Thread(new Quick());
                    try {
                        n.join(-1L);
                    } catch (IllegalArgumentException e) {
                        acc += 4;
                    } catch (InterruptedException e) {
                    }

                    // (8) join(Duration) reports whether the thread finished.
                    shared = 0;
                    Thread d = new Thread(new Quick());
                    d.start();
                    boolean done = false;
                    try { done = d.join(Duration.ofMillis(100000L)); } catch (InterruptedException e) { }
                    if (done && shared == 1) { acc += 8; }

                    // (16) the platform builder: counting name, daemon flag, not virtual.
                    Thread b = Thread.ofPlatform().name("W-", 0L).daemon(true).unstarted(new Quick());
                    if (b.getName().equals("W-0") && b.isDaemon() && !b.isVirtual()) { acc += 16; }

                    // (32) the virtual builder reports virtual.
                    Thread v = Thread.ofVirtual().unstarted(new Quick());
                    if (v.isVirtual()) { acc += 32; }

                    // (64) the toString shape: Thread[#<tid>,<name>,<priority>,<group>].
                    Thread t = new Thread(new Quick());
                    t.setName("z");
                    if (t.toString().indexOf(",z,5,") >= 0) { acc += 64; }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("ThreadProbe2", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 127, "la superficie de Thread no cuadra"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `System`'s fuller surface works end to end. The probe pins the property store as a *mutable*
    /// thing — a seeded key reads back (1), an absent key is null / honours a default (2), `setProperty`
    /// returns the old value and makes the new one visible (4), `clearProperty` removes it (8),
    /// `getProperties` is the live store (16), and `setProperties(null)` restores the platform seed
    /// (32) — plus `lineSeparator` (64), the three standard streams existing with `in` at end-of-stream
    /// (128), `setOut` reassigning the `final` field through its native seam (256), `mapLibraryName`
    /// (512), and an empty `getenv` (1024) → 2047. `gc`/`runFinalization` are called to prove they do
    /// not hang; `System.exit` is deliberately NOT called (it would stop the interpreter).
    #[test]
    fn system_properties_streams_and_surface() {
        let src = r#"
            import java.io.PrintStream;
            import java.util.Properties;
            import java.util.Map;

            public class SystemProbe {
                public int run() {
                    int acc = 0;

                    // (1) a platform-seeded property reads back.
                    if (System.getProperty("os.name") != null) { acc += 1; }
                    // (2) an absent key is null, and honours a supplied default.
                    if (System.getProperty("no.such.key") == null
                            && System.getProperty("no.such.key", "d").equals("d")) { acc += 2; }
                    // (4) setProperty returns the old value (null first) and takes effect.
                    String old = System.setProperty("probe.k", "v1");
                    if (old == null && System.getProperty("probe.k").equals("v1")) { acc += 4; }
                    // (8) clearProperty returns the old value and removes the key.
                    String cleared = System.clearProperty("probe.k");
                    if (cleared.equals("v1") && System.getProperty("probe.k") == null) { acc += 8; }
                    // (16) getProperties is the live store.
                    Properties p = System.getProperties();
                    p.setProperty("live.k", "yes");
                    if (System.getProperty("live.k").equals("yes")) { acc += 16; }
                    // (32) setProperties(null) restores the platform seed.
                    System.setProperties(null);
                    if (System.getProperty("os.name") != null
                            && System.getProperty("live.k") == null) { acc += 32; }
                    // (64) lineSeparator is never null.
                    if (System.lineSeparator() != null) { acc += 64; }
                    // (128) the three standard streams exist; in is at end-of-stream.
                    if (System.out != null && System.err != null
                            && System.in != null && System.in.read() == -1) { acc += 128; }
                    // (256) setOut reassigns the final field via its native seam.
                    PrintStream np = new PrintStream();
                    System.setOut(np);
                    if (System.out == np) { acc += 256; }
                    // (512) mapLibraryName maps the bare name to a platform file name.
                    if (System.mapLibraryName("foo").indexOf("foo") >= 0) { acc += 512; }
                    // (1024) the environment is empty.
                    Map<String, String> env = System.getenv();
                    if (System.getenv("PATH") == null && env.isEmpty()) { acc += 1024; }

                    // These must merely not hang (results not asserted).
                    System.gc();
                    System.runFinalization();

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("SystemProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 2047, "la superficie de System no cuadra"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// The formerly-gated corners of `System`, now backed by real support classes. `console()` is
    /// null (no terminal) (1); the security manager is degraded -- `getSecurityManager` is null (2),
    /// `setSecurityManager` throws (4), and even `new SecurityManager()` throws (8); the Platform
    /// Logging API works -- `getLogger` returns a named logger (16), `isLoggable` gates OFF vs the
    /// rest (32), and `Level` carries the reference severities (64) -- while `getLogger(null)` is an
    /// NPE (128); and `ResourceBundle.getBundle` misses with `MissingResourceException`, KajiJDK
    /// shipping no bundle files (256) → 511.
    #[test]
    fn system_console_security_logger_and_bundles() {
        let src = r#"
            import java.util.ResourceBundle;
            import java.util.MissingResourceException;

            public class SystemGatedProbe {
                public int run() {
                    int acc = 0;

                    // (1) no controlling terminal.
                    if (System.console() == null) { acc += 1; }
                    // (2) the security manager is never installed.
                    if (System.getSecurityManager() == null) { acc += 2; }
                    // (4) installing one is not permitted.
                    try {
                        System.setSecurityManager(null);
                    } catch (UnsupportedOperationException e) {
                        acc += 4;
                    }
                    // (8) and one cannot even be constructed.
                    try {
                        new SecurityManager();
                    } catch (UnsupportedOperationException e) {
                        acc += 8;
                    }
                    // (16) getLogger returns a named logger.
                    System.Logger log = System.getLogger("kaji.probe");
                    if (log != null && log.getName().equals("kaji.probe")) { acc += 16; }
                    // (32) isLoggable gates OFF apart from the rest.
                    if (log.isLoggable(System.Logger.Level.INFO)
                            && !log.isLoggable(System.Logger.Level.OFF)) { acc += 32; }
                    // (64) Level severities and names match the reference.
                    if (System.Logger.Level.INFO.getSeverity() == 800
                            && System.Logger.Level.ERROR.getSeverity() == 1000
                            && System.Logger.Level.WARNING.getName().equals("WARNING")) { acc += 64; }
                    // (128) a null logger name is an NPE.
                    try {
                        System.getLogger(null);
                    } catch (NullPointerException e) {
                        acc += 128;
                    }
                    // (256) no bundle files -> MissingResourceException.
                    try {
                        ResourceBundle.getBundle("no.such.bundle");
                    } catch (MissingResourceException e) {
                        acc += 256;
                    }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("SystemGatedProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 511, "las clases de soporte de System no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// The support classes created to complete `SecurityManager` (30/30) work as libraries in their
    /// own right. `InetAddress` classifies IPv4 addresses from their bytes -- loopback (1), multicast
    /// (2), site-local (4), link-local (8) -- round-trips literals and raw bytes (16, 32), has no
    /// resolver so an unknown host is an `UnknownHostException` (64), and reaches nothing (128).
    /// `FileDescriptor` marks the standard streams valid and a bare one invalid (256). A concrete
    /// `java.security.Permission` subclass carries its name/actions and answers `implies` (512) → 1023.
    #[test]
    fn net_and_security_support_classes() {
        let src = r#"
            import java.net.InetAddress;
            import java.net.UnknownHostException;
            import java.io.FileDescriptor;
            import java.io.IOException;
            import java.security.Permission;

            public class NetSecProbe {
                static final class NamedPermission extends Permission {
                    private final String actions;
                    NamedPermission(String name, String actions) {
                        super(name);
                        this.actions = actions;
                    }
                    public boolean implies(Permission p) {
                        return this.getName().equals(p.getName());
                    }
                    public boolean equals(Object o) {
                        return o instanceof NamedPermission
                                && ((NamedPermission) o).getName().equals(this.getName());
                    }
                    public int hashCode() {
                        return this.getName().hashCode();
                    }
                    public String getActions() {
                        return this.actions;
                    }
                }

                public int run() throws IOException {
                    int acc = 0;

                    if (InetAddress.getByName("127.0.0.1").isLoopbackAddress()) { acc += 1; }
                    if (InetAddress.getByName("224.0.0.5").isMulticastAddress()) { acc += 2; }
                    if (InetAddress.getByName("10.1.2.3").isSiteLocalAddress()
                            && InetAddress.getByName("192.168.0.1").isSiteLocalAddress()) { acc += 4; }
                    if (InetAddress.getByName("169.254.1.1").isLinkLocalAddress()) { acc += 8; }
                    if (InetAddress.getLoopbackAddress().getHostAddress().equals("127.0.0.1")) { acc += 16; }
                    byte[] raw = {8, 8, 8, 8};
                    if (InetAddress.getByAddress(raw).getHostAddress().equals("8.8.8.8")) { acc += 32; }
                    try {
                        InetAddress.getByName("no.such.host.example");
                    } catch (UnknownHostException e) {
                        acc += 64;
                    }
                    if (!InetAddress.getByName("10.0.0.1").isReachable(10)) { acc += 128; }

                    if (FileDescriptor.out.valid() && !new FileDescriptor().valid()) { acc += 256; }

                    NamedPermission p = new NamedPermission("read", "r");
                    if (p.getName().equals("read") && p.getActions().equals("r")
                            && p.implies(new NamedPermission("read", "x"))) { acc += 512; }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("NetSecProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 1023, "las clases de soporte de red/seguridad no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `java.io.File` and `java.nio.file.Path` model the path algebra end to end (KajiJDK has no file
    /// system, so only naming/normalization/resolution are exercised, which is all that is real).
    /// File: name/parent (1), relative vs absolute (2), separator normalization (4), File(parent,
    /// child) (8). Path: element count/name (16), normalize collapsing `.`/`..` (32), resolve with an
    /// absolute override (64), relativize (128), startsWith/endsWith (256), and the File<->Path round
    /// trip via toPath()/toFile() (512) → 1023. Written separator-agnostically (File.separator), so
    /// it holds on any host.
    #[test]
    fn file_and_path_algebra() {
        let src = r#"
            import java.io.File;
            import java.nio.file.Path;

            public class FilePathProbe {
                public int run() {
                    int acc = 0;
                    String S = File.separator;

                    File f = new File(S + "home" + S + "user" + S + "doc.txt");
                    if (f.getName().equals("doc.txt")
                            && f.getParent().equals(S + "home" + S + "user")) { acc += 1; }
                    if (!new File("a/b").isAbsolute() && !Path.of("a/b").isAbsolute()) { acc += 2; }
                    if (new File("x//y/").getPath().equals("x" + S + "y")) { acc += 4; }
                    File j = new File(new File(S + "base"), "sub/file");
                    if (j.getName().equals("file")
                            && j.getParent().equals(S + "base" + S + "sub")) { acc += 8; }

                    Path p = Path.of("a", "b", "c");
                    if (p.getNameCount() == 3 && p.getName(1).toString().equals("b")
                            && p.getFileName().toString().equals("c")) { acc += 16; }
                    if (Path.of("a/./b/../c").normalize().toString().equals("a" + S + "c")) { acc += 32; }
                    Path absolute = Path.of(S + "x");
                    if (Path.of("a/b").resolve("d").toString().equals("a" + S + "b" + S + "d")
                            && Path.of("a").resolve(absolute).equals(absolute)) { acc += 64; }
                    if (Path.of("a/b/c").relativize(Path.of("a/b/d/e")).toString()
                            .equals(".." + S + "d" + S + "e")) { acc += 128; }
                    if (Path.of("a/b/c").startsWith("a/b")
                            && Path.of("a/b/c").endsWith("b/c")) { acc += 256; }

                    File rt = new File("foo/bar.txt");
                    if (rt.toPath().getFileName().toString().equals("bar.txt")
                            && rt.toPath().toFile().getName().equals("bar.txt")) { acc += 512; }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("FilePathProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 1023, "File/Path no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// The `java.nio` buffers expose their backing array through the covariant `array()` (the last
    /// gap that closed the whole buffer family to 100%). The probe checks a `ByteBuffer` hands back
    /// the *same* array object it wrapped (1) and shares it for writes (2), that `IntBuffer` (4) and
    /// `CharBuffer` (8) do the same with their element types, and that `hasArray`/`arrayOffset` agree
    /// (16) → 31. `array()` now returns `byte[]`/`int[]`/`char[]` (not `Object`), with the JDK's
    /// bridge — the frozen javac accepts the covariant array return since the compiler round that
    /// fixed covariant bridges.
    #[test]
    fn nio_buffers_backing_array() {
        let src = r#"
            import java.nio.ByteBuffer;
            import java.nio.IntBuffer;
            import java.nio.CharBuffer;

            public class BufProbe {
                public int run() {
                    int acc = 0;

                    byte[] ba = {10, 20, 30};
                    ByteBuffer bb = ByteBuffer.wrap(ba);
                    if (bb.array() == ba && bb.array().length == 3 && bb.array()[1] == 20) { acc += 1; }
                    bb.put(0, (byte) 99);
                    if (ba[0] == 99) { acc += 2; }

                    int[] ia = {1, 2, 3, 4};
                    IntBuffer ib = IntBuffer.wrap(ia);
                    if (ib.array() == ia && ib.array()[3] == 4) { acc += 4; }

                    char[] ca = {'x', 'y'};
                    CharBuffer cb = CharBuffer.wrap(ca);
                    if (cb.array() == ca && cb.array()[0] == 'x') { acc += 8; }

                    if (bb.hasArray() && bb.arrayOffset() == 0) { acc += 16; }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("BufProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 31, "los buffers de java.nio no cuadran"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// `java.math` closed to 100% (BigInteger 64/64, BigDecimal 90/90). The probe exercises the new
    /// surface behaviourally: BigInteger two's-complement bit ops (1) and `toByteArray` round-trip
    /// (2), `sqrt` floor (4), `modPow`/`modInverse` (8), Miller-Rabin primality (16); and BigDecimal
    /// legacy int rounding (32), `MathContext` divide/multiply (64), exact conversions with the
    /// `ArithmeticException` on a fraction (128), `remainder`/`divideToIntegralValue` (256), and
    /// `sqrt(2)` to 10 digits (512) → 1023.
    #[test]
    fn java_math_bigint_and_bigdecimal() {
        let src = r#"
            import java.math.BigInteger;
            import java.math.BigDecimal;
            import java.math.MathContext;

            public class MathProbe {
                static BigInteger b(long v) { return BigInteger.valueOf(v); }
                static BigDecimal d(String s) { return new BigDecimal(s); }

                public int run() {
                    int acc = 0;

                    if (b(12).and(b(10)).intValue() == 8 && b(12).or(b(10)).intValue() == 14
                            && b(5).not().intValue() == -6 && b(-16).bitCount() == 4) { acc += 1; }
                    if (new BigInteger(new BigInteger("-1180591620717411303424").toByteArray())
                            .equals(new BigInteger("-1180591620717411303424"))) { acc += 2; }
                    if (b(145).sqrt().intValue() == 12
                            && new BigInteger("100000000000000000000").sqrt()
                                    .equals(BigInteger.valueOf(10000000000L))) { acc += 4; }
                    if (b(3).modPow(b(13), b(7)).intValue() == 3
                            && b(3).modInverse(b(11)).intValue() == 4) { acc += 8; }
                    if (b(97).isProbablePrime(20) && !b(91).isProbablePrime(20)
                            && b(90).nextProbablePrime().intValue() == 97) { acc += 16; }

                    if (d("1").divide(d("3"), 5, BigDecimal.ROUND_HALF_UP).toString().equals("0.33333")) {
                        acc += 32;
                    }
                    if (d("1").divide(d("3"), new MathContext(5)).toPlainString().equals("0.33333")
                            && d("2").multiply(d("3.14159"), new MathContext(3)).toString().equals("6.28")) {
                        acc += 64;
                    }
                    if (d("42.00").intValueExact() == 42) {
                        try {
                            d("42.5").intValueExact();
                        } catch (ArithmeticException e) {
                            acc += 128;
                        }
                    }
                    if (d("7").remainder(d("3")).intValue() == 1
                            && d("7").divideToIntegralValue(d("3")).intValue() == 2) { acc += 256; }
                    if (d("2").sqrt(new MathContext(10)).toPlainString().startsWith("1.414213562")) {
                        acc += 512;
                    }

                    return acc;
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[src], &cp).expect("el probe compila");
        let mut metaspace = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (internal, bytes) in classes {
                let cf = ClassFile::from_bytes(bytes).expect("la clase del probe parsea");
                metaspace.add(internal.clone(), cf);
            }
        }
        let entry = metaspace.resolve_method("MathProbe", "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => assert_eq!(v, 1023, "java.math no cuadra"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// Las cuatro clases nuevas de `java.lang` (`Process`/`ProcessHandle` públicas, `CharacterData`/
    /// `BaseVirtualThread` internas) **cargan** en el VM. Son superficie inerte —KajiJDK no tiene
    /// subsistema de procesos ni virtual threads, así que nada las instancia y ningún otro test las
    /// carga—, pero tienen que ser `.class` bien formados. Resolver un método de cada una fuerza su
    /// carga; una clase malformada (o que refiere un tipo ausente) falla acá.
    #[test]
    fn the_new_lang_classes_load() {
        let mut ms = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![],
        );
        assert!(
            ms.resolve_method("java/lang/Process", "isAlive", "()Z").is_some(),
            "Process no carga",
        );
        assert!(
            ms.resolve_method("java/lang/ProcessHandle", "of", "(J)Ljava/util/Optional;").is_some(),
            "ProcessHandle no carga",
        );
        assert!(
            ms.resolve_method("java/lang/CharacterData", "of", "(I)Ljava/lang/CharacterData;")
                .is_some(),
            "CharacterData no carga",
        );
        assert!(
            ms.resolve_method("java/lang/BaseVirtualThread", "park", "()V").is_some(),
            "BaseVirtualThread no carga",
        );
        // StackWalker (pública) + su StackFrame/Option anidados, LiveStackFrame (interna) y los stubs
        // `jdk.internal.vm` que sus firmas nombran.
        assert!(
            ms.resolve_method("java/lang/StackWalker", "getInstance", "()Ljava/lang/StackWalker;")
                .is_some(),
            "StackWalker no carga",
        );
        assert!(
            ms.resolve_method("java/lang/LiveStackFrame", "getMonitors", "()[Ljava/lang/Object;")
                .is_some(),
            "LiveStackFrame no carga",
        );
        assert!(
            ms.resolve_method("jdk/internal/vm/ContinuationScope", "getName", "()Ljava/lang/String;")
                .is_some(),
            "ContinuationScope no carga",
        );
    }

    /// #275 — JLS §5.1.7: boxear un valor entre −128 y 127 devuelve la **misma** referencia.
    ///
    /// Dieciocho propiedades, un bit cada una, y las que piden `!=` valen tanto como las que piden
    /// `==`: una caché que cubriera *todos* los valores haría que el código que compara con `==`
    /// pareciera andar acá y se rompiera contra un JDK real. Se comprueban los dos caminos (por
    /// `valueOf` y por autoboxing), los cuatro bordes exactos (−128, 127, −129, 128) y los seis
    /// wrappers, incluido `Byte` —cuyo rango entero entra en la caché, así que `valueOf` nunca
    /// aloca— y `Boolean`, cuyas dos instancias son constantes.
    ///
    /// **262143 es el número que imprime el JDK 21 corriendo la misma fuente.** Eso es lo que hace
    /// del probe un oráculo: no se eligió el valor que da hoy, se eligió el que tiene que dar.
    /// **`#[ignore]` a propósito, y por una razón que hay que sacar cuando corresponda:** el
    /// arreglo de las cachés vive **sin commitear** en el árbol de trabajo (`Integer.java`,
    /// `Long.java`, `Short.java`, `Byte.java`, `Character.java`), así que sobre `HEAD` este test
    /// falla — se comprobó, y falla, que es justamente lo que lo hace una guarda de verdad y no
    /// una que pasa por casualidad. Con el arreglo aplicado da 262143.
    ///
    /// Se deja escrito acá en vez de esperar, porque el finding que lo motiva es *"se arregló una
    /// vez y se perdió porque nadie lo probaba"*: la prueba que llega después del arreglo llega
    /// tarde por definición. **Sacar el `#[ignore]` cuando los cinco wrappers estén commiteados.**
    #[ignore = "el arreglo de #275 todavia no esta commiteado; ver el comentario"]
    #[test]
    fn the_wrapper_caches_the_language_requires_are_there() {
        assert_eq!(run_probe("java/WrapCacheProbe.class"), 262143);
    }

    /// The fdlibm transcendentals ported into `Math` must be **bit-for-bit** the JDK's fdlibm — that
    /// is the whole point of ULP-contract functions. Each probe folds the raw bits of the function
    /// over a fixed input set into one int; the expected value is what the reference JDK prints for
    /// the identical fold using `StrictMath` (which IS fdlibm). The comparison is against
    /// `StrictMath`, not `Math`: the JDK's `Math` may substitute a hardware intrinsic that differs
    /// from fdlibm by up to 1 ulp (e.g. `Math.cbrt(2.5)`), whereas `StrictMath` is the fixed
    /// reference our port targets. A single wrong bit anywhere changes the fold.
    fn math_fold(body: &str, expected: i32) {
        let src = format!(
            r#"
            public class MathProbe {{
                static double[] xs() {{
                    return new double[]{{8.0,27.0,64.0,-8.0,0.001,2.0,123.456,1.0e300,
                        1.0e-300,0.5,1.0000001,-123.456,3.14159265358979,1.0e-310,2.5,1000000.0}};
                }}
                public int run() {{
                    long acc = 1;
                    for (double x : xs()) {{ acc = acc * 1000003L + Double.doubleToRawLongBits({body}); }}
                    return (int)(acc ^ (acc >>> 32));
                }}
            }}
        "#
        );
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per_unit = crate::javac::compile_units_cp(&[&src], &cp).expect("el probe compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for classes in &per_unit {
            for (n, b) in classes {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("MathProbe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, expected, "fdlibm no es bit-exacto vs el JDK"),
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    #[test]
    fn math_cbrt_is_bit_exact() {
        // Fold of StrictMath.cbrt over the fixed inputs, from the reference JDK. (Our Math.cbrt is
        // fdlibm, so it equals StrictMath.cbrt; the JDK's own Math.cbrt intrinsic folds to a
        // different value, 1051988593, differing at x=2.5 — which is why we target StrictMath.)
        math_fold("Math.cbrt(x)", 1051055158);
    }

    #[test]
    fn math_exp_is_bit_exact() {
        math_fold("Math.exp(x)", -337283989);
    }

    #[test]
    fn math_log_is_bit_exact() {
        math_fold("Math.log(x)", 2131159354);
    }

    // Trigonometry exercises the shared argument-reduction library (FdLibm.kernelRemPioTwo) on the
    // large inputs (1e300, 1e6, 123.456...), the exact place a naive reduction loses all its bits.
    #[test]
    fn math_sin_is_bit_exact() {
        math_fold("Math.sin(x)", -974671370);
    }

    #[test]
    fn math_cos_is_bit_exact() {
        math_fold("Math.cos(x)", -1562459674);
    }

    #[test]
    fn math_tan_is_bit_exact() {
        math_fold("Math.tan(x)", 506040772);
    }

    #[test]
    fn math_expm1_is_bit_exact() {
        math_fold("Math.expm1(x)", -407143630);
    }

    #[test]
    fn math_log10_is_bit_exact() {
        math_fold("Math.log10(x)", 1709501230);
    }

    #[test]
    fn math_log1p_is_bit_exact() {
        math_fold("Math.log1p(x)", 911684525);
    }

    /// Reflective field access via `java.lang.reflect.Field`: typed get/set on instance and static
    /// fields of several kinds (int, long, double, boolean, reference), plus the boxing `get`/`set`.
    /// Each check adds a distinct bit → 511 when the whole read/write path works.
    #[test]
    fn reflective_field_get_and_set() {
        let src = r#"
            import java.lang.reflect.Field;
            public class FieldProbe {
                int i = 42;
                static long s = 100L;
                double d = 3.5;
                String str = "hi";
                boolean b = true;
                public int run() {
                    try {
                        Field[] fs = FieldProbe.class.getDeclaredFields();
                        FieldProbe p = new FieldProbe();
                        int acc = 0;
                        int k = 0;
                        while (k < fs.length) {
                            Field f = fs[k];
                            String n = f.getName();
                            if (n.equals("i")) {
                                if (f.getInt(p) == 42) { acc += 1; }
                                f.setInt(p, 99);
                                if (p.i == 99) { acc += 2; }
                                if (((Integer) f.get(p)).intValue() == 99) { acc += 4; }
                            } else if (n.equals("s")) {
                                if (f.getLong(null) == 100L) { acc += 8; }
                                f.setLong(null, 7L);
                                if (FieldProbe.s == 7L) { acc += 16; }
                            } else if (n.equals("d")) {
                                if (f.getDouble(p) == 3.5) { acc += 32; }
                            } else if (n.equals("str")) {
                                if (f.get(p).equals("hi")) { acc += 64; }
                                f.set(p, "bye");
                                if (p.str.equals("bye")) { acc += 128; }
                            } else if (n.equals("b")) {
                                if (f.getBoolean(p)) { acc += 256; }
                            }
                            k = k + 1;
                        }
                        return acc;
                    } catch (Throwable t) {
                        return -1;
                    }
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("FieldProbe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, 511, "acceso reflexivo a Field falla"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    /// Reflective invocation via `Method.invoke` (the `Intrinsic::MethodInvoke` interception): an
    /// instance method with primitive args (unboxed on the way in, the int result boxed on the way
    /// out) and a static method with a reference arg. Each adds a bit → 3.
    #[test]
    fn reflective_method_invoke() {
        let src = r#"
            import java.lang.reflect.Method;
            public class MethodProbe {
                public int add(int a, int b) { return a + b; }
                public static String greet(String n) { return "hi " + n; }
                public int run() {
                    try {
                        Method[] ms = MethodProbe.class.getDeclaredMethods();
                        MethodProbe p = new MethodProbe();
                        int acc = 0;
                        int k = 0;
                        while (k < ms.length) {
                            Method m = ms[k];
                            if (m.getName().equals("add")) {
                                Object r = m.invoke(p, Integer.valueOf(3), Integer.valueOf(4));
                                if (((Integer) r).intValue() == 7) { acc += 1; }
                            } else if (m.getName().equals("greet")) {
                                Object r = m.invoke(null, "bob");
                                if (r.equals("hi bob")) { acc += 2; }
                            }
                            k = k + 1;
                        }
                        return acc;
                    } catch (Throwable t) {
                        return -1;
                    }
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("MethodProbe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, 3, "Method.invoke falla"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    /// Reflective construction via `Constructor.newInstance` (Intrinsic::ConstructorNewInstance),
    /// still intercepted after being declared as an ordinary (non-native) method.
    #[test]
    fn reflective_constructor_new_instance() {
        let src = r#"
            import java.lang.reflect.Constructor;
            public class CtorProbe {
                int x;
                public CtorProbe() { this.x = 5; }
                public int run() {
                    try {
                        Constructor<?>[] cs = CtorProbe.class.getDeclaredConstructors();
                        Object o = cs[0].newInstance();
                        return ((CtorProbe) o).x;
                    } catch (Throwable t) {
                        return -1;
                    }
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("CtorProbe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, 5, "Constructor.newInstance falla"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    /// `Runtime.Version` (JEP 223 parsing) and its use by `ClassFileFormatVersion`: parse a full
    /// version string, read its parts, and round-trip a format through runtimeVersion()/valueOf().
    /// Each check adds a bit → 127.
    #[test]
    fn runtime_version_and_class_file_format() {
        let src = r#"
            import java.lang.reflect.ClassFileFormatVersion;
            public class VerProbe {
                public int run() {
                    try {
                        Runtime.Version v = Runtime.Version.parse("25.0.3+9");
                        int acc = 0;
                        if (v.feature() == 25) { acc += 1; }
                        if (v.update() == 3) { acc += 2; }
                        if (v.build().get().intValue() == 9) { acc += 4; }
                        if (v.interim() == 0) { acc += 8; }
                        ClassFileFormatVersion c = ClassFileFormatVersion.RELEASE_17;
                        Runtime.Version rv = c.runtimeVersion();
                        if (rv.feature() == 17) { acc += 16; }
                        if (ClassFileFormatVersion.valueOf(rv) == c) { acc += 32; }
                        Runtime.Version a = Runtime.Version.parse("21");
                        Runtime.Version b = Runtime.Version.parse("25");
                        if (a.compareTo(b) < 0) { acc += 64; }
                        return acc;
                    } catch (Throwable t) {
                        return -1;
                    }
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("VerProbe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, 127, "Runtime.Version/CFFV falla"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    #[test]
    fn math_sinh_is_bit_exact() {
        math_fold("Math.sinh(x)", 261112681);
    }

    #[test]
    fn math_cosh_is_bit_exact() {
        math_fold("Math.cosh(x)", -380917814);
    }

    #[test]
    fn math_tanh_is_bit_exact() {
        math_fold("Math.tanh(x)", 2025612183);
    }

    #[test]
    fn math_atan_is_bit_exact() {
        math_fold("Math.atan(x)", -1410318840);
    }

    #[test]
    fn math_asin_is_bit_exact() {
        math_fold("Math.asin(x)", -1966473352);
    }

    #[test]
    fn math_acos_is_bit_exact() {
        math_fold("Math.acos(x)", -1637032315);
    }

    /// atan2 takes two arguments, so it needs its own fold over pairs (y=xs[i], x=xs[i+1]).
    #[test]
    fn math_atan2_is_bit_exact() {
        let src = r#"
            public class At2Probe {
                static double[] xs() {
                    return new double[]{8.0,27.0,64.0,-8.0,0.001,2.0,123.456,1.0e300,
                        1.0e-300,0.5,1.0000001,-123.456,3.14159265358979,1.0e-310,2.5,1000000.0};
                }
                public int run() {
                    double[] xs = xs();
                    long acc = 1;
                    int i = 0;
                    while (i < xs.length) {
                        acc = acc * 1000003L + Double.doubleToRawLongBits(Math.atan2(xs[i], xs[(i + 1) % xs.length]));
                        i = i + 1;
                    }
                    return (int)(acc ^ (acc >>> 32));
                }
            }
        "#;
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("At2Probe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, 535552999, "atan2 no es bit-exacto"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    /// Two-argument fold for hypot and pow over pairs (a=xs[i], b=xs[i+1]).
    fn math_fold2(call: &str, expected: i32) {
        let src = format!(
            r#"
            public class M2Probe {{
                static double[] xs() {{
                    return new double[]{{8.0,27.0,64.0,-8.0,0.001,2.0,123.456,1.0e300,
                        1.0e-300,0.5,1.0000001,-123.456,3.14159265358979,1.0e-310,2.5,1000000.0}};
                }}
                public int run() {{
                    double[] xs = xs();
                    long acc = 1;
                    int i = 0;
                    while (i < xs.length) {{
                        double a = xs[i];
                        double b = xs[(i + 1) % xs.length];
                        acc = acc * 1000003L + Double.doubleToRawLongBits({call});
                        i = i + 1;
                    }}
                    return (int)(acc ^ (acc >>> 32));
                }}
            }}
        "#
        );
        let cp = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let per = crate::javac::compile_units_cp(&[&src], &cp).expect("compila");
        let mut ms = MetaspaceService::new(cp, Vec::new());
        for cs in &per {
            for (n, b) in cs {
                ms.add(n.clone(), ClassFile::from_bytes(b).unwrap());
            }
        }
        let e = ms.resolve_method("M2Probe", "run", "()I").expect("run()");
        let ml = ms.max_locals(e);
        let f = Frame::new(e, ml, Vec::new());
        match execute(ms, f) {
            Some(Value::Int(v)) => assert_eq!(v, expected, "no es bit-exacto"),
            other => panic!("se esperaba int, salió {other:?}"),
        }
    }

    #[test]
    fn math_hypot_is_bit_exact() {
        math_fold2("Math.hypot(a, b)", -7530247);
    }

    #[test]
    fn math_pow_is_bit_exact() {
        math_fold2("Math.pow(a, b)", 493142875);
    }
}
