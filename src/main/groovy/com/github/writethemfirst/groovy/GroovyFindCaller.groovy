package com.github.writethemfirst.groovy

import org.codehaus.groovy.runtime.StackTraceUtils

import com.github.writethemfirst.approvals.utils.stack.FindCaller

import groovy.transform.TypeChecked

@TypeChecked
public class GroovyFindCaller implements FindCaller {

    static final String METHOD_AS_INNER_CLASS_PATTERN = '.*\\$\\p{javaLowerCase}[^.]*'

    /**
     * Returns the caller class of the first potential reference class found by searching the current thread
     * stacktrace.
     *
     * We consider the caller class to be the first one found in the current thread stacktrace after finding the first
     * potential reference class.
     *
     * @param potentialReferenceClasses An array of all potential reference classes to use to search for a caller class.
     *                                  The first class which is found in the current stack trace will be used as
     *                                  reference
     * @return The caller class name of the first potential reference class found in the current stack trace
     */
    @Override
    public String callerClass(Class<?>... potentialReferenceClasses) {
        final List<String> classesInStack =  santizedStackTrace()
                .collect({ it.className })
                .unique()
        List<String> potentialReferenceClassNames = potentialReferenceClasses
                .toList()
                .collect({ it.name })
        firstMatchingCallerClass(classesInStack, potentialReferenceClassNames)
    }

    /**
     * Returns the caller method of the provided `referenceClass`.
     *
     * We consider the caller method to be the first method from the `referenceClass` called in the current thread
     * stacktrace, which isn't a lambda / closure.
     *
     * If found in the current thread stacktrace, the method name will be returned, wrapped in an `Optional`. If no
     * method can be found, an empty `Optional` will be returned.
     *
     * @param referenceClassName the class for which we want to search the caller method in the current thread
     *                           stacktrace
     * @return An `Optional` object containing either the caller method name (as a `String`) or an empty value if it
     * cannot be found
     */
    @Override
    public Optional<String> callerMethod(String referenceClassName) {
        def sanitizedClassName = sanitizeClassName(referenceClassName)
        return santizedStackTrace()
                .findAll({ it.className.equals(sanitizedClassName) })
                .findAll({ !it.methodName.startsWith('lambda$') })
                .collect({ it.methodName })
                .stream().findFirst()
    }

    /**
     * Returns the caller class (from the classes in the current stack trace) of the first matching reference class
     * (from the potential reference classes provided).
     *
     * @param classesInStack            A list of all distinct classes' names from the current thread stacktrace
     * @param potentialReferenceClassNames A List of all potential reference classe names to use to search for a caller class.
     *                                  The first class which is found in the current stack trace will be used as
     *                                  reference to find its caller class
     * @return The name of the caller class of the first matching reference class
     */
    private String firstMatchingCallerClass(List<String> classesInStack, List<String> potentialReferenceClassNames) {
        List<String> reversedStack = classesInStack.reverse()
        final String lastReferenceName = reversedStack
                .find({
                    potentialReferenceClassNames.contains(it)
                })

        if (lastReferenceName) {
            final int referenceIndex = classesInStack.indexOf(lastReferenceName)
            if (referenceIndex + 1 < classesInStack.size()) {
                return classesInStack.get(referenceIndex + 1)
            } else {
                System.err.println("Reference class is found but appears to have no parent in the current stack trace...")
            }
        } else {
            System.err.println("Can't locate any of the provided reference classes in the current stack trace...")
        }
        return ""
    }

    /**
     * Remove all apparently Groovy-internal trace entries from the stacktrace.
     *
     * The Groovy stacktrace is usually littered with lots of intermediate calls. Also in some version of Groovy, you
     * find that a (static) method is represented as an inner class which is then called from
     * org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(), resp. callStatic(), which is not handled by
     * Groovy's StackTraceUtils.sanitize().
     *
     * @return An array of the stacktrace elements without internal Groovy entries.
     */
    private List<StackTraceElement> santizedStackTrace() {
        Throwable t = new Throwable()
        StackTraceUtils.sanitize(t)
        return t.getStackTrace().toList()
                .findAll({ !isGroovyMethodAsInnerClassCalledFromAbstractCallSite(it) })
    }

    /**
     * Decide whether the StackTraceElement belongs to an inner class that actually represents a method being called by AbstractCallSite
     *
     * @param e the StackTraceElement to be evaluated
     * @return the decision, if this is actually a method represented as an inner class
     */
    private boolean isGroovyMethodAsInnerClassCalledFromAbstractCallSite(StackTraceElement e) {
        String methodName = e.methodName
        String className = e.className
        return (methodName.equals("callStatic") || methodName.equals("call") )  && className.matches(METHOD_AS_INNER_CLASS_PATTERN)
    }

    /**
     * Remove the $_methodName_closure1 String part from a Groovy closure class name.
     *
     * Groovy closures are represented as inner classes. This method extracts the actual class name for these cases.
     *
     * @param referenceClassName The `referenceClass` for which we want to search the caller method in the current
     *                           thread stacktrace
     * @return The sanitized class name
     */
    String sanitizeClassName(String referenceClassName) {
        return referenceClassName.replaceAll('\\$_.*_closure\\d+', "")
    }
}
