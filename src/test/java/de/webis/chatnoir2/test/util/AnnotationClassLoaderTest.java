/*
 * ChatNoir 2 Web frontend test suite.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.test.util;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import de.webis.chatnoir2.webclient.util.AnnotationClassLoader;

public class AnnotationClassLoaderTest
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface LoaderTestAnnotation
    {
        String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface LoaderTestAnnotationWithoutValue {}

    @LoaderTestAnnotation()
    public static class LoaderTestClass1 {
        public LoaderTestClass1()  {}
    }

    @LoaderTestAnnotation("foo")
    public static class LoaderTestClass2 {
        public LoaderTestClass2()  {}
    }

    @LoaderTestAnnotationWithoutValue
    public static class LoaderTestClass3 {
        public LoaderTestClass3()  {}
    }

    @SuppressWarnings("unused")
    @LoaderTestAnnotation
    public static class LoaderTestClass4 extends LoaderTestClass1 {
        public LoaderTestClass4()  {}
    }

    @Test
    public void testNewInstance()
    {
        LoaderTestClass1 instance = AnnotationClassLoader.newInstance(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                null,
                LoaderTestAnnotation.class,
                LoaderTestClass1.class);

        assertThat(instance, instanceOf(LoaderTestClass1.class));
    }

    @Test
    public void testNewInstanceWithValue()
    {
        LoaderTestClass2 instance = AnnotationClassLoader.newInstance(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                "foo",
                LoaderTestAnnotation.class,
                LoaderTestClass2.class);

        assertThat(instance, instanceOf(LoaderTestClass2.class));
    }

    @Test(expected = RuntimeException.class)
    public void testNewInstanceWithValueException()
    {
        AnnotationClassLoader.newInstance(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                "foo",
                LoaderTestAnnotationWithoutValue.class,
                LoaderTestClass3.class);
    }

    @Test
    public void testNewInstanceWithValueEmpty()
    {
        LoaderTestClass2 instance = AnnotationClassLoader.newInstance(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                "foobar",
                LoaderTestAnnotation.class,
                LoaderTestClass2.class);

        assertThat(instance, nullValue());
    }

    @Test
    public void testNewInstances()
    {
        List<LoaderTestClass2> instances = AnnotationClassLoader.newInstances(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                null,
                LoaderTestAnnotation.class,
                LoaderTestClass2.class);

        assertThat("Wrong number of instances created", instances.size(), is(1));
    }

    @Test
    public void testNewInstancesWithSubclasses()
    {
        List<LoaderTestClass1> instances = AnnotationClassLoader.newInstances(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                null,
                LoaderTestAnnotation.class,
                LoaderTestClass1.class);

        assertThat("Wrong number of instances created", instances.size(), is(2));
    }

    @Test
    public void testNewInstancesWithSubclassesRestricted()
    {
        List<LoaderTestClass1> instances = AnnotationClassLoader.newInstances(
                "de.webis.chatnoir2.test.util.AnnotationClassLoaderTest",
                null,
                LoaderTestAnnotation.class,
                LoaderTestClass1.class,
                1);

        assertThat("Wrong number of instances created", instances.size(), is(1));
    }
}
