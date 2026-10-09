package java.lang.module;

// KajiLibrary's java.lang.module.InvalidModuleDescriptorException -- thrown when a binary
// `module-info` is found but is malformed: bad magic, a constant pool entry of the wrong tag, a
// Module attribute that contradicts itself. Raised by {@link ModuleDescriptor#read}.
public class InvalidModuleDescriptorException extends RuntimeException {

    public InvalidModuleDescriptorException() {
    }

    public InvalidModuleDescriptorException(String msg) {
        super(msg);
    }
}
