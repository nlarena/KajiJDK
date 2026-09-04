package java.security;

import java.io.Serializable;

// Una clave criptografica, vista como algo **opaco**.
//
// Los tres metodos son todo lo que se puede preguntar sin abrir la clave, y estan elegidos para que
// una clave que vive en una tarjeta o en un HSM pueda seguir siendo una `Key`: dice de que
// algoritmo es, y —si acaso— con que formato se deja exportar. `getEncoded()` puede devolver
// **null** legitimamente, y eso no es un error: significa "esta clave no sale de aca". El codigo
// que asume que nunca es null se rompe justo con las claves mejor protegidas.
public interface Key extends Serializable {

    // El identificador de serializacion es parte del contrato publico: fija el formato con el que
    // una clave viaja entre VMs, y cambiarlo rompe todo lo ya serializado.
    long serialVersionUID = 6603384152749567654L;

    // El nombre del algoritmo: "RSA", "DSA", "AES".
    String getAlgorithm();

    // El nombre del formato de `getEncoded()` —"X.509", "PKCS#8"— o null si no se deja exportar.
    String getFormat();

    // La clave codificada, o null si no se deja exportar.
    byte[] getEncoded();
}
