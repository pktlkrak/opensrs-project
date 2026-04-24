/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import opensrs.dataobjects.User;

/**
 *
 * @author piotr
 */
public interface GenericParameter<T extends DataObject> {
    public String name();
    public BiConsumer<T, ?> getSetter(); //Used when getting objects from database
    public Function<T, ?> getGetter(); //Used when filtering 
    public ParameterType getType();
    public boolean insert();
}
