import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * A call to a method a class OVERRIDES from a parameterized interface is reported as ambiguous:
 * the override is not recognised as overriding, so both declarations stay in the candidate set.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_254.java
 *
 * Expected: compiles (SubmissionPublisher.subscribe overrides Flow.Publisher.subscribe).
 * Actual:   `la referencia a `subscribe` es ambigua`.
 *
 * `porInterfaz` is the workaround AND the evidence: through the interface type there is only one
 * declaration in scope, and the same call resolves.
 *
 * Same root as #123 -- the supertype relation is lost when the clause that names the supertype
 * carries type arguments -- but a different symptom: there the override is rejected at the
 * declaration, here the CALL is rejected at the use.
 */
public class finding_254 {

    /** Calling through the class that declares the override. */
    public static void porClase(SubmissionPublisher<String> pub, Flow.Subscriber<String> sub) {
        pub.subscribe(sub);
    }

    /** Calling through the interface -- the control. */
    public static void porInterfaz(SubmissionPublisher<String> pub, Flow.Subscriber<String> sub) {
        Flow.Publisher<String> as = pub;
        as.subscribe(sub);
    }
}
