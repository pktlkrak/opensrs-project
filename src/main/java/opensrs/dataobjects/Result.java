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
 * {@link DataObject} for representing a result.
 * <p>A result is a single most granular instance of feedback of a user's interaction with the SRS. For example, a results says if the answer for the certain question or coerce was correct or not. </p>
 * @author piotr
 */
public class Result extends DataObject<Result.Parameter>{
    private int id;
    private int reviewID;
    private int itemSUID;
    
    private int itemParam;
    private int amountOfTries;
    
    private int setCode, uid;

    public Result() {
    }
    
    @Override
    public Parameter getIdField(){
        return Parameter.ID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReviewID() {
        return reviewID;
    }

    public void setReviewID(int reviewID) {
        this.reviewID = reviewID;
    }

    public int getItemSUID() {
        return itemSUID;
    }

    public void setItemSUID(int itemSUID) {
        this.itemSUID = itemSUID;
    }

    public int getItemParam() {
        return itemParam;
    }

    public void setItemParam(int itemParam) {
        this.itemParam = itemParam;
    }

    public int getAmountOfTries() {
        return amountOfTries;
    }

    public void setAmountOfTries(int amountOfTries) {
        this.amountOfTries = amountOfTries;
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

    @Override
    protected Object fetchObject(Parameter param) {
        return null;
    }
    
    /**
     * Enum containing all the parameters of the {@link Result} Class.
     */
    public enum Parameter implements GenericParameter<Result>{
        /**
         * The UID of the {@link User} the result belongs to.
         */
        UID((BiConsumer<Result, Integer>) Result::setUid, Result::getUid, ParameterType.EQ_ONLY, true),
        
        /**
         * The code of the set the review comes from (Multiple sets can be set up simultaneously)
         */
        SET_CODE((BiConsumer<Result, Integer>) Result::setSetCode, Result::getSetCode, ParameterType.EQ_ONLY, true),
        
        /**
         * The ID of this particular result.
         */
        ID((BiConsumer<Result, Integer>) Result::setId, Result::getId, ParameterType.EQ_ONLY, false),
        
        /**
         * The ID of the {@link Review} this is a result of.
         */
        REVIEW_ID((BiConsumer<Result, Integer>) Result::setReviewID, Result::getReviewID, ParameterType.EQ_ONLY, true),
        
        /**
         * The internal Set UID Of the item (the ID inside the set, not the DB).
         */
        ITEM_SUID((BiConsumer<Result, Integer>) Result::setItemSUID, Result::getItemSUID, ParameterType.EQ_ONLY, true),
        
        /**
         * Which aspect of the object we are reviewing. (for ex. spelling and meaning - this is defined on a set by set basis)
         */
        ITEM_PARAM((BiConsumer<Result, Integer>) Result::setItemParam, Result::getItemParam, ParameterType.EQ_ONLY, true),
        
        /**
         * The amount of mistakes the user made before submitting the correct answer. For example if the user made 2 mistakes and was correct in their third answer this will be 2.
         */
        TRIES_AMOUNT((BiConsumer<Result, Integer>) Result::setAmountOfTries, Result::getAmountOfTries, ParameterType.NUMBER, true);
                
        private final BiConsumer<Result, ?> setter;
        private final Function<Result, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<Result, ?> setter, Function<Result, ?> getter, ParameterType type, boolean insert) {
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
        public BiConsumer<Result, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<Result, ?> getGetter() {
            return getter;
        }
        
        @Override
        public boolean insert(){
            return insert;
        }
    }
    
}
