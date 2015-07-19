package com.koenv.realmpojo.processor;

import com.koenv.realmpojo.runtime.RealmPojoUtil;
import com.koenv.realmpojo.runtime.Realmable;
import io.realm.Realm;

import java.util.ArrayList;
import java.util.List;

public class DefaultListModel implements Realmable<RealmListModel>, ListModel {
    private final String name;
    private final List<DefaultSimpleModel> models;

    private DefaultSimpleModel(Builder builder) {
        this.name = builder.name;
        this.models = builder.models == null ? new ArrayList<DefaultSimpleModel>() : builder.models;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<DefaultSimpleModel> getModels() {
        return models;
    }

    @Override
    public RealmListModel toRealmObject(Realm realm) {
        RealmListModel realmModel = new RealmListModel();
        realmModel.setName(name);
        realmModel.setModels(RealmPojoUtil.<DefaultSimpleModel, RealmSimpleModel>toRealmList(realm, models));
        return realm.copyToRealmOrUpdate(realmModel);
    }

    public static DefaultListModel fromRealmObject(RealmListModel model) {
        Builder builder = DefaultListModel.newBuilder();
        builder.name(model.getName());
        if (model.getModels() == null) {
            builder.models(new ArrayList<DefaultSimpleModel>());
        } else {
            ArrayList<DefaultSimpleModel> models = new ArrayList<DefaultSimpleModel>();
            for (RealmSimpleModel nestedModel : model.getModels()) {
                models.add(DefaultSimpleModel.fromRealmObject(nestedModel));
            }
            builder.models(models);
        }
        return builder.build();
    }

    public static DefaultListModel getDefaultModel(ListModel model) {
        if (model instanceof DefaultListModel) {
            return (DefaultListModel) model;
        }
        if (model instanceof RealmListModel) {
            return fromRealmObject((RealmListModel) model);
        }
        throw new IllegalArgumentException("Invalid ListModel class: \"" + model.getClass().getName() + "\"");
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private List<DefaultSimpleModel> models;

        private Builder() {

        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder models(List<DefaultSimpleModel> models) {
            this.models = models;
            return this;
        }

        public DefaultListModel build() {
            return new DefaultListModel(this);
        }
    }
}