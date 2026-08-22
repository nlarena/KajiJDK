package java.lang.constant;

import java.util.ArrayList;
import java.util.List;

// A nominal descriptor for a method's signature: the return type plus the parameter types, all
// of them `ClassDesc`s. It is the piece that makes a `MethodHandleDesc` or an `indy` call site
// describable without loading a single class.
//
// `resolveConstantDesc` and the `TypeDescriptor.OfMethod` bridges are OMITTED (`java.lang.invoke`);
// see `ConstantDesc`.
public interface MethodTypeDesc extends ConstantDesc {

    // Parses the class-file spelling, `(II)Ljava/lang/String;`.
    public static MethodTypeDesc ofDescriptor(String descriptor) {
        int close = DescNames.lastIndexOf(descriptor, ')');
        String params = descriptor.substring(1, close);
        String ret = DescNames.substringFrom(descriptor, close + 1);
        return new ConstantMethodTypeDesc(ClassDesc.ofDescriptor(ret), DescNames.splitParams(params));
    }

    public static MethodTypeDesc of(ClassDesc returnDesc) {
        return new ConstantMethodTypeDesc(returnDesc, new ClassDesc[0]);
    }

    public static MethodTypeDesc of(ClassDesc returnDesc, List<ClassDesc> paramDescs) {
        ClassDesc[] params = new ClassDesc[paramDescs.size()];
        int i = 0;
        while (i < params.length) {
            params[i] = paramDescs.get(i);
            i = i + 1;
        }
        return new ConstantMethodTypeDesc(returnDesc, params);
    }

    public static MethodTypeDesc of(ClassDesc returnDesc, ClassDesc[] paramDescs) {
        return new ConstantMethodTypeDesc(returnDesc, paramDescs);
    }

    ClassDesc returnType();

    int parameterCount();

    ClassDesc parameterType(int index);

    List<ClassDesc> parameterList();

    ClassDesc[] parameterArray();

    MethodTypeDesc changeReturnType(ClassDesc returnDesc);

    MethodTypeDesc changeParameterType(int index, ClassDesc paramDesc);

    MethodTypeDesc dropParameterTypes(int start, int end);

    MethodTypeDesc insertParameterTypes(int pos, ClassDesc[] paramDescs);

    String descriptorString();

    // The readable form, `(int,String)void` — display names instead of class-file spelling.
    default String displayDescriptor() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = 0;
        int n = parameterCount();
        while (i < n) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(parameterType(i).displayName());
            i = i + 1;
        }
        sb.append(")");
        sb.append(returnType().displayName());
        return sb.toString();
    }

    boolean equals(Object o);
}

// The implementation shares the file with the interface — see `ClassDesc` for why.
final class ConstantMethodTypeDesc implements MethodTypeDesc {

    private final ClassDesc returnType;
    private final ClassDesc[] params;

    ConstantMethodTypeDesc(ClassDesc returnType, ClassDesc[] params) {
        this.returnType = returnType;
        this.params = params;
    }

    public ClassDesc returnType() {
        return returnType;
    }

    public int parameterCount() {
        return params.length;
    }

    public ClassDesc parameterType(int index) {
        return params[index];
    }

    public List<ClassDesc> parameterList() {
        List<ClassDesc> list = new ArrayList<ClassDesc>();
        int i = 0;
        while (i < params.length) {
            list.add(params[i]);
            i = i + 1;
        }
        return list;
    }

    public ClassDesc[] parameterArray() {
        ClassDesc[] copy = new ClassDesc[params.length];
        int i = 0;
        while (i < params.length) {
            copy[i] = params[i];
            i = i + 1;
        }
        return copy;
    }

    public MethodTypeDesc changeReturnType(ClassDesc returnDesc) {
        return new ConstantMethodTypeDesc(returnDesc, parameterArray());
    }

    public MethodTypeDesc changeParameterType(int index, ClassDesc paramDesc) {
        ClassDesc[] copy = parameterArray();
        copy[index] = paramDesc;
        return new ConstantMethodTypeDesc(returnType, copy);
    }

    // `[start, end)` goes away; everything else keeps its order.
    public MethodTypeDesc dropParameterTypes(int start, int end) {
        ClassDesc[] copy = new ClassDesc[params.length - (end - start)];
        int from = 0;
        int to = 0;
        while (from < params.length) {
            if (from < start || from >= end) {
                copy[to] = params[from];
                to = to + 1;
            }
            from = from + 1;
        }
        return new ConstantMethodTypeDesc(returnType, copy);
    }

    public MethodTypeDesc insertParameterTypes(int pos, ClassDesc[] paramDescs) {
        ClassDesc[] copy = new ClassDesc[params.length + paramDescs.length];
        int to = 0;
        int i = 0;
        while (i < pos) {
            copy[to] = params[i];
            to = to + 1;
            i = i + 1;
        }
        int k = 0;
        while (k < paramDescs.length) {
            copy[to] = paramDescs[k];
            to = to + 1;
            k = k + 1;
        }
        while (i < params.length) {
            copy[to] = params[i];
            to = to + 1;
            i = i + 1;
        }
        return new ConstantMethodTypeDesc(returnType, copy);
    }

    public String descriptorString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = 0;
        while (i < params.length) {
            sb.append(params[i].descriptorString());
            i = i + 1;
        }
        sb.append(")");
        sb.append(returnType.descriptorString());
        return sb.toString();
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ConstantMethodTypeDesc) {
            ConstantMethodTypeDesc other = (ConstantMethodTypeDesc) o;
            same = descriptorString().equals(other.descriptorString());
        }
        return same;
    }

    public int hashCode() {
        return descriptorString().hashCode();
    }

    public String toString() {
        return "MethodTypeDesc[" + displayDescriptor() + "]";
    }
}
