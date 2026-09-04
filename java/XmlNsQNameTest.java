// `javax.xml.namespace.QName` comprobado contra `java` real.
//
// Esta prueba se corre con las dos VMs y compara: en la nuestra usa el `QName` de KajiLibrary, y en
// `H:/jdk-25.0.2/bin/java.exe -cp java` usa el del modulo `java.xml`, porque el paquete es de la
// plataforma y el classpath no lo puede tapar. O sea que el mismo codigo mide las dos
// implementaciones sin cambiar una linea, que es justo lo que hace falta para una clase que es pura
// funcion.
//
// Lo que se cuida:
//
// **El prefijo no entra en `equals` ni en `hashCode`.** Es la regla de la clase y la que mas
// sorprende. Un `QName` con prefijo "a" y otro con prefijo "b", mismo URI y mismo local, son el
// mismo nombre --y ademas tienen que dar el mismo hash, o un `HashMap` los perderia--. Las
// comprobaciones 5 a 8.
//
// **`valueOf` de `{}local` falla.** Es el unico borde que sorprende de verdad: pedir explicitamente
// el espacio de nombres vacio es un error, porque la forma de decir eso es `local` pelado. Una llave
// que cierra sin abrir, en cambio, es nombre local y no error. Comprobaciones 14 a 18.
//
// **La ida y vuelta pierde el prefijo.** `valueOf(q.toString())` no reconstruye el prefijo, y esa
// perdida es intencional y parte del contrato. Comprobacion 13.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import javax.xml.namespace.QName;

public class XmlNsQNameTest {

    static int cuantas = 0;

    static int chequear(boolean condicion) {
        cuantas++;
        return condicion ? 0 : cuantas;
    }

    public static int run() {
        int r;

        // --- Construccion y accesores -------------------------------------------------------
        QName soloLocal = new QName("precio");
        if ((r = chequear("".equals(soloLocal.getNamespaceURI()))) != 0) return r;      // 1
        if ((r = chequear("precio".equals(soloLocal.getLocalPart()))) != 0) return r;   // 2
        if ((r = chequear("".equals(soloLocal.getPrefix()))) != 0) return r;            // 3

        // El URI null se normaliza a la cadena vacia; el local y el prefijo null revientan.
        if ((r = chequear("".equals(new QName(null, "x").getNamespaceURI()))) != 0) return r; // 4

        // --- El prefijo no es identidad ------------------------------------------------------
        QName conA = new QName("http://tienda", "precio", "a");
        QName conB = new QName("http://tienda", "precio", "b");
        if ((r = chequear(conA.equals(conB))) != 0) return r;                           // 5
        if ((r = chequear(conB.equals(conA))) != 0) return r;                           // 6
        if ((r = chequear(conA.hashCode() == conB.hashCode())) != 0) return r;          // 7
        if ((r = chequear("a".equals(conA.getPrefix())
                       && "b".equals(conB.getPrefix()))) != 0) return r;                // 8

        // El URI y el local si son identidad.
        if ((r = chequear(!conA.equals(new QName("http://otra", "precio", "a")))) != 0) return r; // 9
        if ((r = chequear(!conA.equals(new QName("http://tienda", "otro", "a")))) != 0) return r; // 10
        if ((r = chequear(!conA.equals("http://tienda"))) != 0) return r;               // 11

        // --- toString / valueOf --------------------------------------------------------------
        if ((r = chequear("{http://tienda}precio".equals(conA.toString()))) != 0) return r; // 12
        QName vuelta = QName.valueOf(conA.toString());
        if ((r = chequear(vuelta.equals(conA)
                       && "".equals(vuelta.getPrefix()))) != 0) return r;                // 13
        if ((r = chequear("precio".equals(soloLocal.toString()))) != 0) return r;        // 14

        // Sin llaves: todo es nombre local.
        QName pelado = QName.valueOf("precio");
        if ((r = chequear("".equals(pelado.getNamespaceURI())
                       && "precio".equals(pelado.getLocalPart()))) != 0) return r;       // 15

        // La cadena vacia da un nombre local vacio, no un error.
        if ((r = chequear("".equals(QName.valueOf("").getLocalPart()))) != 0) return r;   // 16

        // `{}local` es el borde que sorprende: falla.
        boolean rebotoLlavesVacias = false;
        try {
            QName.valueOf("{}precio");
        } catch (IllegalArgumentException e) {
            rebotoLlavesVacias = true;
        }
        if ((r = chequear(rebotoLlavesVacias)) != 0) return r;                           // 17

        // Llave que abre y no cierra: falla.
        boolean rebotoSinCierre = false;
        try {
            QName.valueOf("{http://tienda");
        } catch (IllegalArgumentException e) {
            rebotoSinCierre = true;
        }
        if ((r = chequear(rebotoSinCierre)) != 0) return r;                              // 18

        // Llave que cierra sin abrir: NO falla, es nombre local.
        QName cierraSola = QName.valueOf("}raro");
        if ((r = chequear("}raro".equals(cierraSola.getLocalPart())
                       && "".equals(cierraSola.getNamespaceURI()))) != 0) return r;      // 19

        // valueOf(null) falla.
        boolean rebotoNulo = false;
        try {
            QName.valueOf(null);
        } catch (IllegalArgumentException e) {
            rebotoNulo = true;
        }
        if ((r = chequear(rebotoNulo)) != 0) return r;                                   // 20

        // --- Las validaciones del constructor -------------------------------------------------
        boolean rebotoLocalNulo = false;
        try {
            new QName("http://x", null);
        } catch (IllegalArgumentException e) {
            rebotoLocalNulo = true;
        }
        if ((r = chequear(rebotoLocalNulo)) != 0) return r;                              // 21

        boolean rebotoPrefijoNulo = false;
        try {
            new QName("http://x", "y", null);
        } catch (IllegalArgumentException e) {
            rebotoPrefijoNulo = true;
        }
        if ((r = chequear(rebotoPrefijoNulo)) != 0) return r;                            // 22

        // --- El hash es el xor de los dos que importan ----------------------------------------
        if ((r = chequear(conA.hashCode()
                       == ("http://tienda".hashCode() ^ "precio".hashCode()))) != 0) return r; // 23

        // Un nombre sin espacio de nombres hashea solo por el local.
        if ((r = chequear(soloLocal.hashCode() == ("".hashCode() ^ "precio".hashCode()))) != 0)
            return r;                                                                    // 24

        return -1;
    }

    public static void main(String[] args) {
        System.out.println("XmlNsQNameTest -> " + run() + " (de " + cuantas + ")");
    }
}
