/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author piotr
 */
public abstract class DataObject<T extends GenericParameter> {
    private final Map<GenericParameter, Object> cache = new HashMap<>();
    protected Object getObject(T param){
        if(!cache.containsKey(param)){
            cache.put(param, fetchObject(param));
        }
        return cache.get(param);
    }
    
    protected abstract Object fetchObject(T param);
    public abstract T getIdField();
    @SuppressWarnings("unchecked")
    public Object getDataObjectId(){
        return getIdField().getGetter().apply((Object) this);
    }
}
