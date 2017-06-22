/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.webis.chatnoir2.webclient.util;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dynamically load classes by their annotations and create instances at runtime.
 */
public class AnnotationClassLoader
{
    /**
     * Dynamically create a single instance of a class annotated with a specific {@link Annotation}.
     * If multiple matching classes exist, only the first one encountered will be returned.
     *
     * @param packagePath package path the class can be found in as String
     * @param matchValue value() property of {@link Annotation} must match (null if annotation has no value())
     * @param annotationType annotation class object
     * @param castType return object cast type class
     * @param <A> annotation type to match
     * @param <T> instance return type
     * @return instance of a matching class or null if no matching class could be found
     */
    public static <A extends Annotation, T> T newInstance(String packagePath, String matchValue, Class<A> annotationType,
                                                                Class<T> castType)
    {
        final List<T> instances = newInstances(packagePath, matchValue, annotationType, castType, 1);
        if (instances.size() > 0) {
            return instances.get(0);
        }

        return null;
    }

    /**
     * Dynamically create instances of classes annotated with a specific {@link Annotation}.
     *
     * @param packagePath package path the class can be found in as String
     * @param matchValue value() property of {@link Annotation} must match (null if annotation has no value())
     * @param annotationType annotation class object
     * @param castType return object cast type class
     * @param <A> annotation type to match
     * @param <T> instance return type
     * @return list of instances of a matching class
     */
    public static <A extends Annotation, T> List<T> newInstances(String packagePath, String matchValue,
                                                                 Class<A> annotationType, Class<T> castType)
    {
        return newInstances(packagePath, matchValue, annotationType, castType, 0);
    }

    /**
     * Dynamically create a limited number of instances of classes annotated with a specific {@link Annotation}.
     *
     * @param packagePath package path the class can be found in as String
     * @param matchValue value() property of {@link Annotation} must match (null if annotation has no value())
     * @param annotationType annotation class object
     * @param castType return object cast type class
     * @param numInstances maximum number of matching instances to return (0 means no limit)
     * @param <A> annotation type to match
     * @param <T> instance return type
     * @return list of instances of a matching class
     */
    @SuppressWarnings({"unchecked", "ClassExplicitlyAnnotation"})
    public static <A extends Annotation, T> List<T> newInstances(String packagePath, String matchValue,Class<A> annotationType,
                                                                 Class<T> castType, int numInstances)
    {
        final Reflections reflections = new Reflections(packagePath);
        final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(annotationType);

        List<T> instances = new ArrayList<>();
        for (final Class<?> apiModuleClass: annotated) {
            final A moduleAnnotation = apiModuleClass.getAnnotation(annotationType);

            try {
                if (!castType.isAssignableFrom(apiModuleClass)) {
                    continue;
                }

                if (null == matchValue) {
                    instances.add((T) apiModuleClass.getConstructor().newInstance());
                } else {
                    Method valueMethod = moduleAnnotation.annotationType().getMethod("value");
                    if (((String[]) valueMethod.invoke(moduleAnnotation))[0].equals(matchValue)) {
                        instances.add((T) apiModuleClass.getConstructor().newInstance());
                    }
                }

                if (numInstances > 0 && instances.size() >= numInstances) {
                    return instances;
                }
            } catch (NoSuchMethodException | ClassCastException | ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("Annotation " + annotationType.getName() +
                        " has no value() method which returns a String[] array");
            } catch (Exception ignored) {}
        }

        return instances;
    }
}
