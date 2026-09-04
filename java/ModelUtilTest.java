import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.UnknownElementException;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.UnknownTypeException;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.AbstractTypeVisitor6;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementKindVisitor14;
import javax.lang.model.util.ElementKindVisitor6;
import javax.lang.model.util.ElementKindVisitor7;
import javax.lang.model.util.ElementScanner14;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.util.ElementScanner7;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleElementVisitorPreview;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.TypeKindVisitor6;
import javax.lang.model.util.TypeKindVisitor9;

/**
 * Prueba de comportamiento de javax.lang.model.util.
 *
 * <p>Lo que se comprueba es el **despacho**: que cada `visitXxx` caiga donde el contrato de su version
 * dice que cae. Para eso no hace falta un compilador — hacen falta implementaciones falsas de `Element`
 * y `TypeMirror` que sepan decir su `getKind()` y llamar al `visitXxx` correcto desde `accept`, que es
 * exactamente lo que hay aca abajo.
 *
 * <p>`run()` devuelve -1 si pasa todo, o el indice de la primera comprobacion que fallo.
 */
public class ModelUtilTest {

    // ---- comprobaciones ------------------------------------------------------------------------

    private static int idx = 0;
    private static int fallo = -1;

    private static void check(boolean ok) {
        if (fallo < 0 && !ok) {
            fallo = idx;
        }
        idx++;
    }

    public static int run() {
        idx = 0;
        fallo = -1;

        elementFilter();
        despachoBase();
        simpleElement();
        simpleType();
        simpleAnnotationValue();
        elementKind();
        typeKind();
        scanner();

        return fallo;
    }

    // ---- ElementFilter -------------------------------------------------------------------------

    private static void elementFilter() {
        List<Element> todos = new ArrayList<Element>();
        FakeVariable campo = new FakeVariable(ElementKind.FIELD, "campo");
        FakeVariable cte = new FakeVariable(ElementKind.ENUM_CONSTANT, "CTE");
        FakeVariable param = new FakeVariable(ElementKind.PARAMETER, "p");
        FakeExecutable metodo = new FakeExecutable(ElementKind.METHOD, "m");
        FakeExecutable ctor = new FakeExecutable(ElementKind.CONSTRUCTOR, "<init>");
        FakeType clase = new FakeType(ElementKind.CLASS, "C");
        FakeType registro = new FakeType(ElementKind.RECORD, "R");
        FakeType interfaz = new FakeType(ElementKind.INTERFACE, "I");
        todos.add(campo);
        todos.add(metodo);
        todos.add(cte);
        todos.add(param);
        todos.add(ctor);
        todos.add(clase);
        todos.add(registro);
        todos.add(interfaz);

        // 0: un campo y una constante de enum son los dos campos; un parametro no.
        check(ElementFilter.fieldsIn(todos).size() == 2);
        // 1: un constructor no es un metodo aunque los dos sean ExecutableElement.
        check(ElementFilter.methodsIn(todos).size() == 1);
        // 2: el constructor si esta en constructorsIn.
        check(ElementFilter.constructorsIn(todos).size() == 1);
        // 3: clase, registro e interfaz son los tres tipos.
        check(ElementFilter.typesIn(todos).size() == 3);
        // 4: nada mas es un paquete.
        check(ElementFilter.packagesIn(todos).size() == 0);

        // 5: la version de List devuelve los campos en el orden de la entrada.
        List<VariableElement> campos = ElementFilter.fieldsIn(todos);
        check(campos.get(0) == campo && campos.get(1) == cte);

        // 6: la version de Set conserva el orden de iteracion del conjunto de entrada.
        Set<Element> conj = new LinkedHashSet<Element>();
        conj.add(cte);
        conj.add(metodo);
        conj.add(campo);
        Set<VariableElement> camposConj = ElementFilter.fieldsIn(conj);
        check(camposConj.size() == 2 && camposConj.iterator().next() == cte);
    }

    // ---- despacho de los abstractos ------------------------------------------------------------

    private static void despachoBase() {
        // 7: `visit` llega al visitXxx que corresponde al elemento, via accept.
        Registro reg = new Registro();
        AbstractElementVisitor6<String, Void> v = new AbstractElementVisitor6<String, Void>() {
            public String visitPackage(javax.lang.model.element.PackageElement e, Void p) {
                return "package";
            }

            public String visitType(TypeElement e, Void p) {
                return "type";
            }

            public String visitVariable(VariableElement e, Void p) {
                return "variable";
            }

            public String visitExecutable(ExecutableElement e, Void p) {
                return "executable";
            }

            public String visitTypeParameter(TypeParameterElement e, Void p) {
                return "typeParameter";
            }
        };
        check("type".equals(v.visit(new FakeType(ElementKind.CLASS, "C"), null)));
        // 8: la forma de un solo argumento pasa null y despacha igual.
        check("executable".equals(v.visit(new FakeExecutable(ElementKind.METHOD, "m"))));
        // 9: un modulo es desconocido para un visitante de 6 — visitUnknown tira.
        check(tiraElemento(v, new FakeModule()));
        // 10: un componente de registro tambien.
        check(tiraElemento(v, new FakeRecordComponent()));
        // 11: y un `visitUnknown` redefinido gana sobre el que tira.
        AbstractElementVisitor6<String, Void> v2 = new AbstractElementVisitor6<String, Void>() {
            public String visitPackage(javax.lang.model.element.PackageElement e, Void p) {
                return null;
            }

            public String visitType(TypeElement e, Void p) {
                return null;
            }

            public String visitVariable(VariableElement e, Void p) {
                return null;
            }

            public String visitExecutable(ExecutableElement e, Void p) {
                return null;
            }

            public String visitTypeParameter(TypeParameterElement e, Void p) {
                return null;
            }

            public String visitUnknown(Element e, Void p) {
                return "desconocido";
            }
        };
        check("desconocido".equals(v2.visit(new FakeModule(), null)));
        reg.toString();
    }

    // ---- SimpleElementVisitor ------------------------------------------------------------------

    private static void simpleElement() {
        SimpleElementVisitor6<String, Void> s6 = new SimpleElementVisitor6D() {
        };
        // 12: un ejecutable cae en defaultAction, que devuelve DEFAULT_VALUE.
        check("D".equals(s6.visit(new FakeExecutable(ElementKind.METHOD, "m"), null)));
        // 13: un campo tambien.
        check("D".equals(s6.visit(new FakeVariable(ElementKind.FIELD, "f"), null)));
        // 14: pero una variable de recurso es de Java 7 y este visitante es de 6: tira.
        check(tiraElemento(s6, new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r")));

        SimpleElementVisitor7<String, Void> s7 = new SimpleElementVisitor7D() {
        };
        // 15: en 7 la variable de recurso ya existe y entra al embudo.
        check("D".equals(s7.visit(new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r"), null)));

        // 16: el constructor sin argumentos deja DEFAULT_VALUE en null.
        SimpleElementVisitor7<String, Void> sinDefecto = new SimpleElementVisitor7<String, Void>() {
        };
        check(sinDefecto.visit(new FakeVariable(ElementKind.FIELD, "f"), null) == null);

        // 17: un modulo tira en la de 7, que baja de AbstractElementVisitor6.
        check(tiraElemento(s7, new FakeModule()));

        SimpleElementVisitor9<String, Void> s9 = new SimpleElementVisitor9D() {
        };
        // 18: en 9 el modulo entra al embudo.
        check("D".equals(s9.visit(new FakeModule(), null)));
        // 19: pero el componente de registro todavia tira.
        check(tiraElemento(s9, new FakeRecordComponent()));

        SimpleElementVisitor14<String, Void> s14 = new SimpleElementVisitor14D() {
        };
        // 20: en 14 el componente de registro entra al embudo.
        check("D".equals(s14.visit(new FakeRecordComponent(), null)));

        // 21: redefinir defaultAction cambia todos los casos de una.
        SimpleElementVisitor14<String, Void> propio = new SimpleElementVisitor14D() {
            protected String defaultAction(Element e, Void p) {
                return "mio:" + e.getKind();
            }
        };
        check("mio:METHOD".equals(propio.visit(new FakeExecutable(ElementKind.METHOD, "m"), null)));

        // 22: la Preview se comporta como la de 14 — es API reflexiva, no cambia el despacho.
        SimpleElementVisitorPreview<String, Void> sp =
                new SimpleElementVisitorPreviewD() {
                };
        check("D".equals(sp.visit(new FakeRecordComponent(), null)));
    }

    // ---- SimpleTypeVisitor ---------------------------------------------------------------------

    private static void simpleType() {
        SimpleTypeVisitor6<String, Void> t6 = new SimpleTypeVisitor6D() {
        };
        // 23: un primitivo cae en defaultAction.
        check("D".equals(t6.visit(new FakePrimitive(TypeKind.INT), null)));
        // 24: el tipo union es de Java 7: en el visitante de 6 tira.
        check(tiraTipo(t6, new FakeUnion()));
        // 25: el tipo interseccion es de Java 8: tambien tira.
        check(tiraTipo(t6, new FakeIntersection()));

        SimpleTypeVisitor7<String, Void> t7 = new SimpleTypeVisitor7D() {
        };
        // 26: en 7 el union entra al embudo.
        check("D".equals(t7.visit(new FakeUnion(), null)));
        // 27: pero la interseccion sigue tirando.
        check(tiraTipo(t7, new FakeIntersection()));

        SimpleTypeVisitor8<String, Void> t8 = new SimpleTypeVisitor8D() {
        };
        // 28: en 8 la interseccion entra al embudo.
        check("D".equals(t8.visit(new FakeIntersection(), null)));

        // 29: el visitante abstracto de tipos tira UnknownTypeException, no la de elementos.
        AbstractTypeVisitor6<String, Void> abs = new BaseTypeVisitor();
        check(tiraTipo(abs, new FakeUnion()));
    }

    // ---- SimpleAnnotationValueVisitor ----------------------------------------------------------

    private static void simpleAnnotationValue() {
        SimpleAnnotationValueVisitor6<String, Void> a6 =
                new SimpleAnnotationValueVisitor6D() {
                };
        // 30: un int cae en defaultAction.
        check("D".equals(a6.visit(new FakeAnnotationValue(Integer.valueOf(7)), null)));

        // 31: el embudo recibe el primitivo **autoboxeado**, que es lo que la firma de Object obliga.
        SimpleAnnotationValueVisitor6<String, Void> mira =
                new SimpleAnnotationValueVisitor6D() {
                    protected String defaultAction(Object o, Void p) {
                        return o.getClass().getName();
                    }
                };
        check("java.lang.Integer".equals(mira.visit(new FakeAnnotationValue(Integer.valueOf(7)),
                null)));

        // 32: redefinir un visitXxx puntual gana sobre el embudo.
        SimpleAnnotationValueVisitor6<String, Void> puntual =
                new SimpleAnnotationValueVisitor6D() {
                    public String visitInt(int i, Void p) {
                        return "int:" + i;
                    }
                };
        check("int:7".equals(puntual.visit(new FakeAnnotationValue(Integer.valueOf(7)), null)));
        // 33: y un String sigue cayendo en el embudo.
        check("D".equals(puntual.visit(new FakeAnnotationValue("hola"), null)));
    }

    // ---- ElementKindVisitor --------------------------------------------------------------------

    private static void elementKind() {
        ElementKindVisitor6<String, Void> k6 = new ElementKindVisitor6D() {
            public String visitTypeAsClass(TypeElement e, Void p) {
                return "clase";
            }

            public String visitVariableAsField(VariableElement e, Void p) {
                return "campo";
            }

            public String visitExecutableAsMethod(ExecutableElement e, Void p) {
                return "metodo";
            }
        };
        // 34: una clase llega a visitTypeAsClass y no al generico visitType.
        check("clase".equals(k6.visit(new FakeType(ElementKind.CLASS, "C"), null)));
        // 35: una interfaz no fue redefinida y cae en el embudo.
        check("D".equals(k6.visit(new FakeType(ElementKind.INTERFACE, "I"), null)));
        // 36: un campo llega a visitVariableAsField.
        check("campo".equals(k6.visit(new FakeVariable(ElementKind.FIELD, "f"), null)));
        // 37: un metodo llega a visitExecutableAsMethod.
        check("metodo".equals(k6.visit(new FakeExecutable(ElementKind.METHOD, "m"), null)));
        // 38: un constructor no: cae en el embudo.
        check("D".equals(k6.visit(new FakeExecutable(ElementKind.CONSTRUCTOR, "<init>"), null)));
        // 39: un registro es de Java 14 — el reparto lo nombra, pero cae en visitUnknown y tira.
        check(tiraElemento(k6, new FakeType(ElementKind.RECORD, "R")));
        // 40: una variable de recurso, igual.
        check(tiraElemento(k6, new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r")));
        // 41: una variable de vinculo, igual.
        check(tiraElemento(k6, new FakeVariable(ElementKind.BINDING_VARIABLE, "b")));

        // 42: un TypeElement con un kind que no declara un tipo es un modelo roto, no algo nuevo:
        //     tira AssertionError y no UnknownElementException.
        check(tiraAssertion(k6, new FakeType(ElementKind.OTHER, "X")));

        ElementKindVisitor7<String, Void> k7 = new ElementKindVisitor7D() {
        };
        // 43: en 7 la variable de recurso entra al embudo.
        check("D".equals(k7.visit(new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r"), null)));
        // 44: pero el registro sigue tirando.
        check(tiraElemento(k7, new FakeType(ElementKind.RECORD, "R")));

        ElementKindVisitor14<String, Void> k14 = new ElementKindVisitor14D() {
            public String visitTypeAsRecord(TypeElement e, Void p) {
                return "registro";
            }
        };
        // 45: en 14 el registro llega a visitTypeAsRecord.
        check("registro".equals(k14.visit(new FakeType(ElementKind.RECORD, "R"), null)));
        // 46: la variable de vinculo entra al embudo.
        check("D".equals(k14.visit(new FakeVariable(ElementKind.BINDING_VARIABLE, "b"), null)));
        // 47: y el componente de registro tambien.
        check("D".equals(k14.visit(new FakeRecordComponent(), null)));
    }

    // ---- TypeKindVisitor -----------------------------------------------------------------------

    private static void typeKind() {
        TypeKindVisitor6<String, Void> k6 = new TypeKindVisitor6D() {
            public String visitPrimitiveAsInt(PrimitiveType t, Void p) {
                return "int";
            }

            public String visitNoTypeAsVoid(NoType t, Void p) {
                return "void";
            }
        };
        // 48: un int llega a visitPrimitiveAsInt y no al generico visitPrimitive.
        check("int".equals(k6.visit(new FakePrimitive(TypeKind.INT), null)));
        // 49: un long no fue redefinido y cae en el embudo.
        check("D".equals(k6.visit(new FakePrimitive(TypeKind.LONG), null)));
        // 50: `void` llega a visitNoTypeAsVoid.
        check("void".equals(k6.visit(new FakeNoType(TypeKind.VOID), null)));
        // 51: NONE cae en el embudo.
        check("D".equals(k6.visit(new FakeNoType(TypeKind.NONE), null)));
        // 52: el pseudotipo de un modulo es de Java 9: en el de 6 tira.
        check(tiraTipo(k6, new FakeNoType(TypeKind.MODULE)));
        // 53: un PrimitiveType cuyo kind no es primitivo es un modelo roto: AssertionError.
        check(tiraAssertionTipo(k6, new FakePrimitive(TypeKind.DECLARED)));

        TypeKindVisitor9<String, Void> k9 = new TypeKindVisitor9D() {
        };
        // 54: en 9 el pseudotipo de modulo entra al embudo.
        check("D".equals(k9.visit(new FakeNoType(TypeKind.MODULE), null)));
        // 55: y el union tambien, porque la rama de kinds lo reencamina desde la de 7.
        check("D".equals(k9.visit(new FakeUnion(), null)));
        // 56: la interseccion, desde la de 8.
        check("D".equals(k9.visit(new FakeIntersection(), null)));
    }

    // ---- ElementScanner ------------------------------------------------------------------------

    private static void scanner() {
        // Una clase con dos campos y un metodo con un parametro.
        FakeVariable f1 = new FakeVariable(ElementKind.FIELD, "a");
        FakeVariable f2 = new FakeVariable(ElementKind.FIELD, "b");
        FakeVariable par = new FakeVariable(ElementKind.PARAMETER, "x");
        FakeExecutable m = new FakeExecutable(ElementKind.METHOD, "m");
        m.parametros.add(par);
        FakeType clase = new FakeType(ElementKind.CLASS, "C");
        clase.contenidos.add(f1);
        clase.contenidos.add(f2);
        clase.contenidos.add(m);

        // `scan` y no `visit`: `visit` es final y va derecho al `visitXxx`, asi que el elemento raiz
        // nunca pasa por `scan(Element, P)` y el contador no lo veria. `scan` si lo cuenta.
        Contador c = new Contador();
        c.scan(clase, null);
        // 57: la clase, sus dos campos, el metodo y el parametro del metodo: cinco.
        check(c.vistos.size() == 5);
        // 58: el orden es en profundidad — el parametro va despues del metodo.
        check(c.vistos.get(0) == clase && c.vistos.get(3) == m && c.vistos.get(4) == par);

        // 59: un ejecutable baja por sus parametros y no por sus contenidos.
        FakeExecutable m2 = new FakeExecutable(ElementKind.METHOD, "m2");
        m2.parametros.add(par);
        m2.contenidos.add(f1);
        Contador c2 = new Contador();
        c2.scan(m2, null);
        check(c2.vistos.size() == 2 && c2.vistos.get(1) == par);

        // 60: una variable de recurso tira en el escaner de 6.
        ElementScanner6<Void, Void> e6 = new ElementScanner6<Void, Void>() {
        };
        check(tiraElemento(e6, new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r")));
        // 61: y un componente de registro tambien.
        check(tiraElemento(e6, new FakeRecordComponent()));

        ElementScanner7<Void, Void> e7 = new ElementScanner7<Void, Void>() {
        };
        // 62: en 7 la variable de recurso se recorre sin tirar.
        boolean ok62 = true;
        try {
            e7.visit(new FakeVariable(ElementKind.RESOURCE_VARIABLE, "r"), null);
        } catch (RuntimeException ex) {
            ok62 = false;
        }
        check(ok62);

        // 63: `scan` de una coleccion vacia devuelve DEFAULT_VALUE.
        ElementScanner7<String, Void> conDefecto = new ElementScanner7D() {
        };
        check("D".equals(conDefecto.scan(new ArrayList<Element>(), null)));

        // 64: `scan` de varios devuelve el resultado del **ultimo**, no una combinacion.
        ElementScanner7<String, Void> ultimo = new ElementScanner7D() {
            public String scan(Element e, Void p) {
                return e.getSimpleName().toString();
            }
        };
        List<Element> tres = new ArrayList<Element>();
        tres.add(f1);
        tres.add(f2);
        tres.add(m);
        check("m".equals(ultimo.scan(tres, null)));

        // Los parametros de tipo no estan en getEnclosedElements: hasta la version 14 nadie los veia.
        FakeTypeParameter tp = new FakeTypeParameter("T");
        FakeType generica = new FakeType(ElementKind.CLASS, "G");
        generica.contenidos.add(f1);
        generica.parametrosDeTipo.add(tp);

        Contador cViejo = new Contador();
        cViejo.scan(generica, null);
        // 65: el escaner de 6 no visita el parametro de tipo: la clase y el campo, nada mas.
        check(cViejo.vistos.size() == 2 && !cViejo.vistos.contains(tp));

        Contador14 cNuevo = new Contador14();
        cNuevo.scan(generica, null);
        // 66: el de 14 si lo visita, y **antes** que los contenidos.
        check(cNuevo.vistos.size() == 3 && cNuevo.vistos.get(1) == tp
                && cNuevo.vistos.get(2) == f1);

        // 67: en 14 un componente de registro se recorre en vez de tirar.
        ElementScanner14<Void, Void> e14 = new ElementScanner14<Void, Void>() {
        };
        boolean ok67 = true;
        try {
            e14.visit(new FakeRecordComponent(), null);
        } catch (RuntimeException ex) {
            ok67 = false;
        }
        check(ok67);
    }

    // ---- utilidades de la prueba ---------------------------------------------------------------

    private static <R> boolean tiraElemento(ElementVisitor<R, Void> v, Element e) {
        try {
            v.visit(e, null);
            return false;
        } catch (UnknownElementException ex) {
            return true;
        }
    }

    private static <R> boolean tiraTipo(TypeVisitor<R, Void> v, TypeMirror t) {
        try {
            v.visit(t, null);
            return false;
        } catch (UnknownTypeException ex) {
            return true;
        }
    }

    private static <R> boolean tiraAssertion(ElementVisitor<R, Void> v, Element e) {
        try {
            v.visit(e, null);
            return false;
        } catch (AssertionError ex) {
            return true;
        }
    }

    private static <R> boolean tiraAssertionTipo(TypeVisitor<R, Void> v, TypeMirror t) {
        try {
            v.visit(t, null);
            return false;
        } catch (AssertionError ex) {
            return true;
        }
    }

    // Un escaner que anota por donde paso, que es la manera de ver el recorrido desde afuera.
    static class Contador extends ElementScanner6<Void, Void> {
        final List<Element> vistos = new ArrayList<Element>();

        public Void scan(Element e, Void p) {
            this.vistos.add(e);
            return super.scan(e, p);
        }
    }

    static class Contador14 extends ElementScanner14<Void, Void> {
        final List<Element> vistos = new ArrayList<Element>();

        public Void scan(Element e, Void p) {
            this.vistos.add(e);
            return super.scan(e, p);
        }
    }

    // Un AbstractTypeVisitor6 concreto, para probar el visitUnknown de la raiz de esa familia.
    static class BaseTypeVisitor extends AbstractTypeVisitor6<String, Void> {
        public String visitPrimitive(PrimitiveType t, Void p) {
            return "primitive";
        }

        public String visitNull(javax.lang.model.type.NullType t, Void p) {
            return "null";
        }

        public String visitArray(javax.lang.model.type.ArrayType t, Void p) {
            return "array";
        }

        public String visitDeclared(javax.lang.model.type.DeclaredType t, Void p) {
            return "declared";
        }

        public String visitError(javax.lang.model.type.ErrorType t, Void p) {
            return "error";
        }

        public String visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
            return "typevar";
        }

        public String visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
            return "wildcard";
        }

        public String visitExecutable(javax.lang.model.type.ExecutableType t, Void p) {
            return "executable";
        }

        public String visitNoType(NoType t, Void p) {
            return "notype";
        }
    }

    static class Registro {
    }

    // Los visitantes de abajo existen con nombre y no como anonimos con argumento porque nuestro
    // javac todavia no genera la llamada al constructor de la superclase cuando el anonimo le pasa un
    // argumento cuyo tipo es una variable de tipo de esa superclase (COMPILER_FINDINGS #402). Fijar
    // DEFAULT_VALUE en un constructor con nombre prueba exactamente lo mismo: el valor sigue llegando
    // por el constructor de un argumento, que es lo que las comprobaciones miran.

    static class SimpleElementVisitor6D extends SimpleElementVisitor6<String, Void> {
        SimpleElementVisitor6D() {
            super("D");
        }
    }

    static class SimpleElementVisitor7D extends SimpleElementVisitor7<String, Void> {
        SimpleElementVisitor7D() {
            super("D");
        }
    }

    static class SimpleElementVisitor9D extends SimpleElementVisitor9<String, Void> {
        SimpleElementVisitor9D() {
            super("D");
        }
    }

    static class SimpleElementVisitor14D extends SimpleElementVisitor14<String, Void> {
        SimpleElementVisitor14D() {
            super("D");
        }
    }

    static class SimpleElementVisitorPreviewD extends SimpleElementVisitorPreview<String, Void> {
        SimpleElementVisitorPreviewD() {
            super("D");
        }
    }

    static class SimpleTypeVisitor6D extends SimpleTypeVisitor6<String, Void> {
        SimpleTypeVisitor6D() {
            super("D");
        }
    }

    static class SimpleTypeVisitor7D extends SimpleTypeVisitor7<String, Void> {
        SimpleTypeVisitor7D() {
            super("D");
        }
    }

    static class SimpleTypeVisitor8D extends SimpleTypeVisitor8<String, Void> {
        SimpleTypeVisitor8D() {
            super("D");
        }
    }

    static class SimpleAnnotationValueVisitor6D extends SimpleAnnotationValueVisitor6<String, Void> {
        SimpleAnnotationValueVisitor6D() {
            super("D");
        }
    }

    static class ElementKindVisitor6D extends ElementKindVisitor6<String, Void> {
        ElementKindVisitor6D() {
            super("D");
        }
    }

    static class ElementKindVisitor7D extends ElementKindVisitor7<String, Void> {
        ElementKindVisitor7D() {
            super("D");
        }
    }

    static class ElementKindVisitor14D extends ElementKindVisitor14<String, Void> {
        ElementKindVisitor14D() {
            super("D");
        }
    }

    static class TypeKindVisitor6D extends TypeKindVisitor6<String, Void> {
        TypeKindVisitor6D() {
            super("D");
        }
    }

    static class TypeKindVisitor9D extends TypeKindVisitor9<String, Void> {
        TypeKindVisitor9D() {
            super("D");
        }
    }

    static class ElementScanner7D extends ElementScanner7<String, Void> {
        ElementScanner7D() {
            super("D");
        }
    }

    // ---- falsos: elementos ---------------------------------------------------------------------

    static class FakeName implements Name {
        private final String s;

        FakeName(String s) {
            this.s = s;
        }

        public int length() {
            return this.s.length();
        }

        public char charAt(int i) {
            return this.s.charAt(i);
        }

        public CharSequence subSequence(int a, int b) {
            return this.s.subSequence(a, b);
        }

        public String toString() {
            return this.s;
        }

        public boolean contentEquals(CharSequence cs) {
            return this.s.contentEquals(cs);
        }
    }

    // La base de todos los elementos falsos: lo que ningun despacho mira devuelve vacio o null.
    abstract static class FakeElement implements Element {
        final ElementKind kind;
        final Name nombre;
        final List<Element> contenidos = new ArrayList<Element>();

        FakeElement(ElementKind kind, String nombre) {
            this.kind = kind;
            this.nombre = new FakeName(nombre);
        }

        public ElementKind getKind() {
            return this.kind;
        }

        public Name getSimpleName() {
            return this.nombre;
        }

        public List<? extends Element> getEnclosedElements() {
            return this.contenidos;
        }

        public Element getEnclosingElement() {
            return null;
        }

        public TypeMirror asType() {
            return null;
        }

        public Set<Modifier> getModifiers() {
            return new LinkedHashSet<Modifier>();
        }

        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return new ArrayList<AnnotationMirror>();
        }

        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            return null;
        }

        public String toString() {
            return this.nombre.toString();
        }
    }

    static class FakeVariable extends FakeElement implements VariableElement {
        FakeVariable(ElementKind kind, String nombre) {
            super(kind, nombre);
        }

        public Object getConstantValue() {
            return null;
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitVariable(this, p);
        }
    }

    static class FakeExecutable extends FakeElement implements ExecutableElement {
        final List<VariableElement> parametros = new ArrayList<VariableElement>();
        final List<TypeParameterElement> parametrosDeTipo = new ArrayList<TypeParameterElement>();

        FakeExecutable(ElementKind kind, String nombre) {
            super(kind, nombre);
        }

        public List<? extends TypeParameterElement> getTypeParameters() {
            return this.parametrosDeTipo;
        }

        public TypeMirror getReturnType() {
            return null;
        }

        public List<? extends VariableElement> getParameters() {
            return this.parametros;
        }

        public TypeMirror getReceiverType() {
            return null;
        }

        public boolean isVarArgs() {
            return false;
        }

        public boolean isDefault() {
            return false;
        }

        public List<? extends TypeMirror> getThrownTypes() {
            return new ArrayList<TypeMirror>();
        }

        public AnnotationValue getDefaultValue() {
            return null;
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }
    }

    static class FakeType extends FakeElement implements TypeElement {
        final List<TypeParameterElement> parametrosDeTipo = new ArrayList<TypeParameterElement>();

        FakeType(ElementKind kind, String nombre) {
            super(kind, nombre);
        }

        public NestingKind getNestingKind() {
            return NestingKind.TOP_LEVEL;
        }

        public Name getQualifiedName() {
            return this.nombre;
        }

        public TypeMirror getSuperclass() {
            return null;
        }

        public List<? extends TypeMirror> getInterfaces() {
            return new ArrayList<TypeMirror>();
        }

        public List<? extends TypeParameterElement> getTypeParameters() {
            return this.parametrosDeTipo;
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitType(this, p);
        }
    }

    static class FakeTypeParameter extends FakeElement implements TypeParameterElement {
        FakeTypeParameter(String nombre) {
            super(ElementKind.TYPE_PARAMETER, nombre);
        }

        public Element getGenericElement() {
            return null;
        }

        public List<? extends TypeMirror> getBounds() {
            return new ArrayList<TypeMirror>();
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitTypeParameter(this, p);
        }
    }

    static class FakeRecordComponent extends FakeElement implements RecordComponentElement {
        FakeRecordComponent() {
            super(ElementKind.RECORD_COMPONENT, "comp");
        }

        public ExecutableElement getAccessor() {
            return null;
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitRecordComponent(this, p);
        }
    }

    static class FakeModule extends FakeElement implements ModuleElement {
        FakeModule() {
            super(ElementKind.MODULE, "mod");
        }

        public Name getQualifiedName() {
            return this.nombre;
        }

        public boolean isOpen() {
            return false;
        }

        public boolean isUnnamed() {
            return false;
        }

        public List<? extends Directive> getDirectives() {
            return new ArrayList<Directive>();
        }

        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitModule(this, p);
        }
    }

    // ---- falsos: tipos -------------------------------------------------------------------------

    abstract static class FakeTypeMirror implements TypeMirror {
        final TypeKind kind;

        FakeTypeMirror(TypeKind kind) {
            this.kind = kind;
        }

        public TypeKind getKind() {
            return this.kind;
        }

        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return new ArrayList<AnnotationMirror>();
        }

        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            return null;
        }

        public String toString() {
            return String.valueOf(this.kind);
        }
    }

    static class FakePrimitive extends FakeTypeMirror implements PrimitiveType {
        FakePrimitive(TypeKind kind) {
            super(kind);
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    static class FakeNoType extends FakeTypeMirror implements NoType {
        FakeNoType(TypeKind kind) {
            super(kind);
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    static class FakeUnion extends FakeTypeMirror implements UnionType {
        FakeUnion() {
            super(TypeKind.UNION);
        }

        public List<? extends TypeMirror> getAlternatives() {
            return new ArrayList<TypeMirror>();
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitUnion(this, p);
        }
    }

    static class FakeIntersection extends FakeTypeMirror implements IntersectionType {
        FakeIntersection() {
            super(TypeKind.INTERSECTION);
        }

        public List<? extends TypeMirror> getBounds() {
            return new ArrayList<TypeMirror>();
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitIntersection(this, p);
        }
    }

    // ---- falsos: valores de anotacion ----------------------------------------------------------

    // Despacha segun el tipo del valor, que es lo que hace un AnnotationValue de verdad.
    static class FakeAnnotationValue implements AnnotationValue {
        private final Object valor;

        FakeAnnotationValue(Object valor) {
            this.valor = valor;
        }

        public Object getValue() {
            return this.valor;
        }

        public String toString() {
            return String.valueOf(this.valor);
        }

        public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            if (this.valor instanceof Integer) {
                return v.visitInt(((Integer) this.valor).intValue(), p);
            }
            if (this.valor instanceof String) {
                return v.visitString((String) this.valor, p);
            }
            if (this.valor instanceof Boolean) {
                return v.visitBoolean(((Boolean) this.valor).booleanValue(), p);
            }
            return v.visitUnknown(this, p);
        }
    }

    public static void main(String[] args) {
        int r = run();
        if (r < 0) {
            System.out.println("ModelUtilTest: OK (" + idx + " comprobaciones)");
        } else {
            System.out.println("ModelUtilTest: FALLO en la comprobacion " + r + " de " + idx);
        }
    }
}
