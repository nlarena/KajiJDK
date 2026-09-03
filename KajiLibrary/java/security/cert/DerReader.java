package java.security.cert;

import java.io.IOException;

// Un lector de DER minimo, solo para las tres estructuras que este paquete necesita leer de verdad.
//
// ===============================================================================================
// POR QUE ESTO EXISTE Y HASTA DONDE LLEGA
// ===============================================================================================
//
// Casi todo `java.security.cert` puede ser honesto sin parsear nada: `X509Certificate` es abstracta,
// los selectores comparan lo que el certificado ya devuelve, y las fabricas delegan en un
// proveedor. Pero quedan tres metodos **concretos** del API cuyo contrato es literalmente "leer un
// pedacito de DER":
//
//   - `PolicyQualifierInfo`, que decodifica un SEQUENCE con un OID adelante.
//   - `X509Certificate.getExtendedKeyUsage()`, que decodifica un SEQUENCE OF OID.
//   - `X509CRLEntry.getRevocationReason()`, que decodifica un ENUMERATED.
//
// Los tres son **codificacion, no criptografia**: no hay ninguna decision de confianza aca. Un bug
// en este archivo produce un OID mal leido o una excepcion, nunca una firma que se acepta sin
// verificar. Por eso se puede escribir sin violar la regla de la casa, a diferencia de lo que
// pasaria con un parser de certificados completo o con un comparador de nombres X.500 —donde
// equivocarse **si** es un agujero, y por eso no estan.
//
// Desde que `javax.security.auth.x500.X500Principal` existe en esta biblioteca se agrego un cuarto
// caso: **caminar** hasta el campo `issuer` o `subject` y recortar sus bytes. Vale la pena decir por
// que eso no cruza el limite de arriba. Este archivo no interpreta el nombre —no lo parsea, no lo
// compara, no lo canoniza—: cuenta campos de un SEQUENCE y devuelve un tramo. Quien entiende ese
// tramo es `X500Principal`, que trae su propio decodificador y su propia forma canonica. Separarlo
// asi es lo que hace que la parte riesgosa —comparar dos nombres— este en un solo lugar y probada.
//
// Lo que este lector sigue sin hacer, y por lo tanto lo que no se declara en el paquete:
// `GeneralName` —y con el, las listas de nombres alternativos y las restricciones de nombres— y
// cualquier cosa con largo indefinido.
//
// Se validan las reglas de DER que importan para no aceptar codificaciones ambiguas: largo
// indefinido prohibido, largos en forma minima, componentes de OID sin ceros a la izquierda.
final class DerReader {

    private final byte[] buf;
    private int pos;
    private final int end;

    DerReader(byte[] buf, int from, int len) {
        this.buf = buf;
        this.pos = from;
        this.end = from + len;
    }

    boolean hasMore() {
        return this.pos < this.end;
    }

    int position() {
        return this.pos;
    }

    int limit() {
        return this.end;
    }

    // Lee el byte de etiqueta.
    int readTag() throws IOException {
        if (this.pos >= this.end) {
            throw new IOException("DER truncado: falta la etiqueta");
        }
        int t = this.buf[this.pos] & 0xff;
        this.pos = this.pos + 1;
        // Las etiquetas de forma larga (los cinco bits bajos en 1) no aparecen en nada de lo que
        // este paquete lee, y aceptarlas sin saber decodificarlas seria peor que rechazarlas.
        if ((t & 0x1f) == 0x1f) {
            throw new IOException("DER: etiqueta de forma larga no soportada");
        }
        return t;
    }

    // Lee el campo de largo y devuelve cuantos bytes de contenido siguen.
    int readLength() throws IOException {
        if (this.pos >= this.end) {
            throw new IOException("DER truncado: falta el largo");
        }
        int b0 = this.buf[this.pos] & 0xff;
        this.pos = this.pos + 1;
        if (b0 < 0x80) {
            return b0;
        }
        // 0x80 es el largo indefinido de BER. DER lo prohibe, y aceptarlo abriria la puerta a que
        // el mismo valor tenga dos codificaciones.
        if (b0 == 0x80) {
            throw new IOException("DER: largo indefinido no permitido");
        }
        int n = b0 & 0x7f;
        // Mas de cuatro bytes de largo no entra en un int, y nada de lo que se lee aca se acerca.
        if (n > 4) {
            throw new IOException("DER: largo demasiado grande");
        }
        int v = 0;
        int i = 0;
        while (i < n) {
            if (this.pos >= this.end) {
                throw new IOException("DER truncado: largo incompleto");
            }
            v = (v << 8) | (this.buf[this.pos] & 0xff);
            this.pos = this.pos + 1;
            i = i + 1;
        }
        if (v < 0) {
            throw new IOException("DER: largo demasiado grande");
        }
        return v;
    }

    // Consume el contenido de un valor y devuelve donde empieza.
    int skip(int len) throws IOException {
        int from = this.pos;
        if (len < 0 || this.pos + len > this.end) {
            throw new IOException("DER truncado: contenido incompleto");
        }
        this.pos = this.pos + len;
        return from;
    }

    // Consume el siguiente valor completo y devuelve {inicioDelTlv, largoTotal, etiqueta}.
    //
    // Es lo que hace falta para recortar un campo **con su cabecera**: un `Name` en DER solo se
    // puede volver a decodificar si viene entero, con su SEQUENCE adelante.
    int[] nextTlv() throws IOException {
        int inicio = this.pos;
        int tag = readTag();
        int len = readLength();
        skip(len);
        return new int[] {inicio, this.pos - inicio, tag};
    }

    // Comprueba que la etiqueta sea la esperada y devuelve el largo del contenido.
    int expect(int tag) throws IOException {
        int t = readTag();
        if (t != tag) {
            throw new IOException("DER: se esperaba la etiqueta 0x"
                + Integer.toHexString(tag) + " y vino 0x" + Integer.toHexString(t));
        }
        return readLength();
    }

    // Decodifica un OBJECT IDENTIFIER a su notacion de puntos.
    //
    // El primer sub-identificador codifica **dos** arcos juntos como 40*a1 + a2. El truco existe
    // porque a1 solo puede valer 0, 1 o 2, asi que hay lugar de sobra; el precio es que para a1 = 2
    // el segundo arco no tiene techo, y por eso el corte de a1 no se puede hacer dividiendo por 40
    // sin mirar el rango.
    String readOid(int from, int len) throws IOException {
        if (len <= 0) {
            throw new IOException("DER: OID vacio");
        }
        int i = from;
        int to = from + len;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (i < to) {
            long v = 0;
            int bytes = 0;
            // Un componente que arranca con 0x80 tendria un cero a la izquierda: DER exige la
            // codificacion mas corta, asi que eso es invalido y no simplemente redundante.
            if ((this.buf[i] & 0xff) == 0x80) {
                throw new IOException("DER: componente de OID con codificacion no minima");
            }
            while (true) {
                if (i >= to) {
                    throw new IOException("DER: OID truncado");
                }
                int x = this.buf[i] & 0xff;
                i = i + 1;
                bytes = bytes + 1;
                // Nueve grupos de siete bits ya se pasan de un long: cortamos antes de desbordar.
                if (bytes > 9) {
                    throw new IOException("DER: componente de OID demasiado grande");
                }
                v = (v << 7) | (x & 0x7f);
                if ((x & 0x80) == 0) {
                    break;
                }
            }
            if (first) {
                long a1;
                long a2;
                if (v < 40) {
                    a1 = 0;
                    a2 = v;
                } else if (v < 80) {
                    a1 = 1;
                    a2 = v - 40;
                } else {
                    a1 = 2;
                    a2 = v - 80;
                }
                sb.append(a1);
                sb.append('.');
                sb.append(a2);
                first = false;
            } else {
                sb.append('.');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    // Copia un tramo del buffer.
    byte[] copy(int from, int len) {
        byte[] c = new byte[len];
        System.arraycopy(this.buf, from, c, 0, len);
        return c;
    }

    // Lee un INTEGER con signo, en complemento a dos y big-endian, como lo codifica DER.
    java.math.BigInteger readInteger(int from, int len) throws IOException {
        if (len <= 0) {
            throw new IOException("DER: INTEGER vacio");
        }
        return new java.math.BigInteger(copy(from, len));
    }

    // Un GeneralizedTime en milisegundos desde la epoca.
    //
    // Se acepta **solo** la forma que DER obliga: `YYYYMMDDHHMMSSZ`, con la fraccion de segundo
    // opcional y siempre en UTC. BER permite ademas omitir los segundos y escribir un desfasaje
    // horario; eso se rechaza a proposito, porque aceptar dos codificaciones del mismo instante es
    // justo lo que DER existe para evitar y porque un certificado conforme nunca las usa.
    //
    // La cuenta de dias es la de Howard Hinnant, con el año corrido para que febrero quede al final:
    // asi el dia bisiesto es el ultimo del ciclo y no hay que tratarlo aparte. Todo en enteros, sin
    // pasar por ninguna clase de fecha.
    static long generalizedTime(byte[] buf, int from, int len) throws IOException {
        if (len < 15) {
            throw new IOException("DER: GeneralizedTime demasiado corto");
        }
        String s = new String(buf, from, len, java.nio.charset.StandardCharsets.US_ASCII);
        if (s.charAt(s.length() - 1) != 'Z') {
            throw new IOException("DER: GeneralizedTime sin Z");
        }
        int year = digitsAt(s, 0, 4);
        int month = digitsAt(s, 4, 2);
        int day = digitsAt(s, 6, 2);
        int hour = digitsAt(s, 8, 2);
        int minute = digitsAt(s, 10, 2);
        int second = digitsAt(s, 12, 2);
        if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59
                || second > 60) {
            throw new IOException("DER: GeneralizedTime fuera de rango: " + s);
        }
        long milis = 0;
        // La fraccion, si esta, va entre los segundos y la Z. Se leen hasta tres digitos: mas
        // precision que un milisegundo no entra en lo que devuelve este metodo.
        if (s.length() > 15) {
            if (s.charAt(14) != '.' && s.charAt(14) != ',') {
                throw new IOException("DER: GeneralizedTime con sobrante: " + s);
            }
            int i = 15;
            int escala = 100;
            while (i < s.length() - 1) {
                char c = s.charAt(i);
                if (c < '0' || c > '9') {
                    throw new IOException("DER: fraccion no numerica: " + s);
                }
                if (escala > 0) {
                    milis = milis + (c - '0') * escala;
                    escala = escala / 10;
                }
                i = i + 1;
            }
        }
        long y = year;
        if (month <= 2) {
            y = y - 1;
        }
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468;
        return ((days * 24 + hour) * 60 + minute) * 60000L + second * 1000L + milis;
    }

    private static int digitsAt(String s, int from, int count) throws IOException {
        int v = 0;
        int i = 0;
        while (i < count) {
            char c = s.charAt(from + i);
            if (c < '0' || c > '9') {
                throw new IOException("DER: se esperaba un digito en \"" + s + "\"");
            }
            v = v * 10 + (c - '0');
            i = i + 1;
        }
        return v;
    }

    // Etiquetas universales que se usan en este paquete.
    static final int TAG_INTEGER = 0x02;
    static final int TAG_ENUMERATED = 0x0a;
    static final int TAG_OID = 0x06;
    static final int TAG_OCTET_STRING = 0x04;
    static final int TAG_SEQUENCE = 0x30;

    // Comprueba que un string sea un OID valido en notacion de puntos, con las mismas reglas que
    // usa el JDK al construir uno.
    //
    // Las tres reglas no son arbitrarias y salen de como se codifica un OID en DER: el primer
    // sub-identificador guarda los dos primeros arcos juntos como 40*a1 + a2, y eso solo cierra si
    // a1 vale 0, 1 o 2 —y, cuando vale 0 o 1, si a2 se queda abajo de 40—. Un OID de un solo arco
    // directamente no se puede codificar.
    static void validateOid(String oid) throws IOException {
        if (oid == null) {
            throw new NullPointerException("oid is null");
        }
        int partes = 0;
        int i = 0;
        int len = oid.length();
        long first = -1;
        long second = -1;
        while (i <= len) {
            int corte = oid.indexOf('.', i);
            if (corte < 0) {
                corte = len;
            }
            if (corte == i) {
                throw new IOException(
                    "ObjectIdentifier() -- Invalid format: componente vacio en \"" + oid + "\"");
            }
            long v = 0;
            int j = i;
            while (j < corte) {
                char c = oid.charAt(j);
                if (c < '0' || c > '9') {
                    throw new IOException(
                        "ObjectIdentifier() -- Invalid format: \"" + oid + "\"");
                }
                v = v * 10 + (c - '0');
                // Se corta antes de desbordar; ningun OID real se acerca a este orden.
                if (v > 0x7fffffffL) {
                    throw new IOException(
                        "ObjectIdentifier() -- componente demasiado grande en \"" + oid + "\"");
                }
                j = j + 1;
            }
            if (partes == 0) {
                first = v;
            } else if (partes == 1) {
                second = v;
            }
            partes = partes + 1;
            i = corte + 1;
        }
        if (partes < 2) {
            throw new IOException("ObjectIdentifier() -- Must be at least two oid components ");
        }
        if (first > 2) {
            throw new IOException("ObjectIdentifier() -- First oid component is invalid ");
        }
        if (first < 2 && second > 39) {
            throw new IOException("ObjectIdentifier() -- Second oid component is invalid ");
        }
    }

    // Desenvuelve el OCTET STRING con el que viaja el valor de una extension X.509 y devuelve su
    // contenido. `getExtensionValue` entrega siempre esa envoltura, nunca el valor pelado.
    static byte[] unwrapOctetString(byte[] ext) throws IOException {
        DerReader d = new DerReader(ext, 0, ext.length);
        int len = d.expect(TAG_OCTET_STRING);
        int from = d.skip(len);
        if (d.hasMore()) {
            throw new IOException("DER: datos de mas despues del OCTET STRING");
        }
        return d.copy(from, len);
    }

    // Los bytes del `issuer` o del `subject` de un certificado X.509, con su cabecera.
    //
    //   Certificate     ::= SEQUENCE { tbsCertificate, signatureAlgorithm, signatureValue }
    //   TBSCertificate  ::= SEQUENCE { [0] version DEFAULT v1, serialNumber, signature,
    //                                  issuer, validity, subject, ... }
    //
    // El unico campo opcional que hay antes del emisor es la version, y viene con etiqueta
    // explicita `[0]` (0xa0), asi que se distingue de un INTEGER sin ambiguedad. De ahi en adelante
    // las posiciones son fijas y alcanza con contar.
    static byte[] certificateName(byte[] der, boolean subject) throws IOException {
        if (der == null) {
            throw new IOException("el certificado no tiene codificacion");
        }
        DerReader outer = new DerReader(der, 0, der.length);
        int certLen = outer.expect(TAG_SEQUENCE);
        int inicioCert = outer.position();
        DerReader cert = new DerReader(der, inicioCert, certLen);
        int tbsLen = cert.expect(TAG_SEQUENCE);
        DerReader tbs = new DerReader(der, cert.position(), tbsLen);

        int[] field = tbs.nextTlv();
        // La version es opcional: si el primer campo no es el `[0]`, ya estabamos parados en la
        // serie y no hay que consumir nada de mas.
        if (field[2] == 0xa0) {
            field = tbs.nextTlv();
        }
        if (field[2] != TAG_INTEGER) {
            throw new IOException("DER: se esperaba el numero de serie");
        }
        field = tbs.nextTlv();
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el algoritmo de firma");
        }
        int[] issuer = tbs.nextTlv();
        if (issuer[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el nombre del emisor");
        }
        if (!subject) {
            return tbs.copy(issuer[0], issuer[1]);
        }
        field = tbs.nextTlv();
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el periodo de validez");
        }
        int[] suj = tbs.nextTlv();
        if (suj[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el nombre del sujeto");
        }
        return tbs.copy(suj[0], suj[1]);
    }

    // Los bytes del `issuer` de una CRL, con su cabecera.
    //
    //   CertificateList ::= SEQUENCE { tbsCertList, signatureAlgorithm, signatureValue }
    //   TBSCertList     ::= SEQUENCE { version OPTIONAL, signature, issuer, thisUpdate, ... }
    //
    // A diferencia del certificado, aca la version opcional **no** lleva etiqueta explicita: es un
    // INTEGER pelado. Por eso se decide mirando la etiqueta y no contando: si el primer campo es un
    // INTEGER es la version, y si es un SEQUENCE ya es el algoritmo de firma.
    static byte[] crlName(byte[] der) throws IOException {
        if (der == null) {
            throw new IOException("la CRL no tiene codificacion");
        }
        DerReader outer = new DerReader(der, 0, der.length);
        int listLen = outer.expect(TAG_SEQUENCE);
        DerReader list = new DerReader(der, outer.position(), listLen);
        int tbsLen = list.expect(TAG_SEQUENCE);
        DerReader tbs = new DerReader(der, list.position(), tbsLen);

        int[] field = tbs.nextTlv();
        if (field[2] == TAG_INTEGER) {
            field = tbs.nextTlv();
        }
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el algoritmo de firma");
        }
        int[] issuer = tbs.nextTlv();
        if (issuer[2] != TAG_SEQUENCE) {
            throw new IOException("DER: se esperaba el nombre del emisor");
        }
        return tbs.copy(issuer[0], issuer[1]);
    }

    // Los pares (OID, valor) de un `Name` X.500, en el orden del DER.
    //
    //   Name ::= SEQUENCE OF RelativeDistinguishedName
    //   RelativeDistinguishedName ::= SET OF AttributeTypeAndValue
    //   AttributeTypeAndValue ::= SEQUENCE { type OBJECT IDENTIFIER, value ANY }
    //
    // Existe para las dos reglas heredadas de NameConstraints: el CN se comprueba tambien como
    // nombre DNS y el EMAILADDRESS como direccion de correo. Las dos necesitan mirar adentro del
    // nombre, y `X500Principal` no lo deja -- solo entrega el texto entero.
    //
    // Un valor que no sea de un tipo de cadena se saltea en vez de romper: un atributo raro no
    // deberia impedir leer los que si se entienden.
    static java.util.List<String[]> attributesOf(byte[] nameDer) throws IOException {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        DerReader outer = new DerReader(nameDer, 0, nameDer.length);
        int nameLen = outer.expect(TAG_SEQUENCE);
        DerReader name = new DerReader(nameDer, outer.position(), nameLen);
        while (name.hasMore()) {
            int setLen = name.expect(0x31);
            DerReader rdn = new DerReader(nameDer, name.position(), setLen);
            name.skip(setLen);
            while (rdn.hasMore()) {
                int avaLen = rdn.expect(TAG_SEQUENCE);
                DerReader ava = new DerReader(nameDer, rdn.position(), avaLen);
                rdn.skip(avaLen);
                int oidLen = ava.expect(TAG_OID);
                int oidAt = ava.skip(oidLen);
                String oid = ava.readOid(oidAt, oidLen);
                int tag = ava.readTag();
                int len = ava.readLength();
                int at = ava.skip(len);
                String text = stringValue(nameDer, tag, at, len);
                if (text != null) {
                    out.add(new String[] {oid, text});
                }
            }
        }
        return out;
    }

    // El texto de un valor de atributo, o null si su etiqueta no es de una cadena.
    //
    // UTF8String va en UTF-8 y los demas en ASCII. BMPString y UniversalString --UTF-16 y UTF-32--
    // se saltean: aparecen casi solo en certificados viejos y leerlos mal daria un nombre que no es.
    private static String stringValue(byte[] buf, int tag, int at, int len) {
        if (tag == 0x0c) {
            return new String(buf, at, len, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (tag == 0x13 || tag == 0x16 || tag == 0x14 || tag == 0x12) {
            return new String(buf, at, len, java.nio.charset.StandardCharsets.US_ASCII);
        }
        return null;
    }
}
