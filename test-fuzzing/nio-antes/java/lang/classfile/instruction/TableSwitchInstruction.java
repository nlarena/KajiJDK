package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.util.List;
import jdk.internal.classfile.impl.Instructions;

// `tableswitch`: the values are a contiguous range from `lowValue()` to `highValue()` and the table
// is an array of destinations indexed by the value minus `low`. A gap in the range is written with
// the default destination, so `cases()` may have fewer branches than `high - low + 1`.
public interface TableSwitchInstruction extends Instruction {

    /** The range's smallest value. */
    int lowValue();

    /** The range's largest value. */
    int highValue();

    /** Where whatever falls into no branch goes. */
    Label defaultTarget();

    /** The branches whose destination is not the default one. */
    List<SwitchCase> cases();

    /** The `tableswitch` with this range, this default destination and these branches. */
    public static TableSwitchInstruction of(int lowValue, int highValue, Label defaultTarget,
            List<SwitchCase> cases) {
        return Instructions.tableSwitch(lowValue, highValue, defaultTarget, cases);
    }
}
