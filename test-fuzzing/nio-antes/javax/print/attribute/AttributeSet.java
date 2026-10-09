package javax.print.attribute;

// KajiLibrary's javax.print.attribute.AttributeSet -- a set of attributes indexed by **category**.
//
// The rule that defines the type, and that shows little in the signature: the set keeps at most
// **one** attribute per category. `add` of an attribute whose category is already there replaces
// the one there was instead of adding another. That is why `get`, `remove` and `containsKey` take a
// `Class` (the category) and `containsValue` takes an `Attribute` (the value): they are two
// different axes.
//
// It does not extend `java.util.Collection`, although it looks like one: the key is the category
// and the value the attribute, so it behaves like a `Map` with the key tucked inside the value.
public interface AttributeSet {

    // The attribute of that category, or null if there is none.
    Attribute get(Class<?> category);

    // Adds the attribute, replacing whatever there was of the same category. Returns true if the
    // set changed -- that is, if there was not already an equal attribute in that category.
    boolean add(Attribute attribute);

    // Removes the attribute of that category, if there is one. true if the set changed.
    boolean remove(Class<?> category);

    // Removes the attribute, if it is there. true if the set changed.
    boolean remove(Attribute attribute);

    // Whether there is any attribute of that category.
    boolean containsKey(Class<?> category);

    // Whether that exact attribute (by equals) is in the set.
    boolean containsValue(Attribute attribute);

    // Adds all of `attributes`, with the same replacement-by-category rule.
    boolean addAll(AttributeSet attributes);

    // How many categories there are -- which is the same as how many attributes, by the rule above.
    int size();

    // The attributes, in a new array. The order is unspecified.
    Attribute[] toArray();

    void clear();

    boolean isEmpty();

    // Two sets are equal if they have the same attributes. It is redeclared here, as in the JDK,
    // because the contract is stronger than Object's.
    boolean equals(Object object);

    int hashCode();
}
