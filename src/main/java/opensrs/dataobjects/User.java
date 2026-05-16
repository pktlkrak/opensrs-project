/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.dataobjects;

import java.util.function.BiConsumer;
import java.util.function.Function;
import opensrs.Utils;
import opensrs.database.DataObject;
import opensrs.database.GenericParameter;
import opensrs.database.ParameterType;

/**
 * {@link DataObject} for representing a result.
 * <p>A user has an UID and login information and allows multiple people to utilise the same SRS and track their progress separately because the progress they make is tied to their UID.</p>
 * @author piotr
 */
public class User extends DataObject<User.Parameter> {
    
    public static User fromPassword(String username, String password) {
        User newUser = new User();
        newUser.setName(username);
        newUser.setSalt(Utils.randStr(20, true));
        newUser.setHash(Utils.getSHA256Hash((password + ":" + newUser.getSalt()).getBytes()));
        return newUser;
    }

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
        /**
         * UID of the user. Used to match the user to his entries in the database.
         */
        UID((BiConsumer<User, Integer>) User::setUid, User::getUid, ParameterType.EQ_ONLY, false),
        
        /**
         * The username.
         */
        NAME((BiConsumer<User, String>) User::setName, User::getName, ParameterType.EQ_ONLY, true),
        
        /**
         * The hash for the password.
         */
        HASH((BiConsumer<User, String>) User::setHash, User::getHash, ParameterType.EQ_ONLY, true),
        
        /**
         * The salt for the password.
         */
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
