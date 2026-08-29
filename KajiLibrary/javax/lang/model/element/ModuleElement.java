package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.UnknownDirectiveException;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.ModuleElement — a module declaration.
//
// asType() gives a NoType of kind MODULE: like a package, a module is a declaration with no
// type. getEnclosedElements() lists the packages the module contains. An *unnamed* module
// reports isUnnamed() true, an empty qualified name, and no directives — it is what code on
// the classpath rather than the module path lands in.
//
// The body of a module declaration is modelled as a list of Directives, one per `requires`
// / `exports` / `opens` / `uses` / `provides` clause, visited through DirectiveVisitor.
// isOpen() covers the whole-module `open module` form, which is not a directive.
public interface ModuleElement extends Element, QualifiedNameable {

    TypeMirror asType();

    Name getQualifiedName();

    Name getSimpleName();

    List<? extends Element> getEnclosedElements();

    boolean isOpen();

    boolean isUnnamed();

    Element getEnclosingElement();

    List<? extends Directive> getDirectives();

    // Every member type below spells out `public`, which is redundant in Java (JLS 9.5:
    // members of an interface are implicitly public) but not to the frozen javac, which
    // applies the implicit-public rule to abstract methods and not to member types — the
    // same gap #116 records for interface `static` methods. Without it the nested classes
    // come out package-private and are unusable from any other package.

    // The kind of a Directive — the discriminator for code that would rather switch than
    // implement a visitor.
    public enum DirectiveKind {
        REQUIRES,
        EXPORTS,
        OPENS,
        USES,
        PROVIDES;
    }

    // One clause in the body of a module declaration. Not an Element: a directive is part
    // of a declaration, not a declaration of its own.
    public interface Directive {

        DirectiveKind getKind();

        <R, P> R accept(DirectiveVisitor<R, P> v, P p);
    }

    // The visitor over module directives. Same shape and same evolution story as
    // ElementVisitor: visitUnknown catches directive kinds added after a visitor was
    // written, and throws UnknownDirectiveException by default.
    public interface DirectiveVisitor<R, P> {

        default R visit(Directive d) {
            return visit(d, null);
        }

        default R visit(Directive d, P p) {
            return d.accept(this, p);
        }

        R visitRequires(RequiresDirective d, P p);

        R visitExports(ExportsDirective d, P p);

        R visitOpens(OpensDirective d, P p);

        R visitUses(UsesDirective d, P p);

        R visitProvides(ProvidesDirective d, P p);

        default R visitUnknown(Directive d, P p) {
            throw new UnknownDirectiveException(d, p);
        }
    }

    // A `requires` directive. isStatic() is the `static` (compile-time only) modifier and
    // isTransitive() the `transitive` (re-exported to readers) one.
    public interface RequiresDirective extends Directive {

        boolean isStatic();

        boolean isTransitive();

        ModuleElement getDependency();
    }

    // An `exports` directive. getTargetModules() is null for an unqualified export — the
    // package goes to everyone — and the target list for a qualified `exports … to …`.
    public interface ExportsDirective extends Directive {

        PackageElement getPackage();

        List<? extends ModuleElement> getTargetModules();
    }

    // An `opens` directive: the reflective counterpart of `exports`, with the same
    // null-means-unqualified convention on getTargetModules().
    public interface OpensDirective extends Directive {

        PackageElement getPackage();

        List<? extends ModuleElement> getTargetModules();
    }

    // A `provides … with …` directive.
    public interface ProvidesDirective extends Directive {

        TypeElement getService();

        List<? extends TypeElement> getImplementations();
    }

    // A `uses` directive.
    public interface UsesDirective extends Directive {

        TypeElement getService();
    }
}
