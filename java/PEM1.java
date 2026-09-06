import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.DEREncodable;
import java.security.PEMDecoder;
import java.security.PEMEncoder;
import java.security.PEMRecord;
import java.security.PublicKey;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Checks {@code PEMDecoder} and {@code PEMEncoder} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>Reading and writing the format: finding the block, splitting off the label, keeping what came
 * before, cutting the base64 into lines. None of that depends on a provider and it is most of the
 * two classes.
 *
 * <p>Of the known labels, {@code ENCRYPTED PRIVATE KEY} is compared, being the only one whose object
 * can be built without providers. The rest fail on both sides with the same exception --
 * {@link IllegalArgumentException} -- though for different reasons: the JDK because the test data is
 * not a real certificate, this library because there is no factory. They are compared all the same,
 * and the note is written here so that nobody reads agreement where there are two different paths.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class PEM1 {

    static final String[] EXPECTED = {
        "single|true|true",
        "immutable|false|false",
        "with-null|NullPointerException|NullPointerException|NullPointerException",
        "class|true",
        "record|ODD THING|AQIDBA==|true",
        "round-trip|-----BEGIN ODD THING-----\\r\\nAQIDBA==\\r\\n-----END ODD THING-----\\r\\n",
        "bytes|true",
        "before|junk before\\r\\n|-----BEGIN ODD THING-----\\r\\nAQIDBA==\\r\\n-----END ODD THING-----\\r\\n",
        "wrapping|-----BEGIN X-----\\r\\nABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKL\\r\\nMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUV\\r\\n-----END X-----\\r\\n",
        "cleanup|AQIDBA==",
        "stream|ODD THING|AQIDBA==",
        "epki-written|-----BEGIN ENCRYPTED PRIVATE KEY-----\\r\\nMBIwCwYJKoZIhvcNAQUDBAMBAgM=\\r\\n-----END ENCRYPTED PRIVATE KEY-----\\r\\n",
        "epki-read|true|PBEWithMD5AndDES|010203",
        "epki-class|PBEWithMD5AndDES",
        "epki-as-record|ENCRYPTED PRIVATE KEY",
        "null|NullPointerException",
        "empty|IllegalArgumentException",
        "no-block|IllegalArgumentException",
        "no-closing|IllegalArgumentException",
        "encode-null|NullPointerException",
        "wrong-class|ClassCastException",
        "cert|IllegalArgumentException",
        "public|IllegalArgumentException",
    };

    /** What the two classes do, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final String nl = System.lineSeparator();

        final PEMDecoder d = PEMDecoder.of();
        final PEMEncoder e = PEMEncoder.of();
        a.add("single|" + (PEMDecoder.of() == d) + "|" + (PEMEncoder.of() == e));
        a.add("immutable|" + (d.withDecryption(new char[] {'x'}) == d)
                + "|" + (e.withEncryption(new char[] {'x'}) == e));
        a.add("with-null|" + attempt(new WithFactory(d)) + "|" + attempt(new WithKey(d))
                + "|" + attempt(new WithEncryption(e)));

        // A block with an unknown label comes back as a record.
        final String pem = "-----BEGIN ODD THING-----" + nl + "AQIDBA==" + nl
                + "-----END ODD THING-----" + nl;
        final DEREncodable x = d.decode(pem);
        a.add("class|" + (x instanceof PEMRecord));
        final PEMRecord r = d.decode(pem, PEMRecord.class);
        a.add("record|" + r.type() + "|" + r.content() + "|" + (r.leadingData() == null));
        a.add("round-trip|" + escape(e.encodeToString(r)));
        a.add("bytes|" + new String(e.encode(r), StandardCharsets.UTF_8)
                .equals(e.encodeToString(r)));

        // What comes before the block is kept and not written out again.
        final PEMRecord r2 = d.decode("junk before" + nl + pem, PEMRecord.class);
        a.add("before|" + escape(new String(r2.leadingData(), StandardCharsets.UTF_8))
                + "|" + escape(e.encodeToString(r2)));

        // A long body is cut into lines of sixty-four.
        final StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            long_.append((char) ('A' + i % 26));
        }
        a.add("wrapping|" + escape(new PEMRecord("X", long_.toString()).toString()));

        // The line breaks and spaces of the body are dropped on reading.
        final PEMRecord r3 = d.decode("-----BEGIN Y-----" + nl + "AQ  ID" + nl + "BA==" + nl
                + "-----END Y-----" + nl, PEMRecord.class);
        a.add("cleanup|" + r3.content());

        // Reading from a stream gives the same as reading from a text.
        final PEMRecord r4 = d.decode(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)), PEMRecord.class);
        a.add("stream|" + r4.type() + "|" + r4.content());

        // The one known label that can be built without providers.
        final EncryptedPrivateKeyInfo epki =
                new EncryptedPrivateKeyInfo("PBEWithMD5AndDES", new byte[] {1, 2, 3});
        final String text = e.encodeToString(epki);
        a.add("epki-written|" + escape(text));
        final DEREncodable read = d.decode(text);
        a.add("epki-read|" + (read instanceof EncryptedPrivateKeyInfo) + "|"
                + ((EncryptedPrivateKeyInfo) read).getAlgName() + "|"
                + hex(((EncryptedPrivateKeyInfo) read).getEncryptedData()));
        a.add("epki-class|" + d.decode(text, EncryptedPrivateKeyInfo.class).getAlgName());
        a.add("epki-as-record|" + d.decode(text, PEMRecord.class).type());

        // The errors.
        a.add("null|" + attempt(new Decode(d, null)));
        a.add("empty|" + attempt(new Decode(d, "")));
        a.add("no-block|" + attempt(new Decode(d, "not pem at all")));
        a.add("no-closing|" + attempt(new Decode(d, "-----BEGIN X-----" + nl + "AQID" + nl)));
        a.add("encode-null|" + attempt(new Encode(e)));
        a.add("wrong-class|" + attempt(new DecodeAs(d, pem)));
        a.add("cert|" + attempt(new Decode(d,
                "-----BEGIN CERTIFICATE-----" + nl + "AQID" + nl + "-----END CERTIFICATE-----" + nl)));
        a.add("public|" + attempt(new Decode(d,
                "-----BEGIN PUBLIC KEY-----" + nl + "AQID" + nl + "-----END PUBLIC KEY-----" + nl)));

        return a.toArray(new String[a.size()]);
    }

    /** The line breaks written out, so the comparison does not depend on the system. */
    static String escape(String s) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** The bytes in hexadecimal. */
    static String hex(byte[] b) {
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            s.append("0123456789abcdef".charAt(v >> 4));
            s.append("0123456789abcdef".charAt(v & 0xf));
        }
        return s.toString();
    }

    /** Something run to see what it fails with. */
    interface Throwing {
        void run() throws Exception;
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Throwing r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class WithFactory implements Throwing {
        private final PEMDecoder d;

        WithFactory(PEMDecoder d) {
            this.d = d;
        }

        public void run() throws Exception {
            d.withFactory(null);
        }
    }

    static class WithKey implements Throwing {
        private final PEMDecoder d;

        WithKey(PEMDecoder d) {
            this.d = d;
        }

        public void run() throws Exception {
            d.withDecryption(null);
        }
    }

    static class WithEncryption implements Throwing {
        private final PEMEncoder e;

        WithEncryption(PEMEncoder e) {
            this.e = e;
        }

        public void run() throws Exception {
            e.withEncryption(null);
        }
    }

    static class Decode implements Throwing {
        private final PEMDecoder d;
        private final String s;

        Decode(PEMDecoder d, String s) {
            this.d = d;
            this.s = s;
        }

        public void run() throws Exception {
            d.decode(this.s);
        }
    }

    static class DecodeAs implements Throwing {
        private final PEMDecoder d;
        private final String s;

        DecodeAs(PEMDecoder d, String s) {
            this.d = d;
            this.s = s;
        }

        public void run() throws Exception {
            d.decode(this.s, PublicKey.class);
        }
    }

    static class Encode implements Throwing {
        private final PEMEncoder e;

        Encode(PEMEncoder e) {
            this.e = e;
        }

        public void run() throws Exception {
            e.encodeToString(null);
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
