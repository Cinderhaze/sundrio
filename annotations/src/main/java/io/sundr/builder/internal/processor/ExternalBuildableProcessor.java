/*
 * Copyright 2015 The original authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.sundr.builder.internal.processor;

import io.sundr.Function;
import io.sundr.builder.annotations.ExternalBuildables;
import io.sundr.builder.internal.ExternalRepository;
import io.sundr.builder.internal.functions.ClazzAs;
import io.sundr.builder.internal.functions.overrides.ToBuildableJavaProperty;
import io.sundr.builder.internal.functions.overrides.ToBuildableJavaType;
import io.sundr.codegen.coverters.JavaClazzFunction;
import io.sundr.codegen.coverters.JavaMethodFunction;
import io.sundr.codegen.model.JavaClazz;
import io.sundr.codegen.model.JavaProperty;
import io.sundr.codegen.model.JavaType;
import io.sundr.codegen.processor.JavaGeneratingProcessor;
import io.sundr.codegen.utils.ModelUtils;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("io.sundr.builder.annotations.ExternalBuildables")
public class ExternalBuildableProcessor extends JavaGeneratingProcessor {

    public static final String DEFAULT_FLUENT_TEMPLATE_LOCATION = "templates/builder/fluent.vm";
    public static final String DEFAULT_BUILDER_TEMPLATE_LOCATION = "templates/builder/builder.vm";


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Elements elements = processingEnv.getElementUtils();
        Function<String, JavaType> toType = new ToBuildableJavaType(elements);
        Function<VariableElement, JavaProperty> toProperty = new ToBuildableJavaProperty(toType);
        JavaMethodFunction toMethod = new JavaMethodFunction(toType, toProperty);
        JavaClazzFunction toClazz = new JavaClazzFunction(elements, toType, toMethod, toProperty);

        //First pass register all externals
        for (TypeElement ann : annotations) {
            for (Element element : env.getElementsAnnotatedWith(ann)) {
                ExternalBuildables generated = element.getAnnotation(ExternalBuildables.class);
                for (String name : generated.value()) {
                    ExternalRepository.register(name);
                }
            }
        }

        for (TypeElement typeElement : annotations) {
            for (Element element : env.getElementsAnnotatedWith(typeElement)) {
                ExternalBuildables generated = element.getAnnotation(ExternalBuildables.class);
                for (String name : generated.value()) {
                    JavaClazz clazz = toClazz.apply(ModelUtils.getClassElement(elements.getTypeElement(name)));
                    try {
                        generateFromClazz(ClazzAs.BUILDER.apply(clazz),
                                processingEnv,
                                DEFAULT_BUILDER_TEMPLATE_LOCATION);

                        generateFromClazz(ClazzAs.FLUENT.apply(clazz),
                                processingEnv,
                                DEFAULT_FLUENT_TEMPLATE_LOCATION);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }
}