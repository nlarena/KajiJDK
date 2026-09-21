package java.security;

import java.io.Serializable;
import java.net.URL;
import java.security.cert.Certificate;

// Where the code came from: a URL and, optionally, who signed it.
//
// It is the "who you are" half of a policy decision. `Policy` looks at a `CodeSource` and answers
// which permissions correspond to it, and the `implies` of this class is what decides whether an
// entry of the policy —"everything below file:/opt/app/-"— applies to a concrete piece of code.
//
// ===============================================================================================
// `implies` IS A SECURITY DECISION, AND HERE IT IS DELIBERATELY STRICTER THAN THE JDK
// ===============================================================================================
//
// An `implies` that returns `true` too often grants permissions the policy did not want to grant.
// That is why, where the JDK uses the host wildcard logic of `SocketPermission` —which resolves
// names and accepts patterns such as `*.example.com`— here only the exact host and the total
// wildcard `*` are accepted. The difference is always in the safe direction: what gives `false`
// here and would give `true` in the JDK translates into a permission **not** granted, never the
// other way round.
//
// The certificates are kept but **not validated**: this class verifies no signature, and does not
// promise that whoever appears as the signer signed anything. The only thing `matchCerts` does is
// compare sets. The real verification would be done by whoever builds the `CodeSource`, and in this
// library there is nobody who can do it.
public class CodeSource implements Serializable {

    // null means "any origin", and that is why it implies them all.
    private final URL location;

    // The certificates of the signing chain, or null if the code does not come signed.
    private final Certificate[] certs;

    private final CodeSigner[] signers;

    public CodeSource(URL url, Certificate[] certs) {
        this.location = url;
        this.certs = certs == null ? null : copyOf(certs);
        this.signers = null;
    }

    public CodeSource(URL url, CodeSigner[] signers) {
        this.location = url;
        this.signers = signers == null ? null : copySigners(signers);
        this.certs = null;
    }

    private static Certificate[] copyOf(Certificate[] a) {
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static CodeSigner[] copySigners(CodeSigner[] a) {
        CodeSigner[] c = new CodeSigner[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    @Override
    public int hashCode() {
        return this.location == null ? 0 : this.location.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeSource)) {
            return false;
        }
        CodeSource cs = (CodeSource) obj;
        if (this.location == null) {
            if (cs.location != null) {
                return false;
            }
        } else if (!this.location.equals(cs.location)) {
            return false;
        }
        // Symmetric equality: each one has to have every certificate of the other.
        return this.hasAll(cs.getCertificates()) && cs.hasAll(this.getCertificates());
    }

    public final URL getLocation() {
        return this.location;
    }

    // The certificates of the signature, or null if there are none.
    //
    // When the `CodeSource` was built with signers, they are derived from them: each signer
    // contributes the certificates of its chain, in order.
    public final Certificate[] getCertificates() {
        if (this.certs != null) {
            return copyOf(this.certs);
        }
        if (this.signers == null) {
            return null;
        }
        java.util.ArrayList<Certificate> list = new java.util.ArrayList<Certificate>();
        int i = 0;
        while (i < this.signers.length) {
            java.util.List<? extends Certificate> cs =
                this.signers[i].getSignerCertPath().getCertificates();
            int j = 0;
            while (j < cs.size()) {
                list.add(cs.get(j));
                j = j + 1;
            }
            i = i + 1;
        }
        Certificate[] a = new Certificate[list.size()];
        int k = 0;
        while (k < list.size()) {
            a[k] = list.get(k);
            k = k + 1;
        }
        return a;
    }

    // The signers, or null if the `CodeSource` was built with loose certificates.
    //
    // A KajiLibrary subset: the JDK knows how to **deduce** the signers from an array of
    // certificates, splitting the list into chains by issuer. That requires reading the issuer and
    // the subject of each X.509, and here there is no X.509 parser. Returning null is saying "I do
    // not know", which is the truth; inventing a grouping would be asserting that certain
    // certificates form a chain without having checked it.
    public final CodeSigner[] getCodeSigners() {
        if (this.signers == null) {
            return null;
        }
        return copySigners(this.signers);
    }

    // Whether this `CodeSource` covers the other: the same or fewer signing requirements, and a
    // location that takes in the other's.
    public boolean implies(CodeSource codesource) {
        if (codesource == null) {
            return false;
        }
        return this.matchCerts(codesource) && this.matchLocation(codesource);
    }

    // The other has to bring **every** certificate this one demands. Bringing extra ones does not
    // matter: code signed by A and B satisfies a policy that asks only for A.
    private boolean matchCerts(CodeSource that) {
        Certificate[] mine = this.getCertificates();
        if (mine == null || mine.length == 0) {
            return true;
        }
        return that.hasAll(mine);
    }

    private boolean hasAll(Certificate[] wanted) {
        if (wanted == null || wanted.length == 0) {
            return true;
        }
        Certificate[] mine = this.getCertificates();
        if (mine == null) {
            return false;
        }
        int i = 0;
        while (i < wanted.length) {
            boolean hallado = false;
            int j = 0;
            while (j < mine.length) {
                if (wanted[i].equals(mine[j])) {
                    hallado = true;
                    j = mine.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // The comparison of locations. The bulk of the decision is in the suffix of the path:
    //
    //     ".../-"   everything that hangs from it, at any depth
    //     ".../*"   the files of that directory, without going deeper
    //     ".../"    whatever starts with that prefix
    //     other     exact equality
    private boolean matchLocation(CodeSource that) {
        if (this.location == null) {
            return true;
        }
        URL other = that.location;
        if (other == null) {
            return false;
        }
        if (this.location.equals(other)) {
            return true;
        }
        String p1 = this.location.getProtocol();
        String p2 = other.getProtocol();
        if (p1 == null || p2 == null || !p1.equalsIgnoreCase(p2)) {
            return false;
        }
        if (!this.matchHost(this.location.getHost(), other.getHost())) {
            return false;
        }
        int port = this.location.getPort();
        if (port != -1 && port != other.getPort()) {
            return false;
        }
        String ref = this.location.getRef();
        if (ref != null && !ref.equals(other.getRef())) {
            return false;
        }
        return this.matchFile(this.location.getFile(), other.getFile());
    }

    // Only the exact host or `*`. See the header: the JDK also accepts patterns with a partial
    // wildcard and equivalences by DNS, and not supporting them can only deny too much.
    private boolean matchHost(String mine, String other) {
        if (mine == null || mine.isEmpty()) {
            return true;
        }
        if (mine.equals("*")) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (mine.equalsIgnoreCase(other)) {
            return true;
        }
        // "" and "localhost" are the same machine in a `file:` or `http:` URL with no host.
        boolean mineIsLocal = mine.equalsIgnoreCase("localhost");
        boolean otherLoc = other.isEmpty() || other.equalsIgnoreCase("localhost");
        return mineIsLocal && otherLoc;
    }

    private boolean matchFile(String mine, String other) {
        if (mine == null) {
            return other == null;
        }
        if (other == null) {
            return false;
        }
        if (mine.endsWith("/-")) {
            return other.startsWith(mine.substring(0, mine.length() - 1));
        }
        if (mine.endsWith("/*")) {
            String prefix = mine.substring(0, mine.length() - 1);
            if (!other.startsWith(prefix)) {
                return false;
            }
            // Without going down a directory: what follows the prefix cannot have another slash.
            return other.indexOf('/', prefix.length()) < 0;
        }
        if (mine.endsWith("/")) {
            return other.startsWith(mine);
        }
        // A directory written without a trailing slash also covers itself with one.
        return mine.equals(other) || (mine + "/").equals(other);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        b.append(this.location);
        Certificate[] cs = this.getCertificates();
        if (cs == null || cs.length == 0) {
            b.append(" <no signer certificates>");
        } else {
            int i = 0;
            while (i < cs.length) {
                b.append("\n");
                b.append(cs[i].toString());
                i = i + 1;
            }
        }
        b.append(")");
        return b.toString();
    }
}
