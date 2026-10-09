package jdk.internal.classfile.impl;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConvertInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * It computes the `max_stack` of a method by walking its flow graph.
 *
 * <p>It is needed because the `Code` carries it written and the JVM checks it: a value smaller than
 * the real depth makes the class not verify. A bigger one verifies all the same and wastes frame,
 * so **when something cannot be decided the upper bound is taken**, not the lower.
 *
 * <h2>Why a walk and not a sum</h2>
 *
 * <p>The depth at an instruction is not the sum of the effects of the previous ones in array order:
 * it depends on where it was reached from. A backward `goto`, a `catch` that comes in with the
 * stack at one, the two branches of an `if` -- all of that makes the textual order not the
 * execution order. The walk visits each instruction with the entry depth of each path and keeps the
 * largest.
 *
 * <p>The method's entry starts at zero; **that of each exception handler starts at one**, because
 * the JVM leaves the exception on the stack for it. Forgetting that gives a `max_stack` that fails
 * exactly in the methods with `try`.
 */
final class StackCounter {

    private StackCounter() {
    }

    /**
     * The `max_stack` of the code made of those instructions.
     *
     * @param elements the instructions, in order
     * @param handlers the handlers, whose entry starts with one on the stack
     * @param labelIndex for each label, the index of the instruction it points at
     */
    static int maxStack(List<Instruction> elements, List<ExceptionCatch> handlers,
            Map<Label, Integer> labelIndex) {
        int n = elements.size();
        if (n == 0) {
            return 0;
        }
        int[] depth = new int[n];
        boolean[] seen = new boolean[n];
        List<Integer> queue = new ArrayList<Integer>();

        StackCounter.seed(0, 0, depth, seen, queue);
        for (int i = 0; i < handlers.size(); i++) {
            Integer at = labelIndex.get(handlers.get(i).handler());
            if (at != null) {
                // One, not zero: the JVM enters the handler with the exception already pushed.
                StackCounter.seed(at.intValue(), 1, depth, seen, queue);
            }
        }

        int max = 0;
        while (!queue.isEmpty()) {
            int i = queue.remove(queue.size() - 1).intValue();
            int d = depth[i];
            if (d > max) {
                max = d;
            }
            Instruction ins = elements.get(i);
            int after = d + StackCounter.effect(ins);
            if (after < 0) {
                // A negative stack means the code is malformed. The count is not cut short
                // --computing a `max_stack` is not verifying-- but an absurdity is not propagated
                // either.
                after = 0;
            }
            if (after > max) {
                max = after;
            }
            StackCounter.successors(ins, i, after, depth, seen, queue, labelIndex, n);
        }
        return max;
    }

    private static void seed(int at, int d, int[] depth, boolean[] seen, List<Integer> queue) {
        if (at < 0 || at >= depth.length) {
            return;
        }
        // If it was already visited with a depth at least as large, there is nothing new to
        // propagate.
        if (seen[at] && depth[at] >= d) {
            return;
        }
        depth[at] = seen[at] ? Math.max(depth[at], d) : d;
        seen[at] = true;
        queue.add(Integer.valueOf(at));
    }

    private static void successors(Instruction ins, int i, int after, int[] depth, boolean[] seen,
            List<Integer> queue, Map<Label, Integer> labelIndex, int n) {
        Opcode op = ins.opcode();
        Opcode.Kind k = op.kind();

        if (k == Opcode.Kind.RETURN || k == Opcode.Kind.THROW_EXCEPTION) {
            return; // it does not fall through
        }
        if (k == Opcode.Kind.BRANCH) {
            Label t = ((BranchInstruction) ins).target();
            Integer at = labelIndex.get(t);
            if (at != null) {
                StackCounter.seed(at.intValue(), after, depth, seen, queue);
            }
            if (op != Opcode.GOTO && op != Opcode.GOTO_W) {
                StackCounter.seed(i + 1, after, depth, seen, queue);
            }
            return;
        }
        if (k == Opcode.Kind.TABLE_SWITCH) {
            TableSwitchInstruction ts = (TableSwitchInstruction) ins;
            StackCounter.seedLabel(ts.defaultTarget(), after, depth, seen, queue, labelIndex);
            List<SwitchCase> cs = ts.cases();
            for (int j = 0; j < cs.size(); j++) {
                StackCounter.seedLabel(cs.get(j).target(), after, depth, seen, queue, labelIndex);
            }
            return;
        }
        if (k == Opcode.Kind.LOOKUP_SWITCH) {
            LookupSwitchInstruction ls = (LookupSwitchInstruction) ins;
            StackCounter.seedLabel(ls.defaultTarget(), after, depth, seen, queue, labelIndex);
            List<SwitchCase> cs = ls.cases();
            for (int j = 0; j < cs.size(); j++) {
                StackCounter.seedLabel(cs.get(j).target(), after, depth, seen, queue, labelIndex);
            }
            return;
        }
        StackCounter.seed(i + 1, after, depth, seen, queue);
    }

    private static void seedLabel(Label l, int d, int[] depth, boolean[] seen, List<Integer> queue,
            Map<Label, Integer> labelIndex) {
        Integer at = labelIndex.get(l);
        if (at != null) {
            StackCounter.seed(at.intValue(), d, depth, seen, queue);
        }
    }

    /**
     * How much the stack grows (or shrinks) with that instruction, in **slots**.
     *
     * <p>A `long` and a `double` count two, which is what they take. It is the reason this is not a
     * bare table of opcodes: the effect of an `invokevirtual` depends on its descriptor, and that
     * of a `getfield` on the type of the field.
     */
    private static int effect(Instruction ins) {
        Opcode op = ins.opcode();
        Opcode.Kind k = op.kind();

        if (k == Opcode.Kind.LOAD) {
            return ((LoadInstruction) ins).typeKind().slotSize();
        }
        if (k == Opcode.Kind.STORE) {
            return -((StoreInstruction) ins).typeKind().slotSize();
        }
        if (k == Opcode.Kind.CONSTANT) {
            return ((ConstantInstruction) ins).typeKind().slotSize();
        }
        if (k == Opcode.Kind.ARRAY_LOAD) {
            // The array and the index go out, the element comes in.
            return -2 + ((ArrayLoadInstruction) ins).typeKind().slotSize();
        }
        if (k == Opcode.Kind.ARRAY_STORE) {
            return -2 - ((ArrayStoreInstruction) ins).typeKind().slotSize();
        }
        if (k == Opcode.Kind.CONVERT) {
            ConvertInstruction c = (ConvertInstruction) ins;
            return c.toType().slotSize() - c.fromType().slotSize();
        }
        if (k == Opcode.Kind.FIELD_ACCESS) {
            FieldInstruction f = (FieldInstruction) ins;
            int slots = TypeKind.fromDescriptor(f.type().stringValue()).slotSize();
            if (op == Opcode.GETSTATIC) {
                return slots;
            }
            if (op == Opcode.PUTSTATIC) {
                return -slots;
            }
            if (op == Opcode.GETFIELD) {
                return slots - 1;
            }
            return -slots - 1; // putfield: the object goes out and the value goes out
        }
        if (k == Opcode.Kind.INVOKE) {
            InvokeInstruction inv = (InvokeInstruction) ins;
            int e = StackCounter.descriptorEffect(inv.type().stringValue());
            if (op != Opcode.INVOKESTATIC) {
                e = e - 1; // the receiver
            }
            return e;
        }
        if (k == Opcode.Kind.INVOKE_DYNAMIC) {
            return StackCounter.descriptorEffect(
                    ((InvokeDynamicInstruction) ins).type().stringValue());
        }
        if (k == Opcode.Kind.NEW_OBJECT) {
            return 1;
        }
        if (k == Opcode.Kind.NEW_PRIMITIVE_ARRAY || k == Opcode.Kind.NEW_REF_ARRAY) {
            return 0; // the length goes out, the array comes in
        }
        if (k == Opcode.Kind.NEW_MULTI_ARRAY) {
            return 1 - ((NewMultiArrayInstruction) ins).dimensions();
        }
        if (k == Opcode.Kind.TYPE_CHECK) {
            return 0; // checkcast leaves the same; instanceof changes the type but not the height
        }
        if (k == Opcode.Kind.MONITOR) {
            return -1;
        }
        if (k == Opcode.Kind.INCREMENT || k == Opcode.Kind.NOP) {
            return 0;
        }
        if (k == Opcode.Kind.RETURN) {
            TypeKind t = ((java.lang.classfile.instruction.ReturnInstruction) ins).typeKind();
            return -t.slotSize();
        }
        if (k == Opcode.Kind.THROW_EXCEPTION) {
            return -1;
        }
        if (k == Opcode.Kind.BRANCH) {
            // The one-operand ones pop one; the two-operand ones, two; `goto` pops nothing.
            if (op == Opcode.GOTO || op == Opcode.GOTO_W) {
                return 0;
            }
            if (op == Opcode.IF_ICMPEQ || op == Opcode.IF_ICMPNE || op == Opcode.IF_ICMPLT
                    || op == Opcode.IF_ICMPGE || op == Opcode.IF_ICMPGT || op == Opcode.IF_ICMPLE
                    || op == Opcode.IF_ACMPEQ || op == Opcode.IF_ACMPNE) {
                return -2;
            }
            return -1;
        }
        if (k == Opcode.Kind.TABLE_SWITCH || k == Opcode.Kind.LOOKUP_SWITCH) {
            return -1;
        }
        if (k == Opcode.Kind.STACK) {
            return StackCounter.stackEffect(op);
        }
        if (k == Opcode.Kind.OPERATOR) {
            return StackCounter.operatorEffect(op);
        }
        if (k == Opcode.Kind.DISCONTINUED_JSR) {
            return op == Opcode.JSR || op == Opcode.JSR_W ? 1 : 0;
        }
        return 0;
    }

    // The nine stack-manipulation ones. They go in a table because there is no rule: `dup2` pushes
    // two slots or duplicates a two-slot value depending on what is on top, and in both readings
    // the effect on the HEIGHT is the same, which is the only thing needed here.
    private static int stackEffect(Opcode op) {
        if (op == Opcode.POP) {
            return -1;
        }
        if (op == Opcode.POP2) {
            return -2;
        }
        if (op == Opcode.DUP || op == Opcode.DUP_X1 || op == Opcode.DUP_X2) {
            return 1;
        }
        if (op == Opcode.DUP2 || op == Opcode.DUP2_X1 || op == Opcode.DUP2_X2) {
            return 2;
        }
        return 0; // swap
    }

    // The arithmetic and comparison ones. `arraylength` falls here and pops one and pushes one.
    private static int operatorEffect(Opcode op) {
        if (op == Opcode.ARRAYLENGTH) {
            return 0;
        }
        if (op == Opcode.INEG || op == Opcode.FNEG) {
            return 0;
        }
        if (op == Opcode.LNEG || op == Opcode.DNEG) {
            return 0;
        }
        if (op == Opcode.LCMP || op == Opcode.DCMPL || op == Opcode.DCMPG) {
            return -3; // two two-slot values go out, an int comes in
        }
        if (op == Opcode.FCMPL || op == Opcode.FCMPG) {
            return -1;
        }
        if (op == Opcode.LSHL || op == Opcode.LSHR || op == Opcode.LUSHR) {
            return -1; // long and int, long remains
        }
        if (op == Opcode.LADD || op == Opcode.LSUB || op == Opcode.LMUL || op == Opcode.LDIV
                || op == Opcode.LREM || op == Opcode.LAND || op == Opcode.LOR
                || op == Opcode.LXOR) {
            return -2;
        }
        if (op == Opcode.DADD || op == Opcode.DSUB || op == Opcode.DMUL || op == Opcode.DDIV
                || op == Opcode.DREM) {
            return -2;
        }
        return -1; // the int and float ones: two go in, one comes out
    }

    /** What a method descriptor does to the stack, without counting the receiver. */
    private static int descriptorEffect(String desc) {
        int i = desc.indexOf('(') + 1;
        int slots = 0;
        while (i < desc.length() && desc.charAt(i) != ')') {
            int j = StackCounter.endOfType(desc, i);
            slots = slots + TypeKind.fromDescriptor(desc.substring(i, j)).slotSize();
            i = j;
        }
        String ret = desc.substring(desc.indexOf(')') + 1);
        return TypeKind.fromDescriptor(ret).slotSize() - slots;
    }

    // Where the type starting at `i` ends. Nested arrays and class names are the only thing that
    // does not measure one character.
    private static int endOfType(String desc, int i) {
        int j = i;
        while (j < desc.length() && desc.charAt(j) == '[') {
            j = j + 1;
        }
        if (j < desc.length() && desc.charAt(j) == 'L') {
            int end = desc.indexOf(';', j);
            return end < 0 ? desc.length() : end + 1;
        }
        return j + 1;
    }

    /** The index of each label in the list of instructions. */
    static Map<Label, Integer> indexLabels(List<Object> raw) {
        Map<Label, Integer> out = new HashMap<Label, Integer>();
        int idx = 0;
        for (int i = 0; i < raw.size(); i++) {
            Object o = raw.get(i);
            if (o instanceof java.lang.classfile.instruction.LabelTarget) {
                out.put(((java.lang.classfile.instruction.LabelTarget) o).label(),
                        Integer.valueOf(idx));
            } else if (o instanceof Instruction) {
                idx = idx + 1;
            }
        }
        return out;
    }
}
