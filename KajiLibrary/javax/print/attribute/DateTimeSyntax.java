package javax.print.attribute;

import java.io.Serializable;
import java.util.Date;

// La clase de sintaxis de los atributos cuyo valor es un instante.
//
// `java.util.Date` es mutable, y esta clase la envuelve **a medias**: copia al salir pero no al
// entrar. Es el comportamiento del JDK y se replica tal cual porque es observable, pero conviene
// verlo de frente porque parece un descuido y lo es:
//
//     Date d = new Date(1000);
//     X x = new X(d);          // se guarda LA MISMA Date, no una copia
//     d.setTime(2000);         // ...asi que esto le cambia el valor al atributo ya construido
//
// `getValue()` si devuelve una copia, de modo que el agujero es de una sola direccion: quien
// construyo el atributo puede cambiarlo por atras, quien solo lo lee no. La mitad que esta es la
// que protege al atributo de sus lectores; la que falta es la que lo protegeria de su creador.
//
// Se penso en cerrarlo copiando tambien en el constructor y se descarto: cambiaria el
// comportamiento frente al JDK en un caso que un programa puede notar, y este paquete es una
// reimplementacion, no una correccion.
public abstract class DateTimeSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -1400819079791208582L;

    private Date value;

    // Se guarda la referencia, no una copia. Ver la cabecera.
    protected DateTimeSyntax(Date value) {
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        this.value = value;
    }

    // Aca si hay copia: una Date nueva con el mismo instante. El JDK escribe exactamente esto --no
    // un `clone()`--, asi que no hace falta que nuestra java.util.Date exponga clone.
    public Date getValue() {
        return new Date(this.value.getTime());
    }

    public boolean equals(Object object) {
        if (!(object instanceof DateTimeSyntax)) {
            return false;
        }
        return this.value.equals(((DateTimeSyntax) object).value);
    }

    public int hashCode() {
        return this.value.hashCode();
    }

    public String toString() {
        return this.value.toString();
    }
}
