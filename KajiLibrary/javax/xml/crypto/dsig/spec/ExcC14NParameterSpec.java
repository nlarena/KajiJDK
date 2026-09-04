package javax.xml.crypto.dsig.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.ExcC14NParameterSpec -- que prefijos arrastra una
 * canonicalizacion exclusiva.
 *
 * <p>La canonicalizacion <b>exclusiva</b> existe para que un fragmento firmado siga validando si se
 * lo mueve a otro documento. La inclusiva arrastra todas las declaraciones de espacio de nombres que
 * estan en el contexto, aunque el fragmento no las use; la exclusiva arrastra solo las que usa. Por
 * eso mover el fragmento no cambia su forma canonica, y la firma sigue cerrando.
 *
 * <p>Esta lista es la excepcion a esa regla: los prefijos que hay que arrastrar <b>igual</b>, aunque
 * no se usen en los nombres. Hace falta cuando un prefijo aparece adentro del <b>contenido</b> --en
 * un atributo, en un valor de XPath-- donde la canonicalizacion no lo ve como uso.
 *
 * <p>Olvidarse uno es la causa clasica de una firma que valida donde se creo y falla en el
 * destinatario.
 *
 * <p>{@link #DEFAULT} nombra al prefijo por omision, que no tiene nombre y por eso necesita un
 * marcador.
 */
public final class ExcC14NParameterSpec implements C14NMethodParameterSpec {

    /** El prefijo por omision, que no tiene nombre propio. */
    public static final String DEFAULT = "#default";

    /** Los prefijos a arrastrar; nunca null, y no modificable. */
    private final List<String> prefixList;

    /** Sin prefijos extra: solo se arrastra lo que se usa. */
    public ExcC14NParameterSpec() {
        this.prefixList = Collections.emptyList();
    }

    /**
     * Con la lista de prefijos a arrastrar.
     *
     * <p>Se copia: la lista que se pasa puede cambiar despues y esto tiene que quedar fijo.
     *
     * @throws NullPointerException si la lista es null
     * @throws ClassCastException si algun elemento no es un {@code String}
     */
    public ExcC14NParameterSpec(List<String> prefixList) {
        if (prefixList == null) {
            throw new NullPointerException("prefixList cannot be null");
        }
        List<String> copy = new ArrayList<String>();
        int i = 0;
        while (i < prefixList.size()) {
            Object p = prefixList.get(i);
            if (!(p instanceof String)) {
                throw new ClassCastException("not a String: " + p);
            }
            copy.add((String) p);
            i = i + 1;
        }
        this.prefixList = Collections.unmodifiableList(copy);
    }

    /** Los prefijos a arrastrar. No modificable. */
    public List<String> getPrefixList() {
        return this.prefixList;
    }
}
