package java.security.cert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// La extension NameConstraints (RFC 5280 §4.2.1.10): en que espacio de nombres puede emitir una CA.
//
// ===============================================================================================
// QUE DECIDE Y POR QUE ES DELICADA
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
// Es la unica forma de acotar una CA. Sin ella, cualquier CA en la que se confie puede emitir un
// certificado para **cualquier** nombre; con ella, una CA de una empresa queda encerrada en sus
// propios dominios aunque este en el almacen de confianza de todo el mundo. Equivocarse aca en el
// lado permisivo es dejar pasar un certificado que la CA no tenia derecho a emitir.
//
// Por eso cada regla de `GeneralNameValue.contains` esta escrita con su motivo, y por eso las tres
// formas de `GeneralName` que este paquete no entiende se comparan solo por igualdad exacta en vez
// de inventarles una contencion.
//
// ===============================================================================================
// LAS DOS REGLAS QUE NO ESTAN EN EL RFC
// ===============================================================================================
//
// Salieron de preguntarle al JDK 25, no de leer el RFC, y sin ellas el resultado difiere:
//
//  1. **El CN se comprueba tambien como nombre DNS**, pero solo si el certificado **no** trae
//     ningun dNSName en su SubjectAltName. Viene de cuando el nombre del host se ponia en el CN;
//     la condicion es la misma que usa un cliente TLS al elegir contra que comparar. Un CN que no
//     tiene forma de nombre DNS --`CN=Juan Perez`, con un espacio-- se saltea.
//  2. **El EMAILADDRESS del sujeto se comprueba como rfc822Name**, y tambien solo si no hay ningun
//     rfc822Name en el SubjectAltName.
//
// Sin la primera, un certificado con `CN=www.otro.com` emitido por una CA acotada a `acme.com`
// pasaria. Con ella no pasa. Es exactamente el agujero que la extension existe para tapar.
//
// ===============================================================================================
// LA REGLA DE "MISMO TIPO"
// ===============================================================================================
//
// Un nombre cuyo tipo **no** aparece en ningun subarbol permitido pasa sin mirarlo. Un nombre cuyo
// tipo **si** aparece tiene que caer adentro de alguno de los subarboles de su tipo, y si hay
// varios nombres de ese tipo tienen que caer **todos**. Es lo que hace el JDK y es lo unico que
// cierra: si un tipo sin subarbol se rechazara, una restriccion sobre dominios prohibiria de paso
// todos los correos.
//
// ===============================================================================================
// UNA DIFERENCIA CON EL JDK, ANOTADA
// ===============================================================================================
//
// El JDK trabaja sobre el certificado **codificado**: reparsea `getEncoded()` con su propia
// implementacion. Aca se usa el API publico -- `getSubjectX500Principal()` y
// `getExtensionValue("2.5.29.17")` --. Para cualquier certificado que cumpla su propio contrato el
// resultado es el mismo, y tiene la ventaja de que funciona con subclases que no traen codificacion
// pero si saben contestar sus campos.
final class NameConstraints {

    private static final String OID_SUBJECT_ALT_NAME = "2.5.29.17";

    private final List<GeneralNameValue> permitted = new ArrayList<GeneralNameValue>();
    private final List<GeneralNameValue> excluded = new ArrayList<GeneralNameValue>();

    /**
     * Lee la extension a partir del DER de su valor.
     *
     * @throws IOException si el DER no es una extension NameConstraints bien formada
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
            // Una extension sin ningun subarbol no restringe nada, y el RFC la prohibe
            // (`SIZE (1..MAX)`). Se rechaza en vez de tratarla como "todo permitido".
            throw new IOException("NameConstraints sin subarboles");
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
                throw new IOException("DER: se esperaba un GeneralSubtree");
            }
            DerReader one = new DerReader(der, treeAt, tree[1]);
            one.readTag();
            int oneLen = one.readLength();
            DerReader inner = new DerReader(der, one.position(), oneLen);
            int baseAt = inner.position();
            int[] base = inner.nextTlv();
            out.add(GeneralNameValue.ofTagged(der, baseAt, base[1]));
            // `minimum` y `maximum` se saltean. El JDK **rechaza** un certificado cuya extension
            // los traiga distintos del default, y con razon: nadie los implementa y tratarlos como
            // si no estuvieran cambiaria el alcance del subarbol.
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

    /** Si ese certificado cae adentro de lo que estas restricciones permiten. */
    boolean verify(X509Certificate cert) {
        try {
            return verify(namesOf(cert));
        } catch (IOException e) {
            // Un certificado cuyos nombres no se pueden leer no se puede afirmar que este adentro.
            return false;
        }
    }

    /** Si todos esos nombres caen adentro. Es lo que usa el criterio `pathToNames`. */
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
        // Ver la nota de la clase: sin subarbol de su tipo, el nombre no esta restringido.
        return !sameTypeSeen;
    }

    /**
     * Todos los nombres de un certificado que una restriccion tiene que mirar: el sujeto, sus
     * nombres alternativos, y los dos heredados del sujeto -- ver la nota de la clase.
     */
    static List<GeneralNameValue> namesOf(X509Certificate cert) throws IOException {
        List<GeneralNameValue> out = new ArrayList<GeneralNameValue>();
        javax.security.auth.x500.X500Principal subject = cert.getSubjectX500Principal();
        byte[] subjectDer = subject.getEncoded();
        if (subject.getName().length() > 0) {
            out.add(GeneralNameValue.ofDirectory(subjectDer));
        }
        boolean hayDns = false;
        boolean hayCorreo = false;
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
                    hayDns = true;
                }
                if (n.type() == GeneralNameValue.RFC822) {
                    hayCorreo = true;
                }
                out.add(n);
            }
        }
        if (!hayDns || !hayCorreo) {
            List<String[]> avas = DerReader.attributesOf(subjectDer);
            int i = 0;
            while (i < avas.size()) {
                String oid = avas.get(i)[0];
                String value = avas.get(i)[1];
                if (!hayDns && GeneralNameValue.commonNameOid().equals(oid)
                        && GeneralNameValue.looksLikeDns(value)) {
                    out.add(GeneralNameValue.ofString(GeneralNameValue.DNS, value));
                }
                if (!hayCorreo && GeneralNameValue.emailAddressOid().equals(oid)
                        && value.length() > 0) {
                    out.add(GeneralNameValue.ofString(GeneralNameValue.RFC822, value));
                }
                i = i + 1;
            }
        }
        return out;
    }
}
