package com.koenv.realmpojo.processor;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.TypeElement;

public class Util {
    static ClassName getDefaultClassName(ClassName interfaceClassName) {
        return ClassName.get(interfaceClassName.packageName(), "Default" + interfaceClassName.simpleName());
    }

    static ClassName getRealmClassName(ClassName interfaceClassName) {
        return ClassName.get(interfaceClassName.packageName(), "Realm" + interfaceClassName.simpleName());
    }

    static ClassName getInterfaceClassName(TypeElement element) {
        return ClassName.get(element);
    }

    static String getFieldName(String name) {
        name = name.substring(3);
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    static String getGetterName(String name) {
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    static String getSetterName(String name) {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
