package jakarta.persistence;

// Converts an entity attribute of type X to/from its database column representation Y.
// Implemented by an application class and named from @Convert / @Converter.
public interface AttributeConverter<X, Y> {
    Y convertToDatabaseColumn(X attribute);
    X convertToEntityAttribute(Y dbData);
}
