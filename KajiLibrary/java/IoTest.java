import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Serializable;

/**
 * Behaviour test of java.io, written to run **the same** in this VM and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed: a single int is enough to compare the two VMs without depending on the
 * console output matching character by character.
 *
 * <p>Everything that touches the disk goes to the temporary directory of the system and is deleted
 * on exit. No case writes inside the project: a test that dirties the tree becomes impossible to
 * repeat.
 *
 * <p>The names of the serialisation fixtures --{@code Node}, {@code Parent}, {@code Child} and
 * company-- are left as they are on purpose. They are written inside the streams of {@code
 * EXPECTED} and they feed the expected {@code serialVersionUID}s, both of which came from running
 * this same file on the real JDK: renaming a class here would change the bytes and the numbers, and
 * the test would stop comparing anything until they were regenerated there.
 */
public class IoTest {

    private static File tmpDir() {
        String t = System.getProperty("java.io.tmpdir");
        return new File(t == null ? "." : t);
    }

    public static int run() {
        int i = 0;
        File created = null;
        try {
            // --- File.createTempFile ---
            created = File.createTempFile("kaji", ".txt");
            if (!created.exists()) return i; i++;                       // 0
            if (!created.isFile()) return i; i++;                       // 1
            if (created.length() != 0L) return i; i++;                  // 2
            if (!created.getName().startsWith("kaji")) return i; i++;   // 3
            if (!created.getName().endsWith(".txt")) return i; i++;     // 4
            // Two calls in a row cannot give the same file.
            File other = File.createTempFile("kaji", ".txt");
            if (created.equals(other)) return i; i++;                    // 5
            other.delete();

            // The null suffix is `.tmp`, and a prefix of fewer than three letters is illegal.
            File noSuffix = File.createTempFile("kaji", null);
            if (!noSuffix.getName().endsWith(".tmp")) return i; i++;  // 6
            noSuffix.delete();
            try {
                File.createTempFile("ab", ".txt");
                return i;                                             // 7
            } catch (IllegalArgumentException expected) {
                i++;
            }

            // With an explicit directory: the parent has to be the one asked for.
            File enDir = File.createTempFile("kaji", ".txt", tmpDir());
            if (!enDir.exists()) return i; i++;                        // 8
            enDir.delete();

            // --- lastModified / setLastModified --- A multiple of 1000: there are file systems
            // that truncate to seconds, and the test is not here to measure the granularity of the
            // disk.
            long cuando = 1234567000L;
            if (!created.setLastModified(cuando)) return i; i++;        // 9
            if (created.lastModified() != cuando) return i; i++;        // 10

            // A file that does not exist has no date: zero, which is what the contract says. And
            // the zero does **not** mean "1 January 1970": that is what the case below is for.
            File ausente = new File(tmpDir(), "kaji-never-ever-exists");
            ausente.delete();
            if (ausente.lastModified() != 0L) return i; i++;           // 11
            if (ausente.setLastModified(cuando)) return i; i++;        // 12

            // The epoch is a valid date and is told apart from "not known" because the file exists.
            if (!created.setLastModified(0L)) return i; i++;            // 13
            if (created.lastModified() != 0L) return i; i++;            // 14
            if (!created.exists()) return i; i++;                       // 15
            created.setLastModified(cuando);

            // A negative date is illegal, not a `false`.
            try {
                created.setLastModified(-1L);
                return i;                                             // 16
            } catch (IllegalArgumentException expected) {
                i++;
            }

            // --- getCanonicalPath ---
            String canon = created.getCanonicalPath();
            // No "verbatim" paths of Windows: the JDK returns `C:\...`, not `\\?\C:\...`.
            if (canon.startsWith("\\\\?\\")) return i; i++;            // 17
            if (!new File(canon).exists()) return i; i++;              // 18
            if (!new File(canon).isAbsolute()) return i; i++;          // 19
            // Canonicalising twice gives the same: it is a fixed point.
            if (!new File(canon).getCanonicalPath().equals(canon)) return i; i++;  // 20

            // The `.` and `..` are resolved.
            File withDot = new File(created.getParent() + File.separator + "."
                    + File.separator + created.getName());
            if (!withDot.getCanonicalPath().equals(canon)) return i; i++;         // 21

            // A file that does not exist still has a canonical path: it is an operation over the
            // name, not over the contents.
            String canonAusente = ausente.getCanonicalPath();
            if (!new File(canonAusente).isAbsolute()) return i; i++;   // 22
            if (canonAusente.startsWith("\\\\?\\")) return i; i++;     // 23
            if (!canonAusente.endsWith(ausente.getName())) return i; i++;          // 24

            // A relative path is canonicalised against the working directory.
            String canonRel = new File("IoTest-relativo-inexistente").getCanonicalPath();
            if (!new File(canonRel).isAbsolute()) return i; i++;       // 25

            // --- getCanonicalFile matches getCanonicalPath ---
            if (!created.getCanonicalFile().getPath().equals(canon)) return i; i++; // 26

            // Each following block numbers from a hundred of its own, so that adding a case in one
            // does not shift the indices of the others and a difference between the two VMs goes on
            // pointing at the same case from one run to the next.
            int r = streams();
            if (r >= 0) return r;
            r = tuberias();
            if (r >= 0) return r;
            r = uids();
            if (r >= 0) return r;
            r = serializing();
            if (r >= 0) return r;
            r = deserializing();
            if (r >= 0) return r;
            r = canales();
            if (r >= 0) return r;

            return -1;
        } catch (IOException e) {
            return i;
        } finally {
            if (created != null) {
                created.delete();
            }
        }
    }

    /**
     * File streams: what happens **after** closing.
     *
     * <p>All of this measures a single thing: that the error come out as a checked {@link
     * IOException} and not as a `RuntimeException`. It is the difference between a `catch
     * (IOException e)` of the caller catching the error and letting it go past until it kills the
     * thread.
     */
    private static int streams() {
        int i = 100;
        File f = null;
        try {
            f = File.createTempFile("kaji", ".bytes");

            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            out.write(65);
            out.close();
            // Writing over a closed stream is an IOException, not a RuntimeException.
            try {
                out.write(66);
                return i;                                              // 100
            } catch (IOException expected) {
                i++;
            }

            java.io.FileInputStream in = new java.io.FileInputStream(f);
            if (in.read() != 65) return i; i++;                        // 101
            if (in.read() != -1) return i; i++;                        // 102
            in.close();
            try {
                in.read();
                return i;                                             // 103
            } catch (IOException expected) {
                i++;
            }

            // `reset()` with no valid mark is also an IOException.
            java.io.FileInputStream in2 = new java.io.FileInputStream(f);
            try {
                in2.reset();
                return i;                                             // 104
            } catch (IOException expected) {
                i++;
            } finally {
                in2.close();
            }

            // And `available()` over a closed stream, likewise.
            java.io.FileInputStream in3 = new java.io.FileInputStream(f);
            in3.close();
            try {
                in3.available();
                return i;                                             // 105
            } catch (IOException expected) {
                i++;
            }

            return -1;
        } catch (IOException e) {
            return i;
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }

    /**
     * Pipes: reading from one whose writer died.
     *
     * <p>The same point as above. The JDK says "Write end dead" with an {@link IOException}; if it
     * came out wrapped in an unchecked one, the reading loop of anybody who uses a pipe falls over
     * instead of finishing.
     */
    private static int tuberias() {
        int i = 200;
        try {
            java.io.PipedOutputStream po = new java.io.PipedOutputStream();
            java.io.PipedInputStream pi = new java.io.PipedInputStream(po);
            po.write(7);
            if (pi.read() != 7) return i; i++;                        // 200
            pi.close();
            // Writing into a pipe with the reader closed: IOException.
            try {
                po.write(8);
                return i;                                             // 201
            } catch (IOException expected) {
                i++;
            }

            java.io.PipedWriter pw = new java.io.PipedWriter();
            java.io.PipedReader pr = new java.io.PipedReader(pw);
            pw.write('x');
            if (pr.read() != 'x') return i; i++;                      // 202
            pr.close();
            try {
                pw.write('y');
                return i;                                             // 203
            } catch (IOException expected) {
                i++;
            }

            // Connecting twice is an IOException, not an unchecked one.
            java.io.PipedOutputStream po2 = new java.io.PipedOutputStream();
            java.io.PipedInputStream pi2 = new java.io.PipedInputStream(po2);
            try {
                po2.connect(new java.io.PipedInputStream());
                return i;                                             // 204
            } catch (IOException expected) {
                i++;
            }
            pi2.close();
            po2.close();

            return -1;
        } catch (IOException e) {
            return i;
        }
    }

    // ---- serialVersionUID (300) ------------------------------------------------------------------
    //
    // The expected numbers come from the **real JDK** and not from this implementation: the UID has
    // no "reasonable" value that can be reasoned out, its only definition is "what the other side
    // computes". Five different shapes, chosen for what each one puts into the fingerprint: with
    // nothing special, with the UID declared by hand, with a static initialiser --the datum
    // reflection does not give--, an interface, and one with private members that do **not** go in.

    /** The minimum: two fields, with no `<clinit>` and no declared UID. */
    static class Simple implements Serializable {
        int x;
        String s;
    }

    /** With the UID by hand: the computation does not run and this number comes out as it is. */
    static class WithSuid implements Serializable {
        private static final long serialVersionUID = 4242424242L;
        int x;
        String s;
        public int mas(int n) { return this.x + n; }
    }

    /**
     * With a static initialiser. It is the case that separates an honest implementation from one
     * that guesses: the static array generates a `<clinit>`, which goes into the fingerprint and
     * which `getDeclaredMethods` does not show.
     */
    static class WithClinit implements Serializable {
        static final int[] TABLA = new int[3];
        int x;
    }

    /**
     * An interface: modifiers of its own and the ABSTRACT that depends on whether it has methods.
     */
    interface Marker extends Serializable {
        int howMany();
    }

    /**
     * With private members. A `private static` and a `private transient` are left out of the
     * fingerprint; an ordinary instance `private` goes in. The `private` methods are left out.
     */
    static class WithPrivates implements Serializable {
        private static int counter;
        private transient Object cache;
        private int hidden;
        public String visible;
        protected long guarded;
        private void internal() { this.hidden = 1; }
        public int read() { this.internal(); return this.hidden; }
    }

    private static int uids() {
        int i = 300;
        if (ObjectStreamClass.lookup(Simple.class).getSerialVersionUID()
                != 1989406208694753721L) return i; i++;              // 300
        if (ObjectStreamClass.lookup(WithSuid.class).getSerialVersionUID()
                != 4242424242L) return i; i++;                        // 301
        if (ObjectStreamClass.lookup(WithClinit.class).getSerialVersionUID()
                != -640774396655770753L) return i; i++;               // 302
        if (ObjectStreamClass.lookup(Marker.class).getSerialVersionUID()
                != 5974040141890578767L) return i; i++;               // 303
        if (ObjectStreamClass.lookup(WithPrivates.class).getSerialVersionUID()
                != -6658153871694079827L) return i; i++;              // 304

        // The same descriptor asked for twice gives the same number: the cache cannot change the
        // answer, and a UID that moves between calls is worse than a wrong one.
        ObjectStreamClass d = ObjectStreamClass.lookup(Simple.class);
        if (d.getSerialVersionUID() != d.getSerialVersionUID()) return i; i++;  // 305

        // An array **does** have a computed UID, and not zero: it is serialisable, and its
        // fingerprint comes from the name `[I` plus `Cloneable` and `Serializable`. It surprises,
        // and that is why the case is here.
        if (ObjectStreamClass.lookup(int[].class).getSerialVersionUID()
                != 5600894804908749477L) return i; i++;               // 306

        // The `toString` of the JDK is the line of the declaration, not only the name. Over a class
        // of our own and not over `String`: in this library `String` does not implement
        // `Serializable` yet, so `lookup` gives `null` -- which is right for what String is here,
        // and not what the test wants to measure.
        if (!ObjectStreamClass.lookup(WithSuid.class).toString().equals(
                "IoTest$WithSuid: static final long serialVersionUID = 4242424242L;"))
            return i; i++;                                            // 307

        return -1;
    }


    // ---- serialisation, byte by byte (400) ------------------------------------------------------
    //
    // The expected strings are the **stream the real JDK produces** for the same object, in
    // hexadecimal. It is the only test that serves for an interchange format: "it can be read back"
    // is met by any invented format, and what is needed is for the other JVM to read it.

    static class Point implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        int y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    /** With a shared reference and with a cycle: it tests the table of handles. */
    static class Node implements Serializable {
        private static final long serialVersionUID = 2L;
        String name;
        Node other;
        Node(String n) { this.name = n; }
    }

    /** With every primitive, for the order and the packing of the fields. */
    static class AllTypes implements Serializable {
        private static final long serialVersionUID = 3L;
        boolean z = true;
        byte b = -2;
        char c = 'Z';
        short sh = -300;
        int i = 70000;
        long j = -5000000000L;
        float f = 1.5F;
        double d = -2.25D;
        String s = "eñe";
        transient int noSale = 99;
    }

    /** With a `writeObject` of its own: block mode and TC_ENDBLOCKDATA. */
    static class WithWriter implements Serializable {
        private static final long serialVersionUID = 4L;
        int n = 5;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(this.n * 2);
            out.writeUTF("extra");
        }
    }

    /** With `putFields`: the same bytes as the default writing, chosen by hand. */
    static class WithPut implements Serializable {
        private static final long serialVersionUID = 7L;
        int a;
        String b;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            java.io.ObjectOutputStream.PutField pf = out.putFields();
            pf.put("a", 42);
            pf.put("b", "puesto");
            out.writeFields();
        }
    }

    static class Parent implements Serializable {
        private static final long serialVersionUID = 5L;
        int arriba = 11;
    }

    /** A subclass: two chained descriptors and the data from the top downwards. */
    static class Child extends Parent implements Serializable {
        private static final long serialVersionUID = 6L;
        int abajo = 22;
    }

    static class External implements java.io.Externalizable {
        private static final long serialVersionUID = 8L;
        int v = 3;
        public External() { }
        public void writeExternal(java.io.ObjectOutput out) throws IOException {
            out.writeInt(this.v);
            out.writeUTF("ext");
        }
        public void readExternal(java.io.ObjectInput in) throws IOException {
            this.v = in.readInt();
            in.readUTF();
        }
    }

    enum Color implements Serializable { ROJO, VERDE }

    private static final char[] HEXA = "0123456789abcdef".toCharArray();

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEXA[(b[i] >> 4) & 0xF]).append(HEXA[b[i] & 0xF]);
        }
        return sb.toString();
    }

    /** It serialises `o` and returns the whole stream in hexadecimal. */
    private static String ser(Object o) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        return hex(bos.toByteArray());
    }

    /** The case of the shared reference: the same `Node` twice in the same stream. */
    private static String sharedRefs() throws IOException {
        Node n = new Node("uno");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(n);
        oos.writeObject(n);
        oos.flush();
        return hex(bos.toByteArray());
    }

    private static String[] cases() throws IOException {
        Node cycle = new Node("a");
        cycle.other = cycle;
        return new String[] {
            ser(null),
            ser("hola"),
            ser(new Point(3, -4)),
            ser(new int[] { 1, 2, 3 }),
            ser(new String[] { "a", "b" }),
            ser(new AllTypes()),
            ser(new WithWriter()),
            ser(new Child()),
            ser(new External()),
            ser(Color.VERDE),
            ser(cycle),
            sharedRefs(),
            ser(new WithPut()),
            ser(new byte[] { 10, -1 }),
            ser(new double[] { 0.5D }),
            ser(Point.class),
        };
    }

    private static final String[] EXPECTED = {
        "aced000570",
        "aced0005740004686f6c61",
        "aced00057372000c496f5465737424506f696e7400000000000000010200024900017849000179787000000003fffffffc",
        "aced0005757200025b494dba602676eab2a5020000787000000003000000010000000200000003",
        "aced0005757200135b4c6a6176612e6c616e672e537472696e673badd256e7e91d7b470200007870000000027400016174000162",
        "aced00057372000f496f5465737424416c6c5479706573000000000000000302000942000162430001634400016446000166490001694a00016a53000273685a00017a4c0001737400124c6a6176612f6c616e672f537472696e673b7870fe005ac0020000000000003fc0000000011170fffffffed5fa0e00fed40174000465c3b165",
        "aced000573720011496f54657374245769746857726974657200000000000000040300014900016e787000000005770b0000000a0005657874726178",
        "aced00057372000c496f54657374244368696c6400000000000000060200014900056162616a6f7872000d496f5465737424506172656e74000000000000000502000149000661727269626178700000000b00000016",
        "aced00057372000f496f546573742445787465726e616c00000000000000080c00007870770900000003000365787478",
        "aced00057e72000c496f5465737424436f6c6f7200000000000000001200007872000e6a6176612e6c616e672e456e756d000000000000000012000078707400055645524445",
        "aced00057372000b496f54657374244e6f646500000000000000020200024c00046e616d657400124c6a6176612f6c616e672f537472696e673b4c00056f7468657274000d4c496f54657374244e6f64653b78707400016171007e0003",
        "aced00057372000b496f54657374244e6f646500000000000000020200024c00046e616d657400124c6a6176612f6c616e672f537472696e673b4c00056f7468657274000d4c496f54657374244e6f64653b7870740003756e6f7071007e0003",
        "aced00057372000e496f5465737424576974685075740000000000000007030002490001614c0001627400124c6a6176612f6c616e672f537472696e673b78700000002a74000670756573746f78",
        "aced0005757200025b42acf317f8060854e00200007870000000020aff",
        "aced0005757200025b443ea68c14ab635a1e0200007870000000013fe0000000000000",
        "aced00057672000c496f5465737424506f696e74000000000000000102000249000178490001797870",
    };

    /**
     * It prints the streams of {@link #cases()} in hexadecimal, one per line.
     *
     * <p>It is where the strings of {@code EXPECTED} come from: it is run against the **real JDK**
     * and the result is pasted. It also serves for seeing which byte differs when a case fails.
     */
    public static int dump() {
        try {
            String[] h = cases();
            for (int k = 0; k < h.length; k++) System.out.println(h[k]);
        } catch (IOException e) {
            System.out.println("ERR " + e);
        }
        return -1;
    }

    private static int serializing() {
        int i = 400;
        try {
            String[] actual = cases();
            for (int k = 0; k < EXPECTED.length; k++) {
                if (!actual[k].equals(EXPECTED[k])) return i + k;   // 400..415
            }
            i = i + EXPECTED.length;

            // An empty stream is only the header: four bytes and nothing else.
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
            oos.flush();
            if (!hex(bos.toByteArray()).equals("aced0005")) return i; i++;   // 416

            // What is not serialisable does not come out, and it tells with the exception that
            // corresponds.
            try {
                ser(new Object());
                return i;                                                    // 417
            } catch (java.io.NotSerializableException expected) {
                i++;
            }
            return -1;
        } catch (IOException e) {
            return i;
        }
    }


    // ---- deserialisation (500) --------------------------------------------------------------------
    //
    // The mirror of the block above, and the only test that serves for the reading side: the inputs
    // are **the same streams the real JDK produced** --the strings of `EXPECTED`, untouched-- and
    // what is checked is that the objects the JDK gets come out of them. "It reads what I wrote"
    // would prove nothing: two routines that get it wrong in the same way meet that too.

    /** A class that reads its own: `defaultReadObject` and then the data the writer added. */
    static class Round implements Serializable {
        private static final long serialVersionUID = 20L;
        int n = 5;
        transient int doble;
        transient String extra;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(this.n * 2);
            out.writeUTF("mas");
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.doble = in.readInt();
            this.extra = in.readUTF();
        }
    }

    /**
     * The fields by name in both directions: `putFields` when writing, `readFields` when reading.
     */
    static class Named implements Serializable {
        private static final long serialVersionUID = 21L;
        int a;
        String b;
        transient boolean avisoDeInexistente;
        transient boolean porOmision;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            java.io.ObjectOutputStream.PutField pf = out.putFields();
            pf.put("a", 9);
            pf.put("b", "nueve");
            out.writeFields();
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            java.io.ObjectInputStream.GetField gf = in.readFields();
            this.a = gf.get("a", -1);
            this.b = (String) gf.get("b", null);
            // A name that exists neither in the stream nor in the class is an error of the caller,
            // not an absent field: it has to tell instead of returning them their own default
            // value.
            try {
                gf.get("nadaQueVer", 7);
            } catch (IllegalArgumentException expected) {
                this.avisoDeInexistente = true;
            }
            this.porOmision = gf.defaulted("a");
        }
    }

    /** It registers a validation, which can only run with the whole graph assembled. */
    static class Validated implements Serializable {
        private static final long serialVersionUID = 22L;
        static int validations;
        int v = 1;
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            in.registerValidation(new Witness(), 0);
        }
    }

    static class Witness implements java.io.ObjectInputValidation {
        public void validateObject() {
            Validated.validations = Validated.validations + 1;
        }
    }

    /** A filter that rejects a class and has no opinion about the rest. */
    static class Forbids implements java.io.ObjectInputFilter {
        private final Class<?> vetoed;
        Forbids(Class<?> vetoed) { this.vetoed = vetoed; }
        public java.io.ObjectInputFilter.Status checkInput(java.io.ObjectInputFilter.FilterInfo info) {
            if (info.serialClass() == this.vetoed) {
                return java.io.ObjectInputFilter.Status.REJECTED;
            }
            return java.io.ObjectInputFilter.Status.UNDECIDED;
        }
    }

    private static byte[] bytes(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int high = Character.digit(h.charAt(i * 2), 16);
            int low = Character.digit(h.charAt(i * 2 + 1), 16);
            b[i] = (byte) ((high << 4) | low);
        }
        return b;
    }

    private static java.io.ObjectInputStream stream(byte[] b) throws IOException {
        return new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(b));
    }

    /** It reads the single object of `hex`, which is a stream written by the real JDK. */
    private static Object des(String hex) throws Exception {
        return stream(bytes(hex)).readObject();
    }

    /** It writes `o` and reads it back in this same VM. */
    private static Object roundTrip(Object o) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        return stream(bos.toByteArray()).readObject();
    }

    private static int deserializing() {
        int i = 500;
        try {
            // --- the streams of the real JDK, read here ---
            if (des(EXPECTED[0]) != null) return i; i++;                       // 500
            if (!"hola".equals(des(EXPECTED[1]))) return i; i++;               // 501
            Point p = (Point) des(EXPECTED[2]);
            if (p.x != 3 || p.y != -4) return i; i++;                          // 502
            int[] ai = (int[]) des(EXPECTED[3]);
            if (ai.length != 3 || ai[0] != 1 || ai[1] != 2 || ai[2] != 3) return i; i++;  // 503
            String[] as = (String[]) des(EXPECTED[4]);
            if (as.length != 2 || !as[0].equals("a") || !as[1].equals("b")) return i; i++;  // 504
            AllTypes t = (AllTypes) des(EXPECTED[5]);
            if (!t.z || t.b != -2 || t.c != 'Z' || t.sh != -300) return i; i++;   // 505
            if (t.i != 70000 || t.j != -5000000000L) return i; i++;               // 506
            if (t.f != 1.5F || t.d != -2.25D || !t.s.equals("e\u00f1e")) return i; i++;  // 507
            // The `transient` does not travel **and the constructor does not run**: it is left at
            // the zero of the VM, not at the 99 the field initialiser gives it.
            if (t.noSale != 0) return i; i++;                                     // 508
            // Written with a `writeObject` of its own and read **without** `readObject`: the
            // default fields come out, and what the writer added is skipped by the block frame
            // without being understood.
            WithWriter ce = (WithWriter) des(EXPECTED[6]);
            if (ce.n != 5) return i; i++;                                         // 509
            Child h = (Child) des(EXPECTED[7]);
            if (h.arriba != 11 || h.abajo != 22) return i; i++;                   // 510
            External ex = (External) des(EXPECTED[8]);
            if (ex.v != 3) return i; i++;                                         // 511
            // The constant is looked up by name: **the same instance** has to come out, not a copy.
            if (des(EXPECTED[9]) != Color.VERDE) return i; i++;                    // 512
            Node cycle = (Node) des(EXPECTED[10]);
            if (!cycle.name.equals("a")) return i; i++;                          // 513
            if (cycle.other != cycle) return i; i++;                                // 514
            // Two `writeObject`s of the same object: on the other side there has to be **one**.
            java.io.ObjectInputStream in = stream(bytes(EXPECTED[11]));
            Object c1 = in.readObject();
            Object c2 = in.readObject();
            if (c1 != c2) return i; i++;                                           // 515
            if (!((Node) c1).name.equals("uno")) return i; i++;                  // 516
            WithPut cp = (WithPut) des(EXPECTED[12]);
            if (cp.a != 42 || !cp.b.equals("puesto")) return i; i++;               // 517
            byte[] ab = (byte[]) des(EXPECTED[13]);
            if (ab.length != 2 || ab[0] != 10 || ab[1] != -1) return i; i++;       // 518
            double[] ad = (double[]) des(EXPECTED[14]);
            if (ad.length != 1 || ad[0] != 0.5D) return i; i++;                    // 519
            if (des(EXPECTED[15]) != Point.class) return i; i++;                    // 520

            // --- what is only seen by reading ---
            Round r = (Round) roundTrip(new Round());
            if (r.n != 5 || r.doble != 10 || !"mas".equals(r.extra)) return i; i++;  // 521

            Named nb = (Named) roundTrip(new Named());
            if (nb.a != 9 || !"nueve".equals(nb.b)) return i; i++;                 // 522
            if (!nb.avisoDeInexistente) return i; i++;                             // 523
            // `a` came from the stream, so it did **not** come out by default.
            if (nb.porOmision) return i; i++;                                      // 524

            // The validation runs once, and after the graph is assembled.
            int before = Validated.validations;
            roundTrip(new Validated());
            if (Validated.validations != before + 1) return i; i++;                    // 525

            // `readUnshared`: two readings of the same object give **two** instances.
            Node one = new Node("u");
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
            oos.writeUnshared(one);
            oos.writeUnshared(one);
            oos.flush();
            java.io.ObjectInputStream su = stream(bos.toByteArray());
            Object u1 = su.readUnshared();
            Object u2 = su.readUnshared();
            if (u1 == u2) return i; i++;                                           // 526
            if (!((Node) u1).name.equals("u")) return i; i++;                     // 527

            // A backward reference **cannot** be read as unshared: it would return precisely the
            // object that was asked not to be shared.
            try {
                java.io.ObjectInputStream sc = stream(bytes(EXPECTED[11]));
                sc.readObject();
                sc.readUnshared();
                return i;                                                          // 528
            } catch (java.io.InvalidObjectException expected) {
                i++;
            }

            // A header that is not the one of the format is rejected in the constructor, not later.
            try {
                stream(bytes("cafe000570"));
                return i;                                                          // 529
            } catch (java.io.StreamCorruptedException expected) {
                i++;
            }

            // Asking for an object where the stream brings primitive data tells with the length,
            // which is the only thing that lets the caller go on reading.
            try {
                java.io.ByteArrayOutputStream bd = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream od = new java.io.ObjectOutputStream(bd);
                od.writeInt(7);
                od.flush();
                stream(bd.toByteArray()).readObject();
                return i;                                                          // 530
            } catch (java.io.OptionalDataException expected) {
                if (expected.length != 4 || expected.eof) return i;
                i++;
            }

            // A filter that rejects a class cuts the reading short before building it.
            try {
                java.io.ByteArrayOutputStream bf = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream of = new java.io.ObjectOutputStream(bf);
                of.writeObject(new Point(1, 1));
                of.flush();
                java.io.ObjectInputStream sf = stream(bf.toByteArray());
                sf.setObjectInputFilter(new Forbids(Point.class));
                sf.readObject();
                return i;                                                          // 531
            } catch (java.io.InvalidClassException expected) {
                i++;
            }

            // The filter is set once and for all: if it could be changed it would not be a policy.
            try {
                java.io.ObjectInputStream sf2 = stream(bytes(EXPECTED[1]));
                sf2.setObjectInputFilter(new Forbids(Point.class));
                sf2.setObjectInputFilter(new Forbids(Node.class));
                return i;                                                          // 532
            } catch (IllegalStateException expected) {
                i++;
            }

            // `reset()` cuts the memory of the stream short: what was written already comes out
            // whole again and on the other side they are **two** objects.
            Node comp = new Node("c");
            java.io.ByteArrayOutputStream br = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream or = new java.io.ObjectOutputStream(br);
            or.writeObject(comp);
            or.reset();
            or.writeObject(comp);
            or.flush();
            java.io.ObjectInputStream sr = stream(br.toByteArray());
            Object r1 = sr.readObject();
            Object r2 = sr.readObject();
            if (r1 == r2) return i; i++;                                           // 533
            if (!((Node) r2).name.equals("c")) return i; i++;                     // 534

            return -1;
        } catch (Throwable e) {
            return i;
        }
    }


    // ---- getChannel (600) -------------------------------------------------------------------------
    //
    // What `getChannel()` promises is not "a channel over the same file" but that the position of
    // the channel and that of the stream are **the same number**. It is the part that gets itself
    // wrong if each one keeps its own count, because nothing fails: it reads or writes in the place
    // that was not the right one and the caller receives perfectly believable bytes. That is why
    // each case moves one and looks at the other.

    private static int canales() {
        int i = 600;
        File f = null;
        File g = null;
        try {
            f = File.createTempFile("kajican", ".bytes");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            fos.close();

            // --- reading ---
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.nio.channels.FileChannel c = in.getChannel();
            // The same object at every call: the contract says "the unique FileChannel object".
            if (c != in.getChannel()) return i; i++;                     // 600
            if (c.position() != 0L) return i; i++;                       // 601
            if (c.size() != 10L) return i; i++;                          // 602
            // Reading from the stream moves the channel.
            if (in.read() != 0) return i; i++;                           // 603
            if (c.position() != 1L) return i; i++;                       // 604
            // Moving the channel changes where the stream reads from.
            c.position(5L);
            if (in.read() != 5) return i; i++;                           // 605
            if (c.position() != 6L) return i; i++;                       // 606
            // Reading through the channel also moves the stream.
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(2);
            if (c.read(bb) != 2) return i; i++;                          // 607
            if (bb.array()[0] != 6 || bb.array()[1] != 7) return i; i++; // 608
            if (in.read() != 8) return i; i++;                           // 609
            // The reading by absolute position does **not** move anything.
            java.nio.ByteBuffer abs = java.nio.ByteBuffer.allocate(1);
            if (c.read(abs, 0L) != 1 || abs.array()[0] != 0) return i; i++;  // 610
            if (c.position() != 9L) return i; i++;                        // 611
            // Going past the end is legal: `position()` returns what was set and -1 is read.
            c.position(100L);
            if (c.position() != 100L) return i; i++;                      // 612
            if (in.read() != -1) return i; i++;                           // 613
            // Read-only, like the JDK's.
            try {
                c.write(java.nio.ByteBuffer.allocate(1));
                return i;                                                 // 614
            } catch (java.nio.channels.NonWritableChannelException expected) { i++; }
            // Closing the stream closes the channel: they are the same thing seen in two ways.
            in.close();
            if (c.isOpen()) return i; i++;                                // 615

            // --- escritura ---
            g = File.createTempFile("kajican", ".out");
            java.io.FileOutputStream out = new java.io.FileOutputStream(g);
            java.nio.channels.FileChannel w = out.getChannel();
            if (w != out.getChannel()) return i; i++;                     // 616
            out.write(new byte[] { 10, 11, 12 });
            // The position counts what the stream wrote, **buffer included**: it is exactly the
            // number that would go out of step if the channel did not flush what is pending before
            // answering.
            if (w.position() != 3L) return i; i++;                        // 617
            if (w.size() != 3L) return i; i++;                            // 618
            // Writing through the channel continues where the stream was going.
            w.write(java.nio.ByteBuffer.wrap(new byte[] { 13 }));
            if (w.position() != 4L) return i; i++;                        // 619
            // And the stream goes on after the channel.
            out.write(14);
            out.close();
            byte[] readBack = new byte[8];
            java.io.FileInputStream v = new java.io.FileInputStream(g);
            int n = v.read(readBack);
            v.close();
            if (n != 5) return i; i++;                                    // 620
            if (readBack[0] != 10 || readBack[3] != 13 || readBack[4] != 14) return i; i++;  // 621

            // Moving the channel backwards changes **where the stream writes**, which is the half
            // of the contract that is lost if the stream goes on adding at the end on its own.
            java.io.FileOutputStream o2 = new java.io.FileOutputStream(g);
            java.nio.channels.FileChannel w2 = o2.getChannel();
            o2.write(new byte[] { 1, 2, 3, 4 });
            w2.position(1L);
            o2.write(new byte[] { 9 });
            o2.close();
            byte[] l2 = new byte[8];
            java.io.FileInputStream v2 = new java.io.FileInputStream(g);
            int n2 = v2.read(l2);
            v2.close();
            if (n2 != 4) return i; i++;                                   // 622
            if (l2[0] != 1 || l2[1] != 9 || l2[2] != 3 || l2[3] != 4) return i; i++;  // 623

            return -1;
        } catch (Throwable e) {
            return i;
        } finally {
            if (f != null) f.delete();
            if (g != null) g.delete();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("ser")) {
            try {
                String[] h = cases();
                for (int k = 0; k < h.length; k++) System.out.println(h[k]);
            } catch (IOException e) {
                System.out.println("ERR " + e);
            }
            return;
        }
        if (args.length > 0 && args[0].equals("uids")) {
            System.out.println(ObjectStreamClass.lookup(Simple.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(WithSuid.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(WithClinit.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(Marker.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(WithPrivates.class).getSerialVersionUID());
            return;
        }
        System.out.println(run());
    }
}
