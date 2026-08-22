package java.lang;

// KajiLibrary's java.lang.TypeNotPresentException — a type named in a signature or an annotation
// was not found when reflection tried to resolve it. It is unchecked on purpose: reading a
// method's parameter types feels like reading data, and forcing every such call to declare a
// checked exception would be intolerable. The name is kept as a String because there is, by
// definition, no Class object to hand back.
public class TypeNotPresentException extends RuntimeException {

    private String typeName;

    public TypeNotPresentException(String typeName, Throwable cause) {
        super(message(typeName), cause);
        this.typeName = typeName;
    }

    public String typeName() {
        return this.typeName;
    }

    private static String message(String typeName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type ");
        sb.append(typeName);
        sb.append(" not present");
        return sb.toString();
    }
}
