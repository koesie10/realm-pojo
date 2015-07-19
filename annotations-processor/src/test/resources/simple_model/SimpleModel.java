package com.koenv.realmpojo.processor;

import com.koenv.realmpojo.annotations.RealmPojo;

@RealmPojo(updateFieldsIndividually = false)
public interface SimpleModel {
    @RealmPojo.PrimaryKey
    String getName();

    int getAge();
}