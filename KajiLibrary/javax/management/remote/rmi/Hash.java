package javax.management.remote.rmi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * El numero con el que un stub de RMI nombra un metodo del otro lado.
 *
 * <h2>Por que un numero y no el nombre</h2>
 *
 * <p>Cuando un stub llama, tiene que decirle al servidor <strong>cual</strong> de los metodos de la
 * interfaz esta invocando. Mandar el nombre y la firma en texto seria caro en cada llamada y
 * ambiguo con las sobrecargas. RMI manda un numero de 64 bits que sale del nombre y del descriptor,
 * y que las dos puntas calculan igual: si difieren, es que las interfaces no son la misma version, y
 * la llamada se rechaza en vez de ir al metodo equivocado.
 *
 * <h2>Como se calcula</h2>
 *
 * <p>Es de la especificacion de RMI, no una eleccion de esta biblioteca: se escribe
 * {@code nombre(descriptor)} con {@code writeUTF} --o sea, el largo en dos bytes y despues los
 * bytes--, se le toma SHA-1, y se arman los primeros ocho bytes del resumen como un {@code long} en
 * orden de byte menos significativo primero.
 *
 * <h2>Por que se calcula aca en vez de estar escrito</h2>
 *
 * <p>En el JDK estos numeros son constantes en el archivo compilado, porque los calculo
 * {@code rmic} al construirlo. Copiarlos a mano seria copiar sesenta literales de dieciocho digitos
 * sin forma de comprobar ninguno. Calcularlos da lo mismo --se comprobo contra los del JDK 25-- y
 * ademas no se puede desincronizar de la firma si la firma cambia.
 */
final class Hash {

    private Hash() {
    }

    /**
     * El numero de ese metodo.
     *
     * @param m el metodo
     * @return el numero
     * @throws Error si esta VM no tiene SHA-1, que es lo unico que impediria calcularlo
     */
    static long de(Method m) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA-1 no disponible", e);
        }
        try {
            final DataOutputStream out = new DataOutputStream(
                    new DigestOutputStream(new ByteArrayOutputStream(), md));
            out.writeUTF(descriptor(m));
            out.flush();
        } catch (IOException e) {
            // El destino es un arreglo en memoria: no hay dispositivo que pueda fallar.
            throw new Error("no se pudo calcular el hash", e);
        }
        final byte[] h = md.digest();
        long hash = 0;
        for (int i = 0; i < 8 && i < h.length; i++) {
            hash += (long) (h[i] & 0xFF) << i * 8;
        }
        return hash;
    }

    /** {@code nombre(tiposDeLosParametros)tipoDeRetorno}, en la notacion del archivo compilado. */
    private static String descriptor(Method m) {
        final StringBuilder b = new StringBuilder(m.getName()).append('(');
        for (final Class<?> p : m.getParameterTypes()) {
            b.append(tipo(p));
        }
        return b.append(')').append(tipo(m.getReturnType())).toString();
    }

    private static String tipo(Class<?> c) {
        if (c.isArray()) {
            return "[" + tipo(c.getComponentType());
        }
        if (!c.isPrimitive()) {
            return "L" + c.getName().replace('.', '/') + ";";
        }
        if (c == boolean.class) {
            return "Z";
        }
        if (c == byte.class) {
            return "B";
        }
        if (c == char.class) {
            return "C";
        }
        if (c == short.class) {
            return "S";
        }
        if (c == int.class) {
            return "I";
        }
        if (c == long.class) {
            return "J";
        }
        if (c == float.class) {
            return "F";
        }
        if (c == double.class) {
            return "D";
        }
        return "V";
    }
}
