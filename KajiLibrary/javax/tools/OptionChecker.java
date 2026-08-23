package javax.tools;

// KajiLibrary's javax.tools.OptionChecker — asks a tool "do you know this command-line
// option, and if so how many arguments does it take?". Negative means "never heard of it",
// which is what lets a caller reject a bad option before starting any work.
public interface OptionChecker {

    int isSupportedOption(String option);
}
