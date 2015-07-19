package com.koenv.realmpojo.runtime;

import io.realm.Realm;
import io.realm.RealmObject;

public interface Realmable<T extends RealmObject> {
    T toRealmObject(Realm realm);
}
