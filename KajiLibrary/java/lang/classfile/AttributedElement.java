package java.lang.classfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Something carrying attributes: a class, a field, a method or a method body.
public interface AttributedElement extends ClassFileElement {

    /** Every attribute, in file order. */
    List<Attribute<?>> attributes();

    /** The first attribute of this mapper, if there is one. */
    default <T extends Attribute<T>> Optional<T> findAttribute(AttributeMapper<T> attr) {
        List<Attribute<?>> all = attributes();
        for (int i = 0; i < all.size(); i++) {
            Attribute<?> a = all.get(i);
            if (a.attributeMapper() == attr) {
                return Optional.of((T) a);
            }
        }
        return Optional.empty();
    }

    /** Every attribute of this mapper, in file order. */
    default <T extends Attribute<T>> List<T> findAttributes(AttributeMapper<T> attr) {
        List<T> found = new ArrayList<T>();
        List<Attribute<?>> all = attributes();
        for (int i = 0; i < all.size(); i++) {
            Attribute<?> a = all.get(i);
            if (a.attributeMapper() == attr) {
                found.add((T) a);
            }
        }
        return found;
    }
}
