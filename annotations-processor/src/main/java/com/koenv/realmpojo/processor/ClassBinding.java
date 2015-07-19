package com.koenv.realmpojo.processor;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class ClassBinding {
    TypeElement element;
    List<FieldBinding> fields;
    boolean updateFieldsIndividually;
    FieldBinding primaryKeyField;
}
