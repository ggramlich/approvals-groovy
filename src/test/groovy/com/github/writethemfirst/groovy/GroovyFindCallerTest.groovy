package com.github.writethemfirst.groovy

import static org.assertj.core.api.Assertions.assertThat

import java.util.stream.Stream

import org.junit.Test

import groovy.transform.TypeChecked

@TypeChecked
class GroovyFindCallerTest  {
    private GroovyFindCaller find = new GroovyFindCaller()

    @Test
    void guessedClassShouldBeTestClass() {
        final String guessed = find.callerClass(GroovyFindCaller.class)
        assertThat(guessed).isEqualTo(getClass().getName())
    }

    @Test
    void methodNameShouldBeEmpty() {
        assertThat(find.callerMethod(String.class.getName())).isEmpty()
    }

    @Test
    void methodNameShouldBeTheMethodName() {
        assertThat(find.callerMethod(getClass().getName())).contains("methodNameShouldBeTheMethodName")
    }

    @Test
    void callerClassShouldExcludeInnerClassStatic() {
        StaticUtils.find = find
        assertThat(StaticUtils.callFinder()).isEqualTo(getClass().getName())
    }

    static class StaticUtils {
        static GroovyFindCaller find
        static String callFinder(){
            return find.callerClass(GroovyFindCaller.class, StaticUtils.class)
        }
    }

    @Test
    void callerClassShouldExcludeInnerClass() {
        def utils = new Utils(find)
        assertThat(utils.callFinder()).isEqualTo(getClass().getName())
    }

    static class Utils {
        private GroovyFindCaller find
        public Utils(GroovyFindCaller find) {
            this.find = find
        }
        String callFinder(){
            return find.callerClass(GroovyFindCaller.class, Utils.class)
        }
    }

    @Test
    void sanitizeClassNameInClosure() {
        String className = getClass().getName()
        Stream.of("whatever")
                .forEach({s -> assertThat(find.sanitizeClassName(getClass().getName())).isEqualTo(className)})
    }

    @Test
    void methodNameShouldNotBeClosure() {
        Stream.of("whatever")
                .forEach({s -> assertThat(find.callerMethod(getClass().getName())).contains("methodNameShouldNotBeClosure")})
    }
}
