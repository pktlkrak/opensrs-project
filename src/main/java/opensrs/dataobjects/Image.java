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
 * {@link DataObject} for representing an uploaded image.
 * @author piotr
 */
public class Image extends DataObject<Image.Parameter> {
    private int id = -1;
    private String contentType;

    public Image() {
    }

    public Image(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public Parameter getIdField() {
        return Parameter.IMAGE_ID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    protected Object fetchObject(Parameter param) {
        throw new UnsupportedOperationException("No fetchable parameters present.");
    }

    /**
     * Enum containing all the parameters of the {@link Image} Class.
     */
    public enum Parameter implements GenericParameter<Image> {
        /**
         * The ID of the image.
         */
        IMAGE_ID((BiConsumer<Image, Integer>) Image::setId, Image::getId, ParameterType.EQ_ONLY, false),

        /**
         * The mimetype of the image.
         */
        IMAGE_TYPE((BiConsumer<Image, String>) Image::setContentType, Image::getContentType, ParameterType.TEXT, true);

        private final BiConsumer<Image, ?> setter;
        private final Function<Image, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<Image, ?> setter, Function<Image, ?> getter, ParameterType type, boolean insert) {
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
        public BiConsumer<Image, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<Image, ?> getGetter() {
            return getter;
        }

        @Override
        public boolean insert() {
            return insert;
        }
    }
}
