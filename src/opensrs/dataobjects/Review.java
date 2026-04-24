/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.dataobjects;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import opensrs.database.DataObject;
import opensrs.database.GenericParameter;
import opensrs.database.ParameterType;

/**
 *
 * @author piotr
 */
public abstract class Review extends DataObject<Review.Parameter>{
    public static final int TYPE_LESSON = 0;
    public static final int TYPE_REVIEW = 1;
    private int id;
    private int srsWeight;
    private int reviewSessionId;
    private int itemSUID;
    private int type;
    private long queuedDate, unlockDate;
    
    private int setCode, uid;

    public Review() {
    }
    
    public static Review create(){
        Review r = new Review() {
            @Override
            protected Object fetchObject(Parameter param) {
                switch(param){
                    case RESULTS: return new LinkedList<>();
                }
                return null;
            }
        };
        r.getResults();
        return r;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    
    
    public int getReviewSessionId() {
        return reviewSessionId;
    }

    public void setReviewSessionId(int reviewSessionId) {
        this.reviewSessionId = reviewSessionId;
    }
    
    public int getSrsWeight() {
        return srsWeight;
    }

    public void setSrsWeight(int srsWeight) {
        this.srsWeight = srsWeight;
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

    public int getItemSUID() {
        return itemSUID;
    }

    public void setItemSUID(int itemSUID) {
        this.itemSUID = itemSUID;
    }

    public long getQueuedDate() {
        return queuedDate;
    }

    public void setQueuedDate(Number queuedDate) {
        this.queuedDate = queuedDate.longValue();
    }

    public long getUnlockDate() {
        return unlockDate;
    }

    public void setUnlockDate(Number unlockDate) {
        this.unlockDate = unlockDate.longValue();
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
    
    public List<Result> getResults(){
        return (List<Result>) fetchObject(Parameter.RESULTS);
    }
    
    public enum Parameter implements GenericParameter<Review>{
        UID((BiConsumer<Review, Integer>) Review::setUid, Review::getUid, ParameterType.EQ_ONLY, true),
        SET_CODE((BiConsumer<Review, Integer>) Review::setSetCode, Review::getSetCode, ParameterType.EQ_ONLY, true),
        ID((BiConsumer<Review, Integer>) Review::setId, Review::getId, ParameterType.EQ_ONLY, false),
        TYPE((BiConsumer<Review, Integer>) Review::setType, Review::getType, ParameterType.EQ_ONLY, true),
        ITEM_SUID((BiConsumer<Review, Integer>) Review::setItemSUID, Review::getItemSUID, ParameterType.EQ_ONLY, true),
        QUEUED_DATE((BiConsumer<Review, Number>) Review::setQueuedDate, Review::getQueuedDate, ParameterType.EQ_ONLY, true),
        UNLOCK_DATE((BiConsumer<Review, Number>) Review::setUnlockDate, Review::getUnlockDate, ParameterType.EQ_ONLY, true),
        SRS_WEIGHT((BiConsumer<Review, Integer>) Review::setSrsWeight, Review::getSrsWeight, ParameterType.NUMBER, true),
        REVIEW_SESSION_ID((BiConsumer<Review, Integer>) Review::setReviewSessionId, Review::getReviewSessionId, ParameterType.EQ_ONLY, true),
        
        RESULTS(null, Review::getResults, ParameterType.NOT_APPLICABLE, false);
        
        private final BiConsumer<Review, ?> setter;
        private final Function<Review, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<Review, ?> setter, Function<Review, Object> getter, ParameterType type, boolean insert) {
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
        public BiConsumer<Review, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<Review, ?> getGetter() {
            return getter;
        }
        
        @Override
        public boolean insert(){
            return insert;
        }
    }
}
