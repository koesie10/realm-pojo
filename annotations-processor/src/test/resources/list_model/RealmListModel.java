package com.koenv.realmpojo.processor;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class RealmListModel extends RealmObject implements ListModel {
    @PrimaryKey
    private String name;
    private RealmList<RealmSimpleModel> models;

    public RealmSimpleModel() {

    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public RealmList<RealmSimpleModel> getModels() {
        return models;
    }

    public void setModels(RealmList<RealmSimpleModel> models) {
        this.models = models
    }
}