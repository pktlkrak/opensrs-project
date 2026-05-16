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
 * TODO: i don't get it
 * @author piotr
 */
public class SRSObjectState extends DataObject<SRSObjectState.Parameter>{

    private int setCode, uid;
    private int itemSUID;
    private int weight;

    public SRSObjectState() {
    }

    public int getSetCode() {
        return setCode;
    }

    public void setSetCode(int setCode) {
        this.setCode = setCode;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getItemSUID() {
        return itemSUID;
    }

    public void setItemSUID(int itemSUID) {
        this.itemSUID = itemSUID;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    protected Object fetchObject(Parameter param) {
        return null;
    }

    @Override
    public Parameter getIdField() {
        return Parameter.ITEM_SUID;
    }

    public enum Parameter implements GenericParameter {
        UID((BiConsumer<SRSObjectState, Integer>) SRSObjectState::setUid, SRSObjectState::getUid, ParameterType.EQ_ONLY, true),
        SET_CODE((BiConsumer<SRSObjectState, Integer>) SRSObjectState::setSetCode, SRSObjectState::getSetCode, ParameterType.EQ_ONLY, true),
        ITEM_SUID((BiConsumer<SRSObjectState, Integer>) SRSObjectState::setItemSUID, SRSObjectState::getItemSUID, ParameterType.EQ_ONLY, true),
        WEIGHT((BiConsumer<SRSObjectState, Integer>) SRSObjectState::setWeight, SRSObjectState::getWeight, ParameterType.NUMBER, false);

        private final BiConsumer<SRSObjectState, ?> setter;

        private final Function<SRSObjectState, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<SRSObjectState, ?> setter, Function<SRSObjectState, ?> getter, ParameterType type, boolean insert) {
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
        public BiConsumer<SRSObjectState, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<SRSObjectState, ?> getGetter() {
            return getter;
        }

        @Override
        public boolean insert() {
            return insert;
        }
    }
}
