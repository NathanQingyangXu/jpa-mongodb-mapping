/*
 *
 * Copyright 2008-present MongoDB, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hibernate.omm.annotation;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("org.hibernate.omm.annotation.InterfaceAdapters")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InterfaceAdaptersProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        for (var element : roundEnvironment.getElementsAnnotatedWith(InterfaceAdapters.class)) {
            var interfaceAdapters = element.getAnnotation(InterfaceAdapters.class);
            for (var interfaceAdapter : interfaceAdapters.value()) {
                try {
                    Class<?> interfaceClass = Class.forName(interfaceAdapter.interfaceName());
                    if (!interfaceClass.isInterface()) {
                        throw new IllegalArgumentException("non-interface class found: " + interfaceClass.getName());
                    }
                    String adapterClassName = interfaceAdapter.adapterClassName();
                    generateAdapterClass(adapterClassName, interfaceClass, interfaceAdapter.overrideDeclaredMethodsOnly());
                } catch (ClassNotFoundException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    private void generateAdapterClass(String adapterClassName, Class<?> interfaceClass, boolean overrideDeclaringMethodsOnly) throws IOException {
        JavaFileObject builderFile = filer.createSourceFile(adapterClassName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            int lastDot = adapterClassName.lastIndexOf('.');
            if (lastDot < 0) {
                throw new IllegalArgumentException("no package found for the adapter class: " + adapterClassName);
            }
            String packageName = adapterClassName.substring(0, lastDot);
            String simpleClassName = adapterClassName.substring(lastDot + 1);
            out.print("package ");
            out.print(packageName);
            out.println(";");
            out.println();
            out.print("public interface ");
            out.print(simpleClassName);
            out.print(" extends ");
            out.print(interfaceClass.getName());
            out.println(" {");
            for (Method method : (overrideDeclaringMethodsOnly? interfaceClass.getDeclaredMethods() : interfaceClass.getMethods())) {
                out.print("    @Override");
                out.println();
                out.print("    default ");
                out.print(getTypeRendered(method.getReturnType()));
                out.print(" ");
                out.print(method.getName());
                out.print("(");
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length > 0) {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (i > 0) {
                            out.print(", ");
                        }
                        out.print(getTypeRendered(parameterTypes[i]) + " p" + i);
                    }
                }
                out.print(")");
                Class<?>[] exceptionTypes = method.getExceptionTypes();
                if (exceptionTypes.length > 0) {
                    out.print(" throws ");
                    for (int i = 0; i < exceptionTypes.length; i++) {
                        if (i > 0) {
                            out.print(", ");
                        }
                        out.print(exceptionTypes[i].getName());
                    }
                }
                out.printf(" { throw new RuntimeException(\"'%s#%s()' method not supported\"); }%n", interfaceClass.getName(),
                        method.getName());
                out.println();
            }
            out.println("}");
        }
    }

    private String getTypeRendered(final Class<?> clazz) {
        var componentType = clazz.getComponentType();
        if (componentType == null) {
            return clazz.getName().replace('$', '.'); // inner class name
        } else {
            return componentType.getCanonicalName() + "[]";
        }
    }
}
