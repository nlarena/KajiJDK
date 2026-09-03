package javax.naming;

/**
 * Una direccion de comunicacion, etiquetada con **de que tipo de direccion se trata**.
 *
 * <h2>Por que una direccion no es una cadena</h2>
 *
 * <p>Un objeto puede ser alcanzable de varias maneras a la vez: el mismo servicio tiene una URL,
 * un puerto RPC y un buzon de mensajes. Guardarlas como cadenas sueltas obliga a adivinar cual es
 * cual por su forma, que es exactamente lo que se rompe cuando aparece la cuarta. Aca cada
 * direccion viene con su tipo --`"URL"`, `"ORB"`, `"LinkAddress"`-- y `Reference.get(String)`
 * pide por tipo. El tipo es un `String` y no un `enum` porque el conjunto lo define cada
 * proveedor, y ninguno los conoce todos.
 *
 * <p>Es abstracta y el contenido lo pone la subclase, porque una direccion puede ser texto
 * (`StringRefAddr`) o bytes opacos que solo entiende el proveedor (`BinaryRefAddr`). Esas dos son
 * las unicas del paquete, pero la clase esta pensada para que un proveedor agregue las suyas.
 *
 * <h2>La igualdad</h2>
 *
 * <p>Dos direcciones son iguales si coinciden **el tipo y el contenido**. Aca se compara el
 * contenido con `equals`, que es lo correcto para `String` pero no para arreglos; por eso
 * `BinaryRefAddr` redefine `equals` y `hashCode` para comparar byte a byte.
 *
 * <p>Sobre `Serializable`: el contrato dice que el contenido tiene que serlo tambien, y no se
 * puede chequear desde aca. Este arbol no tiene `ObjectOutputStream`, asi que la declaracion es un
 * contrato sin quien lo ejercite; el `serialVersionUID` es el del JDK real para que el dia que
 * exista un flujo la forma coincida.
 */
public abstract class RefAddr implements java.io.Serializable {

    private static final long serialVersionUID = -1468165120479475415L;

    /** De que clase de direccion se trata. Es `protected` porque la subclase lo lee para su `toString`. */
    protected String addrType;

    protected RefAddr(String addrType) {
        this.addrType = addrType;
    }

    public String getType() {
        return addrType;
    }

    public abstract Object getContent();

    /** Tipo y contenido. El contenido se compara con `equals`, no por identidad, salvo si es el mismo. */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RefAddr) {
            RefAddr target = (RefAddr) obj;
            if (addrType.compareTo(target.addrType) == 0) {
                Object thisobj = this.getContent();
                Object thatobj = target.getContent();
                if (thisobj == thatobj) {
                    return true;
                }
                if (thisobj != null) {
                    return thisobj.equals(thatobj);
                }
            }
        }
        return false;
    }

    /** Suma en vez de mezcla, para que un contenido nulo no cambie el hash del tipo. */
    @Override
    public int hashCode() {
        return (getContent() == null)
            ? addrType.hashCode()
            : addrType.hashCode() + getContent().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Type: ");
        str.append(addrType).append("\n");
        str.append("Content: ").append(getContent()).append("\n");
        return str.toString();
    }
}
