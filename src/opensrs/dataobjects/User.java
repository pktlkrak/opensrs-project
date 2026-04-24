/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.dataobjects;

import java.util.function.BiConsumer;
import java.util.function.Function;
import opensrs.database.DataObject;
import opensrs.database.GenericParameter;
import opensrs.database.ParameterType;

/**
 *
 * @author piotr
 */
public class User extends DataObject<User.Parameter> {

    private int uid;
    private String name, hash, salt;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public User() {
    }

    @Override
    protected Object fetchObject(Parameter param) {
        throw new UnsupportedOperationException("No fetchable parameters present.");
    }

    @Override
    public Parameter getIdField() {
        return Parameter.UID;
    }

    public enum Parameter implements GenericParameter {
        UID((BiConsumer<User, Integer>) User::setUid, User::getUid, ParameterType.EQ_ONLY, false),
        NAME((BiConsumer<User, String>) User::setName, User::getName, ParameterType.EQ_ONLY, true),
        HASH((BiConsumer<User, String>) User::setHash, User::getHash, ParameterType.EQ_ONLY, true),
        SALT((BiConsumer<User, String>) User::setSalt, User::getSalt, ParameterType.EQ_ONLY, true);

        private final BiConsumer<User, ?> setter;
        private final Function<User, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<User, ?> setter, Function<User, ?> getter, ParameterType type, boolean insert) {
            this.setter = setter;
            this.getter = getter;
            this.type = type;
            this.insert = insert;
        }

        @Override
        public ParameterType getType() {
            return type;
        }

        @Override
        public BiConsumer<User, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<User, ?> getGetter() {
            return getter;
        }

        @Override
        public boolean insert() {
            return insert;
        }

    }
}
