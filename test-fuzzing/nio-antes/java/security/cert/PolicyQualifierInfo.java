package java.security.cert;

import java.io.IOException;

// A certificate policy qualifier (RFC 5280): an OID that says what type it is, plus the value,
// uninterpreted.
//
// The two that exist in practice are CPS (1.3.6.1.5.5.7.2.1), a URL to the declaration of practices
// of the CA, and UserNotice (1.3.6.1.5.5.7.2.2), a text to show the person. This class does **not**
// interpret them: it separates the OID from the rest and hands the rest over raw. It is the right
// thing, because the list of qualifiers is open and whoever knows a new one will know what to do
// with its bytes.
//
// It is the only class of the package with a constructor that parses DER, and it does it because
// its contract is exactly that: it receives bytes and has to return the OID inside. What is read is
// a SEQUENCE with an OBJECT IDENTIFIER in front and nothing else — there is no decision of trust
// involved, so it can be done properly. See `DerReader` for where the limit was put.
public class PolicyQualifierInfo {

    private final byte[] mEncoded;
    private final String mId;
    private final byte[] mData;

    // It reads the qualifier from its DER encoding.
    //
    // The bytes are demanded to be **exactly** one PolicyQualifierInfo: data left over at the end
    // is an error and not something that is ignored. Accepting extra tail would let two encodings
    // of the same value through, which is just what DER exists to prevent.
    public PolicyQualifierInfo(byte[] encoded) throws IOException {
        if (encoded.length < 3) {
            throw new IOException("Too short");
        }
        byte[] copyOf = new byte[encoded.length];
        System.arraycopy(encoded, 0, copyOf, 0, encoded.length);
        this.mEncoded = copyOf;

        DerReader d = new DerReader(copyOf, 0, copyOf.length);
        int tag = d.readTag();
        if (tag != DerReader.TAG_SEQUENCE) {
            throw new IOException("Invalid encoding for PolicyQualifierInfo");
        }
        int seqLen = d.readLength();
        int seqStart = d.position();
        if (seqStart + seqLen != copyOf.length) {
            throw new IOException("extra data at the end");
        }
        int oidLen = d.expect(DerReader.TAG_OID);
        int oidAt = d.skip(oidLen);
        this.mId = d.readOid(oidAt, oidLen);
        // What is left of the SEQUENCE is the qualifier, as it came. It may be of length zero: a
        // PolicyQualifierInfo with no qualifier is legal and returns an empty array, not null.
        int restAt = d.position();
        this.mData = d.copy(restAt, seqStart + seqLen - restAt);
    }

    // The OID of the type of qualifier, in dotted notation.
    public final String getPolicyQualifierId() {
        return this.mId;
    }

    // A copy of the complete encoding that was received.
    public final byte[] getEncoded() {
        byte[] c = new byte[this.mEncoded.length];
        System.arraycopy(this.mEncoded, 0, c, 0, this.mEncoded.length);
        return c;
    }

    // A copy of the value of the qualifier in DER, uninterpreted. Never null: empty if there is
    // none.
    public final byte[] getPolicyQualifier() {
        byte[] c = new byte[this.mData.length];
        System.arraycopy(this.mData, 0, c, 0, this.mData.length);
        return c;
    }

    // A KajiLibrary subset: the JDK prints the qualifier with its internal hexadecimal dump, with
    // offsets and an ASCII column. That format is not specified anywhere and is not worth
    // reproducing byte by byte; here the plain hexadecimal is printed. The structure of the lines
    // and the names of the fields are the same.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PolicyQualifierInfo: [\n");
        sb.append("  qualifierID: " + this.mId + "\n");
        sb.append("  qualifier: " + hex(this.mData) + "\n");
        sb.append("]");
        return sb.toString();
    }

    private static String hex(byte[] b) {
        String d = "0123456789ABCDEF";
        StringBuilder s = new StringBuilder();
        int i = 0;
        while (i < b.length) {
            int v = b[i] & 0xff;
            s.append(d.charAt(v >> 4));
            s.append(d.charAt(v & 15));
            i = i + 1;
        }
        return s.toString();
    }
}
