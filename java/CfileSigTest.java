import java.lang.classfile.ClassSignature;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.Signature.ArrayTypeSig;
import java.lang.classfile.Signature.BaseTypeSig;
import java.lang.classfile.Signature.ClassTypeSig;
import java.lang.classfile.Signature.RefTypeSig;
import java.lang.classfile.Signature.ThrowableSig;
import java.lang.classfile.Signature.TypeArg;
import java.lang.classfile.Signature.TypeParam;
import java.lang.classfile.Signature.TypeVarSig;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

// Firmas genericas (JVMS 4.7.9.1): ida y vuelta, forma del arbol, fabricas y rechazos.
//
// La fuente compila contra las DOS bibliotecas: `java.lang.classfile.Signature` existe igual en
// `java.base` del JDK 25. Corriendo la misma clase con las dos VMs y diffeando la salida, cualquier
// diferencia de comportamiento aparece como una linea distinta. Devuelve -1 si todo cierra.
public class CfileSigTest {

    static int fallas = 0;

    static void ok(String que, Object obtenido, Object esperado) {
        boolean bien = esperado == null ? obtenido == null : esperado.equals(obtenido);
        if (!bien) {
            fallas++;
            System.out.println("FALLA " + que + ": esperaba <" + esperado + "> y hay <"
                    + obtenido + ">");
        } else {
            System.out.println("ok " + que + " = " + obtenido);
        }
    }

    // Ida y vuelta: parsear e imprimir tiene que devolver el mismo texto.
    static void vuelta(String s) {
        ok("tipo/vuelta " + s, Signature.parseFrom(s).signatureString(), s);
    }

    static void vueltaClase(String s) {
        ok("clase/vuelta " + s, ClassSignature.parseFrom(s).signatureString(), s);
    }

    static void vueltaMetodo(String s) {
        ok("metodo/vuelta " + s, MethodSignature.parseFrom(s).signatureString(), s);
    }

    static void rechaza(String etiqueta, Runnable r) {
        try {
            r.run();
            fallas++;
            System.out.println("FALLA " + etiqueta + ": no tiro nada");
        } catch (IllegalArgumentException e) {
            System.out.println("ok " + etiqueta + " -> IllegalArgumentException");
        } catch (NullPointerException e) {
            System.out.println("ok " + etiqueta + " -> NullPointerException");
        } catch (RuntimeException e) {
            fallas++;
            System.out.println("FALLA " + etiqueta + ": tiro " + e.getClass().getName());
        }
    }

    public static int run() {
        fallas = 0;

        vuelta("I");
        vuelta("V");
        vuelta("Ljava/lang/String;");
        vuelta("[[Ljava/lang/String;");
        vuelta("[I");
        vuelta("TT;");
        vuelta("Ljava/util/Map<Ljava/lang/String;+Ljava/lang/Number;>;");
        vuelta("Ljava/util/Map<*>.Entry<TK;TV;>;");
        vuelta("Ljava/util/Map$Entry<TK;TV;>;");
        vuelta("Ljava/util/List<-Ljava/lang/Number;>;");
        vuelta("Ljava/util/List<[TE;>;");

        // La forma del arbol, no solo el texto.
        Signature s = Signature.parseFrom("Ljava/util/Map<*>.Entry<TK;>;");
        ClassTypeSig cts = (ClassTypeSig) s;
        ok("anidada/nombre", cts.className(), "Entry");
        ok("anidada/externa", cts.outerType().get().className(), "java/util/Map");
        ok("anidada/desc", cts.classDesc().descriptorString(), "Ljava/util/Map$Entry;");
        ok("anidada/nargs", "" + cts.typeArgs().size(), "1");
        ok("anidada/externa-args-desnudos",
                "" + cts.outerType().get().typeArgs().size(), "1");
        ok("anidada/externa-desc",
                cts.outerType().get().classDesc().descriptorString(), "Ljava/util/Map;");

        ClassTypeSig plana = (ClassTypeSig) Signature.parseFrom("Ljava/util/Map$Entry<TK;TV;>;");
        ok("plana/nombre", plana.className(), "java/util/Map$Entry");
        ok("plana/sin-externa", "" + plana.outerType().isPresent(), "false");
        ok("plana/desc", plana.classDesc().descriptorString(), "Ljava/util/Map$Entry;");

        ArrayTypeSig arr = (ArrayTypeSig) Signature.parseFrom("[[I");
        ok("arreglo/componente", arr.componentSignature().signatureString(), "[I");
        ok("arreglo/base",
                "" + ((BaseTypeSig) ((ArrayTypeSig) arr.componentSignature())
                        .componentSignature()).baseType(),
                "I");

        TypeVarSig tv = (TypeVarSig) Signature.parseFrom("TE;");
        ok("varde tipo/id", tv.identifier(), "E");

        // Comodines.
        ClassTypeSig m = (ClassTypeSig) Signature.parseFrom(
                "Ljava/util/Map<+Ljava/lang/Number;-Ljava/lang/String;*Ljava/lang/Object;>;");
        List<TypeArg> args = m.typeArgs();
        ok("args/n", "" + args.size(), "4");
        ok("args/0", "" + ((TypeArg.Bounded) args.get(0)).wildcardIndicator(), "EXTENDS");
        ok("args/1", "" + ((TypeArg.Bounded) args.get(1)).wildcardIndicator(), "SUPER");
        ok("args/2 es *", "" + (args.get(2) instanceof TypeArg.Unbounded), "true");
        ok("args/3", "" + ((TypeArg.Bounded) args.get(3)).wildcardIndicator(), "NONE");

        // Firmas de clase.
        vueltaClase("Ljava/lang/Object;");
        vueltaClase("<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/List<TT;>;");
        vueltaClase("<T:Ljava/lang/Object;:Ljava/lang/Comparable<TT;>;>Ljava/lang/Object;");
        vueltaClase("<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;");
        ClassSignature cs = ClassSignature.parseFrom(
                "<T::Ljava/lang/Comparable<TT;>;>Ljava/lang/Object;Ljava/util/List<TT;>;");
        ok("clase/nparams", "" + cs.typeParameters().size(), "1");
        ok("clase/cota-de-clase-ausente",
                "" + cs.typeParameters().get(0).classBound().isPresent(), "false");
        ok("clase/cotas-de-interfaz",
                "" + cs.typeParameters().get(0).interfaceBounds().size(), "1");
        ok("clase/super", cs.superclassSignature().signatureString(), "Ljava/lang/Object;");
        ok("clase/nifs", "" + cs.superinterfaceSignatures().size(), "1");

        // Firmas de metodo.
        vueltaMetodo("()V");
        vueltaMetodo("(IJ)V");
        vueltaMetodo("<T:Ljava/lang/Object;>(TT;[I)Ljava/util/List<TT;>;");
        vueltaMetodo("(Ljava/lang/String;)V^Ljava/io/IOException;^TT;");
        MethodSignature ms = MethodSignature.parseFrom(
                "<T:Ljava/lang/Object;>(TT;[I)Ljava/util/List<TT;>;^Ljava/io/IOException;^TT;");
        ok("metodo/nparams", "" + ms.typeParameters().size(), "1");
        ok("metodo/nargs", "" + ms.arguments().size(), "2");
        ok("metodo/result", ms.result().signatureString(), "Ljava/util/List<TT;>;");
        ok("metodo/nthrows", "" + ms.throwableSignatures().size(), "2");
        ok("metodo/void", MethodSignature.parseFrom("()V").result().signatureString(), "V");

        // Fabricas.
        ok("of/ClassDesc", Signature.of(ClassDesc.of("java.lang.String")).signatureString(),
                "Ljava/lang/String;");
        ok("of/ClassDesc primitivo",
                Signature.of(ClassDesc.ofDescriptor("I")).signatureString(), "I");
        ok("of/ClassDesc arreglo",
                Signature.of(ClassDesc.ofDescriptor("[[J")).signatureString(), "[[J");
        ok("of/MethodTypeDesc",
                MethodSignature.of(MethodTypeDesc.ofDescriptor("(IJ)V")).signatureString(),
                "(IJ)V");
        ok("BaseTypeSig.of(char)", "" + BaseTypeSig.of('J').signatureString(), "J");
        ok("BaseTypeSig.of(ClassDesc)",
                BaseTypeSig.of(ClassDesc.ofDescriptor("D")).signatureString(), "D");
        ok("ArrayTypeSig.of",
                ArrayTypeSig.of(3, BaseTypeSig.of('I')).signatureString(), "[[[I");
        ok("TypeVarSig.of", TypeVarSig.of("X").signatureString(), "TX;");
        ok("ClassTypeSig.of(ClassDesc)",
                ClassTypeSig.of(ClassDesc.of("java.util.List"),
                        TypeArg.of(TypeVarSig.of("E"))).signatureString(),
                "Ljava/util/List<TE;>;");
        ok("ClassTypeSig.of(String)",
                ClassTypeSig.of("java/util/List").signatureString(), "Ljava/util/List;");
        ok("ClassTypeSig.of anidada",
                ClassTypeSig.of(ClassTypeSig.of("java/util/Map", TypeArg.unbounded()),
                        "Entry", TypeArg.of(TypeVarSig.of("K"))).signatureString(),
                "Ljava/util/Map<*>.Entry<TK;>;");
        ok("TypeArg.extendsOf",
                ClassTypeSig.of("p/C", TypeArg.extendsOf(TypeVarSig.of("T"))).signatureString(),
                "Lp/C<+TT;>;");
        ok("TypeArg.superOf",
                ClassTypeSig.of("p/C", TypeArg.superOf(TypeVarSig.of("T"))).signatureString(),
                "Lp/C<-TT;>;");
        ok("TypeArg.unbounded",
                ClassTypeSig.of("p/C", TypeArg.unbounded()).signatureString(), "Lp/C<*>;");
        ok("TypeArg.bounded",
                ClassTypeSig.of("p/C",
                        TypeArg.bounded(TypeArg.Bounded.WildcardIndicator.EXTENDS,
                                TypeVarSig.of("T"))).signatureString(),
                "Lp/C<+TT;>;");

        RefTypeSig objeto = ClassTypeSig.of("java/lang/Object");
        TypeParam tp = TypeParam.of("T", objeto);
        ok("TypeParam.of", tp.identifier() + ":" + tp.classBound().get().signatureString(),
                "T:Ljava/lang/Object;");
        List<TypeParam> tps = new ArrayList<TypeParam>();
        tps.add(tp);
        ok("ClassSignature.of con params",
                ClassSignature.of(tps, ClassTypeSig.of("java/lang/Object"),
                        ClassTypeSig.of("java/io/Serializable")).signatureString(),
                "<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;");
        ok("ClassSignature.of sin params",
                ClassSignature.of(ClassTypeSig.of("java/lang/Object")).signatureString(),
                "Ljava/lang/Object;");
        List<ThrowableSig> tirados = new ArrayList<ThrowableSig>();
        tirados.add(ClassTypeSig.of("java/io/IOException"));
        ok("MethodSignature.of completo",
                MethodSignature.of(tps, tirados, BaseTypeSig.of('V'),
                        TypeVarSig.of("T")).signatureString(),
                "<T:Ljava/lang/Object;>(TT;)V^Ljava/io/IOException;");
        ok("MethodSignature.of simple",
                MethodSignature.of(BaseTypeSig.of('V'), BaseTypeSig.of('I')).signatureString(),
                "(I)V");

        // Rechazos: una firma mal formada NO se acepta a medias.
        rechaza("tipo/letra invalida", new Rechazo(0));
        rechaza("tipo/sin punto y coma", new Rechazo(1));
        rechaza("tipo/sobra texto", new Rechazo(2));
        rechaza("tipo/vacio", new Rechazo(3));
        rechaza("clase/no arranca en L", new Rechazo(4));
        rechaza("clase/sobra texto", new Rechazo(5));
        rechaza("metodo/sin parentesis", new Rechazo(6));
        rechaza("metodo/sin cerrar", new Rechazo(7));
        rechaza("metodo/throws invalido", new Rechazo(8));
        rechaza("tipo/args sin cerrar", new Rechazo(9));
        rechaza("BaseTypeSig.of letra invalida", new Rechazo(10));
        rechaza("BaseTypeSig.of no primitivo", new Rechazo(11));
        rechaza("tipo/null", new Rechazo(12));
        rechaza("args vacios", new Rechazo(13));
        rechaza("params vacios", new Rechazo(14));

        System.out.println("fallas=" + fallas);
        return fallas == 0 ? -1 : fallas;
    }

    public static void main(String[] args) {
        run();
    }
}

// Cada caso de rechazo, como objeto con nombre: el `default` de una lambda por caso haria la fuente
// mas corta y menos portable entre las dos cadenas de compilacion.
class Rechazo implements Runnable {

    private final int cual;

    Rechazo(int cual) {
        this.cual = cual;
    }

    public void run() {
        if (cual == 0) {
            Signature.parseFrom("Q");
        } else if (cual == 1) {
            Signature.parseFrom("Ljava/lang/String");
        } else if (cual == 2) {
            Signature.parseFrom("Ljava/lang/String;X");
        } else if (cual == 3) {
            Signature.parseFrom("");
        } else if (cual == 4) {
            ClassSignature.parseFrom("I");
        } else if (cual == 5) {
            ClassSignature.parseFrom("Ljava/lang/Object;X");
        } else if (cual == 6) {
            MethodSignature.parseFrom("IV");
        } else if (cual == 7) {
            MethodSignature.parseFrom("(I");
        } else if (cual == 8) {
            MethodSignature.parseFrom("()V^I");
        } else if (cual == 9) {
            Signature.parseFrom("Ljava/util/List<TT;;");
        } else if (cual == 10) {
            BaseTypeSig.of('Q');
        } else if (cual == 11) {
            BaseTypeSig.of(ClassDesc.of("java.lang.String"));
        } else if (cual == 12) {
            Signature.parseFrom(null);
        } else if (cual == 13) {
            Signature.parseFrom("Ljava/util/List<>;");
        } else if (cual == 15 - 1) {
            ClassSignature.parseFrom("<>Ljava/lang/Object;");
        }
    }
}
