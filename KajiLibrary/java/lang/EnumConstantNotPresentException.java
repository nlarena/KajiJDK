package java.lang;

/**
 * KajiLibrary's java.lang.EnumConstantNotPresentException — an annotation named an enum
 * constant that the enum no longer has.
 *
 * It exists because of how annotations are stored: an enum-valued element is written into the
 * class file as a NAME, not as a reference, so the constant can be deleted after the
 * annotation was compiled. Reflection only finds out when it tries to resolve it, and this is
 * what it throws then.
 */
public class EnumConstantNotPresentException extends RuntimeException {

    private final Class<? extends Enum> enumType;

    private final String constantName;

    public EnumConstantNotPresentException(Class<? extends Enum> enumType, String constantName) {
        super(enumType.getName() + "." + constantName);
        this.enumType = enumType;
        this.constantName = constantName;
    }

    /**
     * The enum type that was missing the constant.
     */
    public Class<? extends Enum> enumType() {
        return this.enumType;
    }

    /**
     * The name the annotation asked for.
     */
    public String constantName() {
        return this.constantName;
    }
}
