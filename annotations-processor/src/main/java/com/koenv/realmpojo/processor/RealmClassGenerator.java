package com.koenv.realmpojo.processor;

import com.squareup.javapoet.*;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

import javax.lang.model.element.Modifier;

public class RealmClassGenerator {
    private ClassBinding classBinding;
    private ClassName interfaceClassName;
    private ClassName realmClassName;

    public RealmClassGenerator(ClassBinding classBinding) {
        this.classBinding = classBinding;
        this.interfaceClassName = Util.getInterfaceClassName(classBinding.element);
        this.realmClassName = Util.getRealmClassName(interfaceClassName);
    }

    public JavaFile generate() {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(realmClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(RealmObject.class))
                .addSuperinterface(interfaceClassName)
                .addAnnotation(ClassName.get(RealmClass.class))
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());

        for (FieldBinding field : classBinding.fields) {
            TypeName fieldType = ClassName.get(field.type);

            if (field.isList) {
                ClassName fieldClassName = (ClassName) fieldType;
                fieldType = ParameterizedTypeName.get(ClassName.get(RealmList.class), ClassName.get(fieldClassName.packageName(), "Realm" + fieldClassName.simpleName()));
            }

            addField(classBuilder, fieldType, field.name, field.isPrimaryKey, field.isIgnored);
            addGetter(classBuilder, fieldType, field.name);
            addSetter(classBuilder, fieldType, field.name);
        }

        return JavaFile.builder(realmClassName.packageName(), classBuilder.build()).skipJavaLangImports(true).build();
    }

    private void addField(TypeSpec.Builder builder, TypeName fieldType, String fieldName, boolean isPrimaryKey, boolean isIgnored) {
        FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE);

        if (isPrimaryKey) {
            fieldSpecBuilder.addAnnotation(ClassName.get(PrimaryKey.class));
        }

        if (isIgnored) {
            fieldSpecBuilder.addAnnotation(ClassName.get(Ignore.class));
        }

        FieldSpec fieldSpec = fieldSpecBuilder.build();

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

    private void addSetter(TypeSpec.Builder builder, TypeName fieldType, String fieldName) {
        MethodSpec setterSpec = MethodSpec.methodBuilder(Util.getSetterName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$N = $L", fieldName, fieldName)
                .build();

        builder.addMethod(setterSpec);
    }
}
