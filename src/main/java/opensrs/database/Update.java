/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * An update for a certain {@link GenericParameter}.
 * <p>This object specifies which parameter should be updated and with what value</p>
 * @author piotr
 */
public class Update <T extends GenericParameter> {
    private T parameter;
    private Object newValue;

    /**
     * Constructor specifying the parameter and new value.
     * @param parameter The parameter to be updated.
     * @param newValue  The value to be updated with.
     */
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
