package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.util.List;
import jdk.internal.classfile.impl.Instructions;

// `lookupswitch`: the values are arbitrary and go with their destination in a table the format
// requires to be kept sorted by value, because the JVM searches it binarily.
public interface LookupSwitchInstruction extends Instruction {

    /** Where whatever falls into no branch goes. */
    Label defaultTarget();

    /** The branches, sorted by value. */
    List<SwitchCase> cases();

    /** The `lookupswitch` with this default destination and these branches. */
    public static LookupSwitchInstruction of(Label defaultTarget, List<SwitchCase> cases) {
        return Instructions.lookupSwitch(defaultTarget, cases);
    }
}
