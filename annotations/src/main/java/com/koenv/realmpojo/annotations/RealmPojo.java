package com.koenv.realmpojo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RealmPojo {
    boolean updateFieldsIndividually() default false;

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    @interface PrimaryKey {

    }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    @interface Ignore {

    }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    @interface Index {

    }
}
