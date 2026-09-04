package java.net;

// Una direccion de una placa de red, con su mascara y su broadcast.
//
// ===========================================================================================
// EXISTE, PERO EN ESTA VM NADIE PUEDE FABRICAR UNA
// ===========================================================================================
//
// Y eso es correcto, no un hueco tapado. En el JDK tampoco hay forma de construir una a mano: el
// constructor es de paquete, y la unica fuente es `NetworkInterface.getInterfaceAddresses()`.
// `NetworkInterface` no esta en este arbol --enumerar las placas de la maquina es una llamada al
// sistema operativo que la VM no expone; el porque completo esta en la cabecera de `InetAddress`--
// asi que la fuente no existe y la clase queda inalcanzable.
//
// Escribirla igual tiene sentido por dos razones y ninguna es el conteo:
//
//  1. **No miente nada.** Sus tres accesores devuelven lo que se le puso, y si nadie le pone nada,
//     nadie los llama. No hay un solo metodo aca que prometa una operacion que no ocurra.
//  2. Es el tipo que las firmas necesitan nombrar. El dia que haya `NetworkInterface`, esta clase
//     no cambia una linea.
//
// Lo que **no** se hace es darle un constructor publico que el JDK no tiene, para que se pueda
// "usar". Eso si seria inventar API: cambiaria el contrato para tapar la ausencia de otra clase.
//
// La longitud del prefijo es un `short` y no un `int` porque no pasa de 128 (IPv6) ni de 32 (IPv4);
// el tipo lo fija el JDK.
public final class InterfaceAddress {

    private final InetAddress address;
    private final Inet4Address broadcast;
    private final short maskLength;

    // De paquete, como en el JDK: la fabrica es `NetworkInterface`, y no hay otra.
    InterfaceAddress(InetAddress address, Inet4Address broadcast, short maskLength) {
        this.address = address;
        this.broadcast = broadcast;
        this.maskLength = maskLength;
    }

    /** La direccion IP de esta placa. */
    public InetAddress getAddress() {
        return this.address;
    }

    /**
     * La direccion de broadcast de esta subred, o null.
     *
     * <p>Null para IPv6 **siempre**, y no por falta de datos: IPv6 no tiene broadcast, usa multicast
     * en su lugar. Por eso el tipo de retorno declarado es `InetAddress` pero el valor solo puede
     * ser una `Inet4Address`.
     */
    public InetAddress getBroadcast() {
        return this.broadcast;
    }

    /** Cuantos bits de la direccion son la red: 24 para una mascara 255.255.255.0. */
    public short getNetworkPrefixLength() {
        return this.maskLength;
    }

    /** Iguales si coinciden las tres partes. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress otra = (InterfaceAddress) obj;
        if (!iguales(this.address, otra.address)) {
            return false;
        }
        if (!iguales(this.broadcast, otra.broadcast)) {
            return false;
        }
        return this.maskLength == otra.maskLength;
    }

    private static boolean iguales(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public int hashCode() {
        int h = this.maskLength;
        if (this.address != null) {
            h = h + this.address.hashCode();
        }
        if (this.broadcast != null) {
            h = h + this.broadcast.hashCode();
        }
        return h;
    }

    /** En la forma "direccion/prefijo [broadcast]", que es la del JDK. */
    @Override
    public String toString() {
        return this.address + "/" + this.maskLength + " [" + this.broadcast + "]";
    }
}
