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
 * Generic Parameter that 
 * @author piotr
 */
public interface GenericParameter<T extends DataObject> {
    /**
     * Gets the name of the object
     * @return  object name
     */
    public String name();
    
    /**
     * Returns the function to set a given parameter on the object
     * @return  setter function
     */
    public BiConsumer<T, ?> getSetter(); //Used when getting objects from database
    
    /**
     * Returns the function to get a given parameter of the object
     * @return  getter function
     */
    public Function<T, ?> getGetter(); //Used when filtering 
    
    /**
     * Gets the type of the objects parameter
     * see ParameterType
     * @return  parameter type
     */
    public ParameterType getType();
    
    /**
     * If the object can be inserted into the database.
     * @return 
     */
    public boolean insert();
}
