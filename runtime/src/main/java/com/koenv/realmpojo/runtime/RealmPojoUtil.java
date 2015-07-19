package com.koenv.realmpojo.runtime;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

import java.util.List;

public class RealmPojoUtil {
    @SuppressWarnings("unchecked")
    public static <T extends Realmable> void toRealmList(Realm realm, RealmList realmList, List<T> list) {
        for (T object : list) {
            realmList.add(object.toRealmObject(realm));
        }
    }

    public static <T extends Realmable<P>, P extends RealmObject> RealmList<P> toRealmList(Realm realm, List<T> list) {
        RealmList<P> realmList = new RealmList<>();
        if (list != null) {
            for (T object : list) {
                realmList.add(object.toRealmObject(realm));
            }
        }
        return realmList;
    }
}
