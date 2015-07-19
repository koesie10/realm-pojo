Realm POJO
============

This were to be an annotation processor to automatically create 2 classes of an interface, one which extends 
[RealmObject](https://realm.io/docs/java/latest/api/io/realm/RealmObject.html) and one immutable form. Unfortunately,
the Realm annotation processor only runs in the first round, while the Realm classes generated in this processor are also
created in the first round. For this to work, the Realm processor would need to look for the classes in all rounds.

**This means this project won't work!**

If this project were to work though, it would be used like this:

Create an interface with only getters and a `@RealmPojo` annotation and a `@RealmPojo.PrimaryKey` annotation on one
field, which is legal as a primary key in Realm:
```java
@RealmPojo
public interface User {
    @RealmPojo.PrimaryKey
    int getId();
    
    String getName();
    String getPassword();
}
```

This will then create the following classes (only outlines shown here):

* DefaultUser implement User, Realmable<RealmUser>
    * getId()
    * getName()
    * getPassword()
    * toRealmObject(Realm realm)
    * static fromRealmObject(RealmUser model)
    * static getDefaultModel(User model)
    * static newBuilder()
    * Builder
        * id(int id)
        * name(String name)
        * password(String password)
        * build
* RealmUser extends RealmObject implements User
    * getId()
    * setId(int id)
    * getName()
    * setName(String name)
    * getPassword()
    * setPassword(String password)