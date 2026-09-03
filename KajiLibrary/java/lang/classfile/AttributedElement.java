package java.lang.classfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Algo que lleva atributos: una clase, un campo, un método o un cuerpo de método.
public interface AttributedElement extends ClassFileElement {

    /** Todos los atributos, en el orden del archivo. */
    List<Attribute<?>> attributes();

    /** El primer atributo de este mapeador, si hay alguno. */
    default <T extends Attribute<T>> Optional<T> findAttribute(AttributeMapper<T> attr) {
        List<Attribute<?>> todos = attributes();
        for (int i = 0; i < todos.size(); i++) {
            Attribute<?> a = todos.get(i);
            if (a.attributeMapper() == attr) {
                return Optional.of((T) a);
            }
        }
        return Optional.empty();
    }

    /** Todos los atributos de este mapeador, en el orden del archivo. */
    default <T extends Attribute<T>> List<T> findAttributes(AttributeMapper<T> attr) {
        List<T> encontrados = new ArrayList<T>();
        List<Attribute<?>> todos = attributes();
        for (int i = 0; i < todos.size(); i++) {
            Attribute<?> a = todos.get(i);
            if (a.attributeMapper() == attr) {
                encontrados.add((T) a);
            }
        }
        return encontrados;
    }
}
