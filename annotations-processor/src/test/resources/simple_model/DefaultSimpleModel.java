package com.koenv.realmpojo.processor;

import com.koenv.realmpojo.runtime.Realmable;
import io.realm.Realm;

public class DefaultSimpleModel implements Realmable<RealmSimpleModel>, SimpleModel {
    private final String name;
    private final int age;

    private DefaultSimpleModel(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public RealmSimpleModel toRealmObject(Realm realm) {
        RealmSimpleModel realmModel = new RealmSimpleModel();

        realmModel.setName(name);
        realmModel.setAge(age);

        return realm.copyToRealmOrUpdate(realmModel);
    }

    public static DefaultSimpleModel fromRealmObject(RealmSimpleModel model) {
        Builder builder = DefaultSimpleModel.newBuilder();
        builder.name(model.getName());
        builder.age(model.getAge());

        return builder.build();
    }

    public static DefaultSimpleModel getDefaultModel(SimpleModel model) {
        if (model instanceof DefaultSimpleModel) {
            return (DefaultSimpleModel) model;
        }
        if (model instanceof RealmSimpleModel) {
            return fromRealmObject((RealmSimpleModel) model);
        }
        throw new IllegalArgumentException("Invalid SimpleModel class: \"" + model.getClass().getName() + "\"");
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private int age;

        private Builder() {

        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public DefaultSimpleModel build() {
            return new DefaultSimpleModel(this);
        }
    }
}