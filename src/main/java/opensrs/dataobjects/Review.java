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
 * {@link DataObject} for representing a result.
 * <p>A review is a single instance of reviewing an SRS object.</p>
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
        /**
         * The UID of the {@link User} the result belongs to.
         */
        UID((BiConsumer<Review, Integer>) Review::setUid, Review::getUid, ParameterType.EQ_ONLY, true),
                
        /**
         * The code of the set the review comes from (Multiple sets can be set up simultaneously)
         */
        SET_CODE((BiConsumer<Review, Integer>) Review::setSetCode, Review::getSetCode, ParameterType.EQ_ONLY, true),
        
        /**
         * The ID of this particular review.
         */
        ID((BiConsumer<Review, Integer>) Review::setId, Review::getId, ParameterType.EQ_ONLY, false),
        
        /**
         * The type of the review. Can be {@link Review#TYPE_LESSON} or {@link Review#TYPE_REVIEW}
         */
        TYPE((BiConsumer<Review, Integer>) Review::setType, Review::getType, ParameterType.EQ_ONLY, true),
        
        /**
         * The internal Set UID Of the item (the ID inside the set, not the DB).
         */
        ITEM_SUID((BiConsumer<Review, Integer>) Review::setItemSUID, Review::getItemSUID, ParameterType.EQ_ONLY, true),
        
        /**
         * The date and time after which the result should be done again by the user.
         */
        QUEUED_DATE((BiConsumer<Review, Number>) Review::setQueuedDate, Review::getQueuedDate, ParameterType.EQ_ONLY, true),
        
        /**
         * The date when the user unlocked this item to be reviewed.
         * {@code -1} means unreviewed.
         * {@code -2} means lesson.
         */
        UNLOCK_DATE((BiConsumer<Review, Number>) Review::setUnlockDate, Review::getUnlockDate, ParameterType.EQ_ONLY, true),
        
        /**
         * The change of the level for this object caused by this review.
         * This is based on the penalty config per level.
         */
        SRS_WEIGHT((BiConsumer<Review, Integer>) Review::setSrsWeight, Review::getSrsWeight, ParameterType.NUMBER, true),
        
        /**
         * The ID of the review session this singular review was a part of.
         * If the user reviewed multiple objects in one session they will share this ID.
         */
        REVIEW_SESSION_ID((BiConsumer<Review, Integer>) Review::setReviewSessionId, Review::getReviewSessionId, ParameterType.EQ_ONLY, true),
        
        /**
         * A pseudo parameter to access the results of this review.
         */
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
