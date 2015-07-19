package com.koenv.realmpojo.processor;

import com.koenv.realmpojo.annotations.RealmPojo;

import java.util.List;

@RealmPojo
public interface ListModel {
    @RealmPojo.PrimaryKey
    String getName();

    List<? extends SimpleModel> getModels();
}