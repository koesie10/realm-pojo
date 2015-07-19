package com.koenv.realmpojo.processor;

import com.google.common.base.Defaults;
import com.koenv.realmpojo.runtime.RealmPojoUtil;
import com.koenv.realmpojo.runtime.Realmable;
import com.squareup.javapoet.*;
import io.realm.Realm;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class ImmutableClassGenerator {
    private ClassBinding classBinding;
    private ClassName interfaceClassName;
    private ClassName defaultClassName;
    private ClassName realmClassName;
    private ClassName builderName;

    public ImmutableClassGenerator(ClassBinding classBinding) {
        this.classBinding = classBinding;
        this.interfaceClassName = Util.getInterfaceClassName(classBinding.element);
        this.defaultClassName = Util.getDefaultClassName(interfaceClassName);
        this.realmClassName = Util.getRealmClassName(interfaceClassName);
        this.builderName = defaultClassName.nestedClass("Builder");
    }

    public JavaFile generate() {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(defaultClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Realmable.class), realmClassName))
                .addSuperinterface(interfaceClassName);

        TypeSpec.Builder builderBuilder = TypeSpec.classBuilder(builderName.simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        builderBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(builderName, "builder");

        MethodSpec.Builder realmableMethodBuilder = MethodSpec.methodBuilder("toRealmObject")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(Realm.class), "realm").build())
                .returns(realmClassName);

        addRealmableMethodBegin(realmableMethodBuilder);

        MethodSpec.Builder fromRealmObjectBuilder = MethodSpec.methodBuilder("fromRealmObject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(realmClassName, "model")
                .returns(defaultClassName);

        addFromRealmObjectMethodBegin(fromRealmObjectBuilder);

        for (FieldBinding field : classBinding.fields) {
            TypeName fieldType = ClassName.get(field.type);
            ClassName fieldClassName = null;
            ParameterizedTypeName arrayListType = null;

            if (field.isList) {
                fieldClassName = (ClassName) fieldType;
                ClassName listFieldClassName = ClassName.get(fieldClassName.packageName(), "Default" + fieldClassName.simpleName());
                fieldType = ParameterizedTypeName.get(ClassName.get(List.class), listFieldClassName);
                arrayListType = ParameterizedTypeName.get(ClassName.get(ArrayList.class), listFieldClassName);
            }

            addField(classBuilder, fieldType, field.name);
            addGetter(classBuilder, fieldType, field.name);
            addToBuilder(builderBuilder, fieldType, field.name);
            addToConstructor(constructorBuilder, field.isList, field.name, arrayListType);
            addToRealmableMethod(realmableMethodBuilder, field.type, field.isPrimaryKey, field.isList, field.name, fieldClassName);
            addToFromRealmObjectMethod(fromRealmObjectBuilder, field.name, field.isList, fieldClassName, arrayListType);
        }

        if (classBinding.updateFieldsIndividually) {
            realmableMethodBuilder.addStatement("return realmModel");
        } else {
            realmableMethodBuilder.addStatement("return realm.copyToRealmOrUpdate(realmModel)");
        }

        addBuilderEnd(builderBuilder);

        classBuilder.addType(builderBuilder.build());

        classBuilder.addMethod(realmableMethodBuilder.build());
        classBuilder.addMethod(constructorBuilder.build());

        addFromRealmObjectMethodEnd(fromRealmObjectBuilder);

        classBuilder.addMethod(fromRealmObjectBuilder.build());

        addGetDefaultModelMethod(classBuilder);

        addNewBuilderMethod(classBuilder);

        return JavaFile.builder(defaultClassName.packageName(), classBuilder.build()).skipJavaLangImports(true).build();
    }

    private void addGetDefaultModelMethod(TypeSpec.Builder builder) {
        CodeBlock codeBlock = CodeBlock.builder()
                .beginControlFlow("if (model instanceof $T)", defaultClassName)
                .addStatement("return ($T) model", defaultClassName)
                .endControlFlow()
                .beginControlFlow("if (model instanceof $T)", realmClassName)
                .addStatement("return fromRealmObject(($T) model)", realmClassName)
                .endControlFlow()
                .addStatement("throw new IllegalArgumentException(\"Invalid $T class: \\\"\" + model.getClass().getName() + \"\\\"\")", interfaceClassName)
                .build();

        MethodSpec getDefaultModelMethod = MethodSpec.methodBuilder("getDefaultModel")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(interfaceClassName, "model")
                .returns(defaultClassName)
                .addCode(codeBlock)
                .build();

        builder.addMethod(getDefaultModelMethod);
    }

    private void addBuilderEnd(TypeSpec.Builder builderBuilder) {
        MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(defaultClassName)
                .addStatement("return new $T(this)", defaultClassName)
                .build();

        builderBuilder.addMethod(buildMethod);
    }

    private void addNewBuilderMethod(TypeSpec.Builder builder) {
        MethodSpec newBuilderMethod = MethodSpec.methodBuilder("newBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderName)
                .addStatement("return new $T()", builderName)
                .build();

        builder.addMethod(newBuilderMethod);
    }

    private void addRealmableMethodBegin(MethodSpec.Builder builder) {
        if (classBinding.updateFieldsIndividually) {
            builder
                    .addStatement(
                            "$T realmModel = realm.where($T.class).equalTo($S, $L).findFirst()",
                            realmClassName,
                            realmClassName,
                            classBinding.primaryKeyField.name,
                            classBinding.primaryKeyField.name
                    )
                    .addCode(
                            CodeBlock.builder()
                                    .beginControlFlow("if (realmModel == null)")
                                    .addStatement("realmModel = realm.createObject($T.class)", realmClassName)
                                    .endControlFlow()
                                    .build()
                    );
        } else {
            builder.addStatement("$T realmModel = new $T()", realmClassName, realmClassName);
        }
    }

    private void addFromRealmObjectMethodBegin(MethodSpec.Builder builder) {
        builder.addStatement("$T builder = $T.newBuilder()", builderName, defaultClassName);
    }

    private void addFromRealmObjectMethodEnd(MethodSpec.Builder builder) {
        builder.addStatement("return builder.build()");
    }

    private void addField(TypeSpec.Builder builder, TypeName fieldType, String fieldName) {
        FieldSpec fieldSpec = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        builder.addField(fieldSpec);
    }

    private void addGetter(TypeSpec.Builder builder, TypeName fieldType, String fieldName) {
        MethodSpec getterSpec = MethodSpec.methodBuilder(Util.getGetterName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addAnnotation(Override.class)
                .addStatement("return $N", fieldName)
                .build();

        builder.addMethod(getterSpec);
    }

    private void addToBuilder(TypeSpec.Builder builder, TypeName fieldType, String fieldName) {
        FieldSpec builderField = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE)
                .build();

        MethodSpec builderMethod = MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .returns(builderName)
                .addStatement("this.$N = $L", fieldName, fieldName)
                .addStatement("return this")
                .build();

        builder.addField(builderField);
        builder.addMethod(builderMethod);
    }

    private void addToConstructor(MethodSpec.Builder builder, boolean isList, String fieldName, ParameterizedTypeName arrayListType) {
        if (isList) {
            builder.addStatement("this.$N = builder.$L == null ? new $T() : builder.$L", fieldName, fieldName, arrayListType, fieldName);
        } else {
            builder.addStatement("this.$N = builder.$L", fieldName, fieldName);
        }
    }

    private void addToRealmableMethod(MethodSpec.Builder builder, TypeMirror type, boolean isPrimaryKey, boolean isList, String fieldName, ClassName fieldClassName) {
        if (isPrimaryKey) {
            addRealmSetStatement(builder, fieldName);
        } else {
            CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
            if (classBinding.updateFieldsIndividually) {
                Class<?> defaultValueType;

                switch (type.getKind()) {
                    case INT:
                        defaultValueType = Integer.TYPE;
                        break;
                    case BYTE:
                        defaultValueType = Byte.TYPE;
                        break;
                    case CHAR:
                        defaultValueType = Character.TYPE;
                        break;
                    case DOUBLE:
                        defaultValueType = Double.TYPE;
                        break;
                    case FLOAT:
                        defaultValueType = Float.TYPE;
                        break;
                    case LONG:
                        defaultValueType = Long.TYPE;
                        break;
                    case SHORT:
                        defaultValueType = Short.TYPE;
                        break;
                    default:
                        defaultValueType = null;
                }

                codeBlockBuilder.beginControlFlow("if ($N != $L)", fieldName, Defaults.defaultValue(defaultValueType));
            }

            if (isList) {
                addRealmSetListStatement(codeBlockBuilder, fieldName, fieldClassName);
            } else {
                addRealmSetStatement(codeBlockBuilder, fieldName);
            }

            if (classBinding.updateFieldsIndividually) {
                codeBlockBuilder.endControlFlow();
            }

            builder.addCode(codeBlockBuilder.build());
        }
    }

    private void addToFromRealmObjectMethod(MethodSpec.Builder builder, String fieldName, boolean isList, ClassName fieldClassName, ParameterizedTypeName arrayListType) {
        if (isList) {
            ClassName defaultClassName = ClassName.get(fieldClassName.packageName(), "Default" + fieldClassName.simpleName());
            ClassName realmClassName = ClassName.get(fieldClassName.packageName(), "Realm" + fieldClassName.simpleName());
            builder.addCode(
                    CodeBlock.builder()
                            .beginControlFlow("if (model.$L() == null)", Util.getGetterName(fieldName))
                            .addStatement("builder.$L(new $T())", fieldName, arrayListType)
                            .nextControlFlow("else")
                            .addStatement("$T $L = new $T()", arrayListType, fieldName, arrayListType)
                            .beginControlFlow("for ($T nestedModel : model.$L())", realmClassName, Util.getGetterName(fieldName))
                            .addStatement("$L.add($T.fromRealmObject(nestedModel))", fieldName, defaultClassName)
                            .endControlFlow()
                            .addStatement("builder.$L($L)", fieldName, fieldName)
                            .endControlFlow()
                            .build()
            );
        } else {
            builder.addStatement("builder.$L(model.$L())", fieldName, Util.getGetterName(fieldName));
        }
    }

    private void addRealmSetStatement(MethodSpec.Builder builder, String fieldName) {
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        addRealmSetStatement(codeBlockBuilder, fieldName);
        builder.addCode(codeBlockBuilder.build());
    }

    private void addRealmSetStatement(CodeBlock.Builder builder, String fieldName) {
        builder.addStatement("realmModel.$L($N)", Util.getSetterName(fieldName), fieldName);
    }

    private void addRealmSetListStatement(CodeBlock.Builder builder, String fieldName, ClassName fieldClassName) {
        ClassName defaultClassName = ClassName.get(fieldClassName.packageName(), "Default" + fieldClassName.simpleName());
        ClassName realmClassName = ClassName.get(fieldClassName.packageName(), "Realm" + fieldClassName.simpleName());
        builder.addStatement("realmModel.$L($T.<$T, $T>toRealmList(realm, $L))", Util.getSetterName(fieldName), ClassName.get(RealmPojoUtil.class), defaultClassName, realmClassName, fieldName);
    }
}
