package com.koenv.realmpojo.processor;

import javax.lang.model.type.TypeMirror;

public class FieldBinding {
    TypeMirror type;
    String name;
    boolean isPrimaryKey;
    boolean isIgnored;
    boolean isIndex;
    boolean isList;
}
