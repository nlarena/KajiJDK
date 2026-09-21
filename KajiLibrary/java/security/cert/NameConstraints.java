package java.security.cert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// The NameConstraints extension (RFC 5280 §4.2.1.10): in which name space a CA can issue.
//
// ===============================================================================================
// WHAT IT DECIDES AND WHY IT IS DELICATE
// ===============================================================================================
//
//   NameConstraints ::= SEQUENCE {
//       permittedSubtrees [0] GeneralSubtrees OPTIONAL,
//       excludedSubtrees  [1] GeneralSubtrees OPTIONAL }
//   GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
//   GeneralSubtree  ::= SEQUENCE { base GeneralName,
//                                  minimum [0] BaseDistance DEFAULT 0,
//                                  maximum [1] BaseDistance OPTIONAL }
//
// It is the only way of bounding a CA. Without it, any CA that is trusted can issue a certificate
// for **any** name; with it, the CA of a company is shut inside its own domains even though it is
// in everybody's trust store. Getting it wrong here on the permissive side is letting through a
// certificate the CA had no right to issue.
//
// That is why each rule of `GeneralNameValue.contains` is written with its reason, and that is why
// the three forms of `GeneralName` this package does not understand are compared only by exact
// equality instead of having a containment invented for them.
//
// ===============================================================================================
// THE TWO RULES THAT ARE NOT IN THE RFC
// ===============================================================================================
//
// They came out of asking JDK 25, not out of reading the RFC, and without them the result differs:
//
//  1. **The CN is also checked as a DNS name**, but only if the certificate does **not** bring any
//     dNSName in its SubjectAltName. It comes from when the name of the host was put in the CN;
//     the condition is the same a TLS client uses when choosing what to compare against. A CN that
//     has no form of a DNS name --`CN=Juan Perez`, with a space-- is skipped.
//  2. **The EMAILADDRESS of the subject is checked as an rfc822Name**, and also only if there is no
//     rfc822Name in the SubjectAltName.
//
// Without the first, a certificate with `CN=www.other.com` issued by a CA bounded to `acme.com`
// would pass. With it, it does not. It is exactly the hole the extension exists to plug.
//
// ===============================================================================================
// THE RULE OF "THE SAME TYPE"
// ===============================================================================================
//
// A name whose type does **not** appear in any permitted subtree passes without being looked at. A
// name whose type **does** appear has to fall inside one of the subtrees of its type, and if there
// are several names of that type they all have to fall inside. It is what the JDK does and it is
// the only thing that closes: if a type with no subtree were rejected, a constraint over domains
// would forbid every mail address in passing.
//
// ===============================================================================================
// A DIFFERENCE WITH THE JDK, NOTED
// ===============================================================================================
//
// The JDK works over the **encoded** certificate: it reparses `getEncoded()` with its own
// implementation. Here the public API is used -- `getSubjectX500Principal()` and
// `getExtensionValue("2.5.29.17")` --. For any certificate that fulfils its own contract the result
// is the same, and it has the advantage of working with subclasses that bring no encoding but do
// know how to answer their fields.
final class NameConstraints {

    private static final String OID_SUBJECT_ALT_NAME = "2.5.29.17";

    private final List<GeneralNameValue> permitted = new ArrayList<GeneralNameValue>();
    private final List<GeneralNameValue> excluded = new ArrayList<GeneralNameValue>();

    /**
     * It reads the extension from the DER of its value.
     *
     * @throws IOException if the DER is not a well formed NameConstraints extension
     */
    static NameConstraints of(byte[] der) throws IOException {
        NameConstraints nc = new NameConstraints();
        DerReader outer = new DerReader(der, 0, der.length);
        int len = outer.expect(DerReader.TAG_SEQUENCE);
        DerReader body = new DerReader(der, outer.position(), len);
        while (body.hasMore()) {
            int at = body.position();
            int[] field = body.nextTlv();
            if (field[2] == 0xa0) {
                readSubtrees(der, at, field[1], nc.permitted);
            } else if (field[2] == 0xa1) {
                readSubtrees(der, at, field[1], nc.excluded);
            } else {
                throw new IOException("DER: campo inesperado en NameConstraints");
            }
        }
        if (nc.permitted.isEmpty() && nc.excluded.isEmpty()) {
            // An extension with no subtree at all restricts nothing, and the RFC forbids it
            // (`SIZE (1..MAX)`). It is rejected instead of being treated as "everything permitted".
            throw new IOException("NameConstraints with no subtrees");
        }
        return nc;
    }

    private static void readSubtrees(byte[] der, int at, int total, List<GeneralNameValue> out)
            throws IOException {
        DerReader tagged = new DerReader(der, at, total);
        tagged.readTag();
        int len = tagged.readLength();
        DerReader trees = new DerReader(der, tagged.position(), len);
        while (trees.hasMore()) {
            int treeAt = trees.position();
            int[] tree = trees.nextTlv();
            if (tree[2] != DerReader.TAG_SEQUENCE) {
                throw new IOException("DER: a GeneralSubtree was expected");
            }
            DerReader one = new DerReader(der, treeAt, tree[1]);
            one.readTag();
            int oneLen = one.readLength();
            DerReader inner = new DerReader(der, one.position(), oneLen);
            int baseAt = inner.position();
            int[] base = inner.nextTlv();
            out.add(GeneralNameValue.ofTagged(der, baseAt, base[1]));
            // `minimum` and `maximum` are skipped. The JDK **rejects** a certificate whose
            // extension brings them different from the default, and with reason: nobody implements
            // them and treating them as if they were not there would change the reach of the
            // subtree.
            while (inner.hasMore()) {
                int[] extra = inner.nextTlv();
                if (extra[2] == 0xa0 || extra[2] == 0xa1) {
                    throw new IOException(
                        "Non-default BaseDistance in name constraints is not supported");
                }
                throw new IOException("DER: campo inesperado en GeneralSubtree");
            }
        }
    }

    /** Whether that certificate falls inside what these constraints permit. */
    boolean verify(X509Certificate cert) {
        try {
            return verify(namesOf(cert));
        } catch (IOException e) {
            // A certificate whose names cannot be read cannot be asserted to be inside.
            return false;
        }
    }

    /** Whether all those names fall inside. It is what the `pathToNames` criterion uses. */
    boolean verify(List<GeneralNameValue> names) {
        int i = 0;
        while (i < names.size()) {
            if (!allows(names.get(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private boolean allows(GeneralNameValue name) {
        int i = 0;
        while (i < this.excluded.size()) {
            if (this.excluded.get(i).contains(name)) {
                return false;
            }
            i = i + 1;
        }
        boolean sameTypeSeen = false;
        i = 0;
        while (i < this.permitted.size()) {
            GeneralNameValue base = this.permitted.get(i);
            if (base.type() == name.type()) {
                if (base.contains(name)) {
                    return true;
                }
                sameTypeSeen = true;
            }
            i = i + 1;
        }
        // See the note of the class: with no subtree of its type, the name is not restricted.
        return !sameTypeSeen;
    }

    /**
     * Every name of a certificate a constraint has to look at: the subject, its alternative names,
     * and the two inherited from the subject -- see the note of the class.
     */
    static List<GeneralNameValue> namesOf(X509Certificate cert) throws IOException {
        List<GeneralNameValue> out = new ArrayList<GeneralNameValue>();
        javax.security.auth.x500.X500Principal subject = cert.getSubjectX500Principal();
        byte[] subjectDer = subject.getEncoded();
        if (subject.getName().length() > 0) {
            out.add(GeneralNameValue.ofDirectory(subjectDer));
        }
        boolean hasDns = false;
        boolean hasEmail = false;
        byte[] ext = cert.getExtensionValue(OID_SUBJECT_ALT_NAME);
        if (ext != null) {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            DerReader list = new DerReader(value, d.position(), len);
            while (list.hasMore()) {
                int at = list.position();
                int[] one = list.nextTlv();
                GeneralNameValue n = GeneralNameValue.ofTagged(value, at, one[1]);
                if (n.type() == GeneralNameValue.DNS) {
                    hasDns = true;
                }
                if (n.type() == GeneralNameValue.RFC822) {
                    hasEmail = true;
                }
                out.add(n);
            }
        }
        if (!hasDns || !hasEmail) {
            List<String[]> avas = DerReader.attributesOf(subjectDer);
            int i = 0;
            while (i < avas.size()) {
                String oid = avas.get(i)[0];
                String value = avas.get(i)[1];
                if (!hasDns && GeneralNameValue.commonNameOid().equals(oid)
                        && GeneralNameValue.looksLikeDns(value)) {
                    out.add(GeneralNameValue.ofString(GeneralNameValue.DNS, value));
                }
                if (!hasEmail && GeneralNameValue.emailAddressOid().equals(oid)
                        && value.length() > 0) {
                    out.add(GeneralNameValue.ofString(GeneralNameValue.RFC822, value));
                }
                i = i + 1;
            }
        }
        return out;
    }
}
