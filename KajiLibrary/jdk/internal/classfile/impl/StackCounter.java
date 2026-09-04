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
 * Calcula el `max_stack` de un metodo recorriendo su grafo de flujo.
 *
 * <p>Hace falta porque el `Code` lo lleva escrito y la JVM lo comprueba: un valor mas chico que la
 * profundidad real hace que la clase no verifique. Uno mas grande verifica igual y desperdicia
 * marco, asi que **cuando algo no se puede decidir se toma la cota de arriba**, no la de abajo.
 *
 * <h2>Por que un recorrido y no una suma</h2>
 *
 * <p>La profundidad en una instruccion no es la suma de los efectos de las anteriores en el orden
 * del arreglo: depende de por donde se llego. Un `goto` hacia atras, un `catch` que entra con la
 * pila en uno, las dos ramas de un `if` -- todo eso hace que el orden textual no sea el orden de
 * ejecucion. El recorrido visita cada instruccion con la profundidad de entrada de cada camino y se
 * queda con la mayor.
 *
 * <p>La entrada del metodo empieza en cero; **la de cada manejador de excepciones empieza en uno**,
 * porque la JVM le deja la excepcion en la pila. Olvidar eso da un `max_stack` que falla exactamente
 * en los metodos con `try`.
 */
final class StackCounter {

    private StackCounter() {
    }

    /**
     * El `max_stack` del codigo formado por esas instrucciones.
     *
     * @param elements las instrucciones, en orden
     * @param handlers los manejadores, cuya entrada arranca con uno en la pila
     * @param labelIndex de cada etiqueta, el indice de la instruccion a la que apunta
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
                // Uno, no cero: la JVM entra al manejador con la excepcion ya empujada.
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
                // Una pila negativa significa que el codigo esta mal formado. No se corta la cuenta
                // -- calcular un `max_stack` no es verificar-- pero tampoco se propaga un absurdo.
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
        // Si ya se lo visito con una profundidad al menos igual, no hay nada nuevo que propagar.
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
            return; // no sigue
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
     * Cuanto crece (o decrece) la pila con esa instruccion, en **ranuras**.
     *
     * <p>Un `long` y un `double` cuentan dos, que es lo que ocupan. Es la razon de que esto no sea
     * una tabla de opcodes a secas: el efecto de un `invokevirtual` depende de su descriptor, y el
     * de un `getfield` del tipo del campo.
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
            // Salen el arreglo y el indice, entra el elemento.
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
            return -slots - 1; // putfield: sale el objeto y sale el valor
        }
        if (k == Opcode.Kind.INVOKE) {
            InvokeInstruction inv = (InvokeInstruction) ins;
            int e = StackCounter.descriptorEffect(inv.type().stringValue());
            if (op != Opcode.INVOKESTATIC) {
                e = e - 1; // el receptor
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
            return 0; // sale el largo, entra el arreglo
        }
        if (k == Opcode.Kind.NEW_MULTI_ARRAY) {
            return 1 - ((NewMultiArrayInstruction) ins).dimensions();
        }
        if (k == Opcode.Kind.TYPE_CHECK) {
            return 0; // checkcast deja lo mismo; instanceof cambia el tipo pero no la altura
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
            // Los de un operando sacan uno; los de dos, dos; `goto` no saca nada.
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

    // Los nueve de manipulacion de pila. Van en una tabla porque no hay regla: `dup2` empuja dos
    // ranuras o duplica un valor de dos segun lo que haya arriba, y en las dos lecturas el efecto
    // sobre la ALTURA es el mismo, que es lo unico que hace falta acá.
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

    // Los aritmeticos y los de comparacion. `arraylength` cae acá y saca uno y pone uno.
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
            return -3; // salen dos de dos ranuras, entra un int
        }
        if (op == Opcode.FCMPL || op == Opcode.FCMPG) {
            return -1;
        }
        if (op == Opcode.LSHL || op == Opcode.LSHR || op == Opcode.LUSHR) {
            return -1; // long y int, queda long
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
        return -1; // los de int y float: dos entran, uno sale
    }

    /** Lo que un descriptor de metodo le hace a la pila, sin contar el receptor. */
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

    // Donde termina el tipo que empieza en `i`. Los arreglos anidados y los nombres de clase son lo
    // unico que no mide un caracter.
    private static int endOfType(String desc, int i) {
        int j = i;
        while (j < desc.length() && desc.charAt(j) == '[') {
            j = j + 1;
        }
        if (j < desc.length() && desc.charAt(j) == 'L') {
            int fin = desc.indexOf(';', j);
            return fin < 0 ? desc.length() : fin + 1;
        }
        return j + 1;
    }

    /** El indice de cada etiqueta en la lista de instrucciones. */
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
