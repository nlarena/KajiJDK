// Prueba de comportamiento de org.ietf.jgss, java.awt.image.renderable y javax.crypto.interfaces.
//
// `run()` devuelve -1 si pasa todo, o el indice del primer caso que falla. Los numeros no se
// reciclan.
//
// Lo que difiere a proposito --que `GSSManager.getInstance()` no traiga ningun mecanismo, porque
// esta biblioteca no tiene Kerberos-- esta en `runKaji()`.

import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.ietf.jgss.ChannelBinding;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.MessageProp;
import org.ietf.jgss.Oid;

public class JgssPkgTest {

    public static int run() {
        // --- org.ietf.jgss.Oid: las dos reglas de forma ---
        if (!oidRejects("1")) return 0;
        if (!oidRejects("3.1")) return 1;
        if (!oidRejects("no.es.un.oid")) return 2;
        if (!oidRejects("")) return 3;
        if (!oidRejects("1..2")) return 4;
        if (oidRejects("0.0")) return 5;
        if (oidRejects("2.5.4.3")) return 6;

        try {
            // El DER que produce: etiqueta 6, largo, y los dos primeros arcos en un solo byte.
            Oid cn = new Oid("2.5.4.3");
            byte[] der = cn.getDER();
            if (der.length != 5) return 7;
            if (der[0] != 6) return 8;
            if (der[1] != 3) return 9;
            // 2 * 40 + 5 = 85
            if (der[2] != 85) return 10;
            if (der[3] != 4) return 11;
            if (der[4] != 3) return 12;

            // Kerberos: tiene arcos que no entran en un byte, asi que se codifican en base 128.
            Oid krb = new Oid("1.2.840.113554.1.2.2");
            byte[] kder = krb.getDER();
            if (kder.length != 11) return 13;
            if (kder[0] != 6 || kder[1] != 9) return 14;
            // 1 * 40 + 2 = 42
            if (kder[2] != 42) return 15;
            // 840 = 0x348 -> 0x86 0x48
            if ((kder[3] & 0xff) != 0x86) return 16;
            if ((kder[4] & 0xff) != 0x48) return 17;

            // Ida y vuelta por los tres constructores.
            if (!new Oid(kder).equals(krb)) return 18;
            if (!new Oid(new ByteArrayInputStream(kder)).equals(krb)) return 19;
            if (!"1.2.840.113554.1.2.2".equals(krb.toString())) return 20;
            if (!new Oid(der).toString().equals("2.5.4.3")) return 21;

            // equals y hashCode van juntos.
            if (krb.equals(cn)) return 22;
            if (!krb.equals(new Oid("1.2.840.113554.1.2.2"))) return 23;
            if (krb.hashCode() != new Oid("1.2.840.113554.1.2.2").hashCode()) return 24;
            if (krb.equals("1.2.840.113554.1.2.2")) return 25;

            if (!cn.containedIn(new Oid[] {krb, cn})) return 26;
            if (cn.containedIn(new Oid[] {krb})) return 27;
            if (cn.containedIn(new Oid[0])) return 28;

            // Los tipos de nombre son OID conocidos, y sus valores son parte del estandar.
            if (!"1.2.840.113554.1.2.1.4".equals(GSSName.NT_HOSTBASED_SERVICE.toString())) return 29;
            if (!"1.2.840.113554.1.2.1.1".equals(GSSName.NT_USER_NAME.toString())) return 30;
            if (!"1.2.840.113554.1.2.1.2".equals(GSSName.NT_MACHINE_UID_NAME.toString())) return 31;
            if (!"1.2.840.113554.1.2.1.3".equals(GSSName.NT_STRING_UID_NAME.toString())) return 32;
            if (!"1.3.6.1.5.6.3".equals(GSSName.NT_ANONYMOUS.toString())) return 33;
            if (!"1.3.6.1.5.6.4".equals(GSSName.NT_EXPORT_NAME.toString())) return 34;
        } catch (GSSException e) {
            return 35;
        }

        // --- org.ietf.jgss.GSSException ---
        GSSException plain = new GSSException(GSSException.BAD_MECH);
        if (plain.getMajor() != 2) return 36;
        if (plain.getMinor() != 0) return 37;
        if (plain.getMinorString() != null) return 38;
        if (!"Unsupported mechanism requested".equals(plain.getMajorString())) return 39;
        // Sin codigo menor, el mensaje es solo el mayor.
        if (!"Unsupported mechanism requested".equals(plain.getMessage())) return 40;
        if (!"GSSException: Unsupported mechanism requested".equals(plain.toString())) return 41;

        GSSException both = new GSSException(GSSException.FAILURE, 42, "detalle");
        if (both.getMinor() != 42) return 42;
        // Con los dos, el menor va entre parentesis.
        if (!"Failure unspecified at GSS-API level (Mechanism level: detalle)"
                .equals(both.getMessage())) return 43;

        // Un codigo mayor desconocido se describe como falla sin especificar, no rompe.
        if (!"Failure unspecified at GSS-API level".equals(new GSSException(999).getMajorString())) {
            return 44;
        }
        if (!"Failure unspecified at GSS-API level".equals(new GSSException(0).getMajorString())) {
            return 45;
        }

        // setMinor asigna las dos cosas siempre, pero el que manda para el mensaje es el codigo:
        // con 0, el texto queda guardado y no sale en getMessage.
        GSSException late = new GSSException(GSSException.NO_CRED);
        late.setMinor(0, "guardado pero no mostrado");
        if (late.getMinor() != 0) return 46;
        if (!"guardado pero no mostrado".equals(late.getMinorString())) return 47;
        if (!"No valid credentials provided".equals(late.getMessage())) return 126;
        late.setMinor(7, "ahora si");
        if (late.getMinor() != 7) return 48;
        if (!"ahora si".equals(late.getMinorString())) return 49;
        // Codigo sin texto: sale igual, con el null a la vista.
        GSSException noText = new GSSException(GSSException.NO_CRED);
        noText.setMinor(9, null);
        if (!"No valid credentials provided (Mechanism level: null)".equals(noText.getMessage())) {
            return 127;
        }

        // Las constantes son parte del estandar y sus valores importan.
        if (GSSException.BAD_BINDINGS != 1) return 50;
        if (GSSException.FAILURE != 11) return 51;
        if (GSSException.UNAVAILABLE != 16) return 52;
        if (GSSException.GAP_TOKEN != 22) return 53;

        // --- org.ietf.jgss.MessageProp ---
        MessageProp asking = new MessageProp(true);
        if (asking.getQOP() != 0) return 54;
        if (!asking.getPrivacy()) return 55;
        // Los cuatro avisos arrancan apagados: nadie recibio nada todavia.
        if (asking.isDuplicateToken()) return 56;
        if (asking.isOldToken()) return 57;
        if (asking.isUnseqToken()) return 58;
        if (asking.isGapToken()) return 59;
        if (asking.getMinorStatus() != 0) return 60;
        if (asking.getMinorString() != null) return 61;

        MessageProp told = new MessageProp(7, false);
        if (told.getQOP() != 7) return 62;
        if (told.getPrivacy()) return 63;
        told.setSupplementaryStates(true, true, true, true, 5, "x");
        if (!told.isDuplicateToken()) return 64;
        if (!told.isOldToken()) return 65;
        if (!told.isUnseqToken()) return 66;
        if (!told.isGapToken()) return 67;
        if (told.getMinorStatus() != 5) return 68;
        if (!"x".equals(told.getMinorString())) return 69;
        told.setQOP(2);
        told.setPrivacy(true);
        if (told.getQOP() != 2) return 70;
        if (!told.getPrivacy()) return 71;

        // --- org.ietf.jgss.ChannelBinding ---
        ChannelBinding cb = new ChannelBinding(new byte[] {1, 2});
        if (cb.getInitiatorAddress() != null) return 72;
        if (cb.getAcceptorAddress() != null) return 73;
        byte[] app = cb.getApplicationData();
        if (app == null || app.length != 2 || app[0] != 1 || app[1] != 2) return 74;
        // Igualdad por contenido, no por identidad del arreglo.
        if (!cb.equals(new ChannelBinding(new byte[] {1, 2}))) return 75;
        if (cb.equals(new ChannelBinding(new byte[] {1, 3}))) return 76;
        if (cb.equals(new ChannelBinding(new byte[] {1}))) return 77;
        if (cb.hashCode() != new ChannelBinding(new byte[] {1, 2}).hashCode()) return 78;
        if (cb.equals("no soy un binding")) return 79;
        if (new ChannelBinding(null).getApplicationData() != null) return 80;

        // --- java.awt.image.renderable.ParameterBlock ---
        ParameterBlock pb = new ParameterBlock();
        if (pb.getNumSources() != 0) return 81;
        if (pb.getNumParameters() != 0) return 82;
        // Los add encadenan.
        pb.add(1).add(2.5f).add("texto");
        if (pb.getNumParameters() != 3) return 83;
        if (pb.getIntParameter(0) != 1) return 84;
        if (pb.getFloatParameter(1) != 2.5f) return 85;
        if (!"texto".equals(pb.getObjectParameter(2))) return 86;
        // Los get tipados no convierten: un int guardado no se lee como long.
        try {
            pb.getLongParameter(0);
            return 87;
        } catch (ClassCastException expected) {
            // asi tiene que ser
        }
        // getParamClasses devuelve la clase primitiva, no la del envoltorio.
        Class<?>[] classes = pb.getParamClasses();
        if (classes.length != 3) return 88;
        if (classes[0] != int.class) return 89;
        if (classes[1] != float.class) return 90;
        if (classes[2] != String.class) return 91;

        // set mas alla del final agranda y deja huecos en null.
        ParameterBlock sparse = new ParameterBlock();
        sparse.set(9, 3);
        if (sparse.getNumParameters() != 4) return 92;
        if (sparse.getObjectParameter(0) != null) return 93;
        if (sparse.getIntParameter(3) != 9) return 94;
        // Un null no tiene clase: getParamClasses lo choca en vez de devolver null adentro.
        try {
            sparse.getParamClasses();
            return 95;
        } catch (NullPointerException expected) {
            // asi tiene que ser
        }

        // Las fuentes van en su propia lista.
        ParameterBlock withSources = new ParameterBlock();
        withSources.addSource("fuente0").addSource("fuente1");
        if (withSources.getNumSources() != 2) return 96;
        if (!"fuente1".equals(withSources.getSource(1))) return 97;
        withSources.setSource("otra", 5);
        if (withSources.getNumSources() != 6) return 98;
        if (withSources.getSource(3) != null) return 99;
        withSources.removeSources();
        if (withSources.getNumSources() != 0) return 100;

        // clone copia las listas; shallowClone las comparte.
        ParameterBlock original = new ParameterBlock();
        original.add(1);
        ParameterBlock deep = (ParameterBlock) original.clone();
        deep.add(2);
        if (original.getNumParameters() != 1) return 101;
        if (deep.getNumParameters() != 2) return 102;
        ParameterBlock shallow = (ParameterBlock) original.shallowClone();
        shallow.add(3);
        if (original.getNumParameters() != 2) return 103;

        // Los constructores con Vector toman las listas que se les dan.
        Vector<Object> sources = new Vector<Object>();
        sources.addElement("s");
        Vector<Object> params = new Vector<Object>();
        params.addElement("p");
        ParameterBlock fromVectors = new ParameterBlock(sources, params);
        if (fromVectors.getNumSources() != 1) return 104;
        if (fromVectors.getNumParameters() != 1) return 105;

        // --- java.awt.image.renderable.RenderContext ---
        RenderContext rc = new RenderContext(AffineTransform.getScaleInstance(2, 2));
        if (rc.getAreaOfInterest() != null) return 106;
        if (rc.getRenderingHints() != null) return 107;
        if (rc.getTransform().getScaleX() != 2.0) return 108;
        // El getter devuelve una copia: moverla no mueve la del contexto.
        AffineTransform copy = rc.getTransform();
        copy.scale(10, 10);
        if (rc.getTransform().getScaleX() != 2.0) return 109;
        // Componer no conmuta, y por eso hay dos metodos.
        RenderContext post = new RenderContext(AffineTransform.getScaleInstance(2, 2));
        post.concatenateTransform(AffineTransform.getTranslateInstance(3, 0));
        RenderContext pre = new RenderContext(AffineTransform.getScaleInstance(2, 2));
        pre.preConcatenateTransform(AffineTransform.getTranslateInstance(3, 0));
        if (post.getTransform().getTranslateX() == pre.getTransform().getTranslateX()) return 110;
        // Los nombres mal escritos hacen exactamente lo mismo que los correctos.
        RenderContext typo = new RenderContext(AffineTransform.getScaleInstance(2, 2));
        typo.concetenateTransform(AffineTransform.getTranslateInstance(3, 0));
        if (typo.getTransform().getTranslateX() != post.getTransform().getTranslateX()) return 111;
        RenderContext typoPre = new RenderContext(AffineTransform.getScaleInstance(2, 2));
        typoPre.preConcetenateTransform(AffineTransform.getTranslateInstance(3, 0));
        if (typoPre.getTransform().getTranslateX() != pre.getTransform().getTranslateX()) return 112;
        // Un contexto sin transformacion no existe: no hay resolucion que deducir.
        try {
            new RenderContext(null);
            return 113;
        } catch (NullPointerException expected) {
            // asi tiene que ser
        }
        RenderContext cloned = (RenderContext) post.clone();
        if (cloned.getTransform().getTranslateX() != post.getTransform().getTranslateX()) return 114;

        return -1;
    }

    /**
     * Los casos que <b>no</b> corren en el JDK.
     *
     * <p>Alla {@code GSSManager.getInstance()} trae Kerberos y aca no hay ningun mecanismo. Las
     * respuestas de este lado son las declaradas para "no hay" --arreglo vacio donde vacio es la
     * verdad, {@code UNAVAILABLE} y {@code BAD_MECH} donde no hay nada que devolver-- pero el JDK
     * contesta otra cosa porque de verdad tiene mecanismos.
     *
     * @return el indice del primer caso que falla, o -1
     */
    public static int runKaji() {
        GSSManager manager = GSSManager.getInstance();
        if (manager == null) return 115;
        if (manager.getMechs().length != 0) return 116;
        if (manager.getMechsForName(GSSName.NT_USER_NAME).length != 0) return 117;
        try {
            manager.getNamesForMech(GSSName.NT_USER_NAME);
            return 118;
        } catch (GSSException e) {
            if (e.getMajor() != GSSException.BAD_MECH) return 119;
        }
        try {
            manager.createName("alguien", GSSName.NT_USER_NAME);
            return 120;
        } catch (GSSException e) {
            if (e.getMajor() != GSSException.UNAVAILABLE) return 121;
        }
        try {
            manager.createCredential(0);
            return 122;
        } catch (GSSException e) {
            if (e.getMajor() != GSSException.UNAVAILABLE) return 123;
        }
        try {
            manager.createContext(new byte[0]);
            return 124;
        } catch (GSSException e) {
            if (e.getMajor() != GSSException.UNAVAILABLE) return 125;
        }
        return -1;
    }

    /** Si esa cadena no es un OID valido. */
    static boolean oidRejects(String text) {
        try {
            new Oid(text);
            return false;
        } catch (GSSException expected) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
