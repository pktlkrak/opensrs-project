/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 *
 * @author piotr
 */
public class Update <T extends GenericParameter> {
    private T parameter;
    private Object newValue;

    public Update(T parameter, Object newValue) {
        this.parameter = parameter;
        this.newValue = newValue;
    }

    public T getParameter() {
        return parameter;
    }

    public void setParameter(T parameter) {
        this.parameter = parameter;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
    
}
