package java.security;

// A PEM block exactly as it came out of the file: its label, its undecoded base64 body, and whatever
// was written before the "-----BEGIN".
//
// ===============================================================================================
// WHY THIS CLASS ARRIVED FIRST
// ===============================================================================================
//
// All three arrived together in JDK 25, but they do not ask for the same thing. This record
// **decodes nothing**: it keeps the text as it is, so it could be met in full from day one.
// `PEMDecoder` and `PEMEncoder`, on the other hand, have to turn the bytes into a `PrivateKey` or a
// `Certificate`, and for that they need a `KeyFactory` or a `CertificateFactory` that knows the
// algorithm; there is none registered in this library.
//
// **That argument stopped being enough and both are here.** What changed is that a label turned up
// whose object can be built without providers: `ENCRYPTED PRIVATE KEY` gives a
// `javax.crypto.EncryptedPrivateKeyInfo`, which reads its own DER. With that, and with the blocks of
// unknown label --which come back as this record-- most of the API is really met; what needs a
// factory does the lookup all the same and fails with the same exception the JDK throws when it
// finds nothing to build with. See `PEMDecoder`'s note.
//
// ===============================================================================================
// TWO THINGS THAT SURPRISE AND ARE CORRECT
// ===============================================================================================
//
// `content` is **not** the decoded content: it is the raw base64, without the dashes or the line
// breaks. And `leadingData` keeps whatever was there before the block --comments, a tool's output--
// which has to be preserved because it is sometimes signed along with the rest.
//
// Like every `record` with an array component, `equals` compares `leadingData` **by reference**, not
// by content, and neither the constructor nor {@link #leadingData()} copies it. It is left that way
// because it is exactly what the JDK does: changing it would give a class with different semantics
// under the same name, which is worse than the surprise.
public record PEMRecord(String type, String content, byte[] leadingData) implements DEREncodable {

    // How many base64 characters fit on a line. It is what RFC 7468 fixes and what any tool that
    // later reads the file expects.
    private static final int PER_LINE = 64;

    /**
     * The `type` is **the label alone**: "CERTIFICATE", not the whole line.
     *
     * <p>That is why anything that looks like already assembled PEM syntax is refused. Without that
     * check, a {@code new PEMRecord("BEGIN CERTIFICATE", ...)} would produce a
     * {@code -----BEGIN BEGIN CERTIFICATE-----} that no reader accepts, and the mistake would only
     * turn up when the file was read. Nothing else is validated: upper case, lower case or invented
     * labels all pass, because the record is not the one who decides which labels exist.
     *
     * <p>It is written as a full canonical constructor and not in the compact form
     * ({@code public PEMRecord { ... }}) because **our javac does not parse the compact form yet**;
     * see finding #403 in COMPILER_FINDINGS.md. The two forms are equivalent for the language: the
     * compact one only saves writing the three final assignments.
     */
    public PEMRecord(String type, String content, byte[] leadingData) {
        if (type == null) {
            throw new NullPointerException("\"type\" cannot be null.");
        }
        if (content == null) {
            throw new NullPointerException("\"content\" cannot be null.");
        }
        if (type.startsWith("-") || type.startsWith("BEGIN ") || type.startsWith("END ")) {
            throw new IllegalArgumentException("PEM syntax labels found.  "
                    + "Only the PEM type identifier is allowed");
        }
        this.type = type;
        this.content = content;
        this.leadingData = leadingData;
    }

    /**
     * `leadingData` stays {@code null}, which is not the same as an empty array: it means "there was
     * nothing before the block", not "there were zero bytes".
     */
    public PEMRecord(String type, String content) {
        this(type, content, null);
    }

    /**
     * The assembled PEM block, ready to write.
     *
     * <p>It separates with {@link System#lineSeparator()} and not with a fixed "\n" because that is
     * what the JDK does: the result is meant to go into a text file on the platform. That means
     * **the text that comes out depends on the system**, and a test comparing it has to build what
     * it expects with the same separator instead of writing it by hand.
     *
     * <p>There is always at least one line of content, even when `content` is empty: a block with no
     * line in the middle is not what any other tool produces.
     *
     * <p>It does not include `leadingData`. That is what was kept from **before** the block; the
     * block is this.
     */
    @Override
    public String toString() {
        String sep = System.lineSeparator();
        StringBuilder s = new StringBuilder();
        s.append("-----BEGIN ").append(type).append("-----").append(sep);
        int i = 0;
        do {
            int end = Math.min(i + PER_LINE, content.length());
            s.append(content, i, end).append(sep);
            i = end;
        } while (i < content.length());
        s.append("-----END ").append(type).append("-----").append(sep);
        return s.toString();
    }
}
