package java.lang.constant;

// The implementation `ClassDesc.of` hands back. It exists because a static method on an
// interface cannot instantiate that interface — the real JDK has the same split.
final class ConstantClassDesc implements ClassDesc {
    private final String name;

    ConstantClassDesc(String name) {
        this.name = name;
    }

    public String descriptorString() {
        return name;
    }
}
