package java.lang.constant;

import java.util.ArrayList;
import java.util.List;

// Shared character-level helpers for the descriptor spellings. They exist as plain loops
// because KajiLibrary's `String` is deliberately minimal — it has `length`, `charAt` and
// `substring`, and nothing like `replace`, `indexOf` or `toCharArray`. Writing them here keeps
// that gap in one place instead of scattering hand-rolled loops through the package.
class DescNames {

    // `java.lang` -> `java/lang`, and the reverse. One routine with the pair as arguments,
    // since the two directions differ only in which character is swapped for which.
    static String swap(String s, char from, char to) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == from) {
                sb.append(to);
            } else {
                sb.append(c);
            }
            i = i + 1;
        }
        return sb.toString();
    }

    // The index of the last `c` in `s`, or -1. Stands in for `String.lastIndexOf`.
    static int lastIndexOf(String s, char c) {
        int found = -1;
        int i = 0;
        int n = s.length();
        while (i < n) {
            if (s.charAt(i) == c) {
                found = i;
            }
            i = i + 1;
        }
        return found;
    }

    // The number of leading `c` characters — how an array descriptor's dimension is counted.
    static int countLeading(String s, char c) {
        int i = 0;
        int n = s.length();
        while (i < n && s.charAt(i) == c) {
            i = i + 1;
        }
        return i;
    }

    static String substringFrom(String s, int begin) {
        return s.substring(begin, s.length());
    }

    // True when `s` is a single-character primitive descriptor (`I`, `V`, `Z`, ...).
    static boolean isPrimitiveDescriptor(String s) {
        boolean prim = false;
        if (s.length() == 1) {
            char c = s.charAt(0);
            prim = c == 'B' || c == 'C' || c == 'D' || c == 'F'
                    || c == 'I' || c == 'J' || c == 'S' || c == 'Z' || c == 'V';
        }
        return prim;
    }

    // The keyword a one-character primitive descriptor stands for. Lives here rather than on
    // `ClassDesc`: an interface member is implicitly public, and a public member the JDK does not
    // declare is an `EXTRA` for the gate.
    static String primitiveName(char tag) {
        String name = "void";
        if (tag == 'B') { name = "byte"; }
        else if (tag == 'C') { name = "char"; }
        else if (tag == 'D') { name = "double"; }
        else if (tag == 'F') { name = "float"; }
        else if (tag == 'I') { name = "int"; }
        else if (tag == 'J') { name = "long"; }
        else if (tag == 'S') { name = "short"; }
        else if (tag == 'Z') { name = "boolean"; }
        return name;
    }

    // Walks a parameter list, one field descriptor at a time. A descriptor is self-delimiting:
    // `[` prefixes repeat, `L...;` runs to the semicolon, anything else is one character — which
    // is why no separator is needed between parameters in the class file.
    static ClassDesc[] splitParams(String params) {
        List<ClassDesc> found = new ArrayList<ClassDesc>();
        int i = 0;
        int n = params.length();
        while (i < n) {
            int start = i;
            while (params.charAt(i) == '[') { i = i + 1; }
            if (params.charAt(i) == 'L') {
                while (params.charAt(i) != ';') { i = i + 1; }
            }
            i = i + 1;
            found.add(ClassDesc.ofDescriptor(params.substring(start, i)));
        }
        ClassDesc[] out = new ClassDesc[found.size()];
        int k = 0;
        while (k < out.length) { out[k] = found.get(k); k = k + 1; }
        return out;
    }

    // The dotted binary name a class-or-interface descriptor stands for: `Ljava/lang/String;`
    // gives `java.lang.String`. For a primitive or an array the notion does not apply and the
    // descriptor comes back unchanged.
    static String binaryNameOf(String descriptor) {
        String name = descriptor;
        if (descriptor.charAt(0) == 'L') {
            name = swap(descriptor.substring(1, descriptor.length() - 1), '/', '.');
        }
        return name;
    }
}
