package com.koenv.realmpojo.processor;

import com.koenv.realmpojo.annotations.RealmPojo;
import com.squareup.javapoet.*;
import io.realm.annotations.RealmModule;
import io.realm.processor.Constants;
import io.realm.processor.RealmProcessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes("com.koenv.realmpojo.annotations.RealmPojo")
public class RealmPojoProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    private DeclaredType list;
    private List<TypeMirror> validPrimaryKeyTypes;
    private List<TypeMirror> validIndexTypes;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();

        TypeMirror stringType = processingEnv.getElementUtils().getTypeElement(String.class.getName()).asType();
        list = typeUtils.getDeclaredType(processingEnv.getElementUtils().getTypeElement(List.class.getName()), typeUtils.getWildcardType(null, null));
        validPrimaryKeyTypes = Arrays.asList(
                stringType,
                typeUtils.getPrimitiveType(TypeKind.SHORT),
                typeUtils.getPrimitiveType(TypeKind.INT),
                typeUtils.getPrimitiveType(TypeKind.LONG)
        );
        validIndexTypes = new ArrayList<>(validPrimaryKeyTypes);
        validIndexTypes.addAll(
                Arrays.asList(
                        typeUtils.getPrimitiveType(TypeKind.BYTE),
                        typeUtils.getPrimitiveType(TypeKind.BOOLEAN),
                        processingEnv.getElementUtils().getTypeElement(Byte.class.getName()).asType(),
                        processingEnv.getElementUtils().getTypeElement(Short.class.getName()).asType(),
                        processingEnv.getElementUtils().getTypeElement(Integer.class.getName()).asType(),
                        processingEnv.getElementUtils().getTypeElement(Long.class.getName()).asType(),
                        processingEnv.getElementUtils().getTypeElement(Date.class.getName()).asType(),
                        processingEnv.getElementUtils().getTypeElement(Boolean.class.getName()).asType()
                )
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(RealmPojo.class)) {
            if (!annotatedElement.getKind().isInterface()) {
                error(annotatedElement, "Only interfaces can be annotated with %s", RealmPojo.class.getSimpleName());
                return false;
            }
            if (!annotatedElement.getEnclosingElement().getKind().equals(ElementKind.PACKAGE)) {
                error(annotatedElement, "Only top-level clases are supported, failed for %s", annotatedElement.getSimpleName());
                return false;
            }

            RealmPojo annotation = annotatedElement.getAnnotation(RealmPojo.class);

            TypeElement typeElement = (TypeElement) annotatedElement;

            ClassBinding classBinding = new ClassBinding();
            classBinding.element = typeElement;
            classBinding.fields = new ArrayList<>();
            classBinding.updateFieldsIndividually = annotation.updateFieldsIndividually();

            boolean foundPrimaryKey = false;

            // walk up the interfaces finding the fields
            List<TypeElement> nextClasses = new ArrayList<>(Collections.singletonList(typeElement));
            while (nextClasses.size() > 0) {
                List<TypeElement> workingClasses = new ArrayList<>(nextClasses);
                nextClasses.clear();
                for (TypeElement working : workingClasses) {
                    for (Element element : working.getEnclosedElements()) {
                        if (element.getKind().equals(ElementKind.METHOD)) {
                            FieldBinding fieldBinding = new FieldBinding();

                            ExecutableElement executableElement = (ExecutableElement) element;
                            if (!executableElement.getSimpleName().toString().startsWith("get")) {
                                error(
                                        executableElement,
                                        "All method names must start with get, failed for %s in class %s",
                                        executableElement.getSimpleName().toString(),
                                        typeElement.getSimpleName().toString()
                                );
                                return false;
                            }
                            if (executableElement.getParameters().size() > 0) {
                                error(
                                        executableElement,
                                        "Methods must not take any parameters, failed for %s in class %s",
                                        executableElement.getSimpleName().toString(),
                                        typeElement.getSimpleName().toString()
                                );
                                return false;
                            }

                            fieldBinding.name = Util.getFieldName(executableElement.getSimpleName().toString());

                            boolean isPrimaryKey = executableElement.getAnnotation(RealmPojo.PrimaryKey.class) != null;
                            if (isPrimaryKey && foundPrimaryKey) {
                                error(
                                        executableElement,
                                        "Realm POJOs must have only one primary key, failed for %s",
                                        typeElement.getSimpleName().toString()
                                );
                                return false;
                            } else if (isPrimaryKey) {
                                if (!isValidPrimaryKeyType(executableElement.getReturnType())) {
                                    error(
                                            executableElement,
                                            "%s is not a valid primary key type, failed for %s",
                                            executableElement.getReturnType().toString(),
                                            executableElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                foundPrimaryKey = true;
                                classBinding.primaryKeyField = fieldBinding;
                            }
                            fieldBinding.isPrimaryKey = isPrimaryKey;

                            fieldBinding.isIndex = executableElement.getAnnotation(RealmPojo.Index.class) != null;

                            if (fieldBinding.isIndex) {
                                if (!isValidIndexType(executableElement.getReturnType())) {
                                    error(
                                            executableElement,
                                            "%s is not a valid index type, failed for %s",
                                            executableElement.getReturnType().toString(),
                                            executableElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                            }

                            fieldBinding.isIgnored = executableElement.getAnnotation(RealmPojo.Ignore.class) != null;

                            if ((fieldBinding.isPrimaryKey || fieldBinding.isIndex) && fieldBinding.isIgnored) {
                                error(
                                        executableElement,
                                        "A primary key or index cannot be ignored, failed for %s",
                                        typeElement.getSimpleName().toString()
                                );
                                return false;
                            }

                            TypeMirror fieldType = executableElement.getReturnType();

                            if (fieldType.getKind() == TypeKind.VOID) {
                                error(
                                        executableElement,
                                        "Methods may not return void, failed for %s in %s",
                                        executableElement.getSimpleName().toString(),
                                        typeElement.getSimpleName().toString()
                                );
                                return false;
                            }

                            fieldBinding.type = fieldType;

                            if (typeUtils.isAssignable(fieldType, list)) {
                                List<? extends TypeMirror> typeArguments = ((DeclaredType) fieldType).getTypeArguments();
                                if (typeArguments.size() == 0) {
                                    error(
                                            executableElement,
                                            "Methods must return a list with a type, failed for %s in %s",
                                            executableElement.getSimpleName().toString(),
                                            typeElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                if (!(typeArguments.get(0) instanceof WildcardType)) {
                                    error(
                                            executableElement,
                                            "Methods must return a list with a type in an extends bound, failed for %s in %s",
                                            executableElement.getSimpleName().toString(),
                                            typeElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                WildcardType type = (WildcardType) typeArguments.get(0);
                                if (type.getExtendsBound() == null || type.getSuperBound() != null) {
                                    error(
                                            executableElement,
                                            "Methods must return a list with a type in an extends bound, failed for %s in %s",
                                            executableElement.getSimpleName().toString(),
                                            typeElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                Element listElement = typeUtils.asElement(type.getExtendsBound());
                                if (!listElement.getKind().isInterface()) {
                                    error(
                                            executableElement,
                                            "Methods must return an interface in a list, failed for %s in %s",
                                            executableElement.getSimpleName().toString(),
                                            typeElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                if (listElement.getAnnotation(RealmPojo.class) == null) {
                                    error(
                                            executableElement,
                                            "Methods must return a RealmPojo interface in a list, failed for %s in %s",
                                            executableElement.getSimpleName().toString(),
                                            typeElement.getSimpleName().toString()
                                    );
                                    return false;
                                }
                                fieldBinding.isList = true;
                                fieldBinding.type = type.getExtendsBound();
                            }

                            classBinding.fields.add(fieldBinding);
                        }
                    }
                    for (TypeMirror typeMirror : working.getInterfaces()) {
                        nextClasses.add((TypeElement) typeUtils.asElement(typeMirror));
                    }
                }
            }

            if (classBinding.fields.isEmpty()) {
                error(
                        typeElement,
                        "Every interface annotated with %s must have at least one getter, failed for %s",
                        RealmPojo.class.getSimpleName(),
                        typeElement.getSimpleName().toString()
                );
                return false;
            }

            if (!foundPrimaryKey) {
                error(
                        typeElement,
                        "Every interface annotated with %s must have one field annotated with %s, failed for %s",
                        RealmPojo.class.getSimpleName(),
                        RealmPojo.PrimaryKey.class.getSimpleName(),
                        typeElement.getSimpleName().toString()
                );
            }

            ImmutableClassGenerator immutableClassGenerator = new ImmutableClassGenerator(classBinding);

            JavaFile defaultClass = immutableClassGenerator.generate();

            try {
                defaultClass.writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Code gen failed: " + e);
                return false;
            }

            RealmClassGenerator realmClassGenerator = new RealmClassGenerator(classBinding);

            JavaFile realmClass = realmClassGenerator.generate();

            try {
                realmClass.writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Code gen failed: " + e);
                return false;
            }
        }

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e
        );
    }

    private boolean isValidPrimaryKeyType(TypeMirror type) {
        for (TypeMirror validType : validPrimaryKeyTypes) {
            if (typeUtils.isAssignable(type, validType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidIndexType(TypeMirror type) {
        for (TypeMirror validType : validIndexTypes) {
            if (typeUtils.isAssignable(type, validType)) {
                return true;
            }
        }
        return false;
    }
}
