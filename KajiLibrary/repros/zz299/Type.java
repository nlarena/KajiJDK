package zz299;

// Un homonimo de java.lang.reflect.Type, en su propio paquete. Es el que tiene que ganar el
// nombre simple `Type` desde adentro de zz299 (§6.5.5.1).
public interface Type<X> {

    String nombre();
}
