package com.koenv.realmpojo.processor;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class RealmSimpleModel extends RealmObject implements SimpleModel {
    @PrimaryKey
    private String name;
    private int age;

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
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}