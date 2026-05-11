/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * A filter for a certain parameter.
 * The parameter should extend {@link GenericParameter}.
 * <p>Specific filters for specific data types are defined in {@link Filters}</p>
 * @see GenericParameter
 * @see Filters
 * @author piotr
 */
public class Filter<T extends GenericParameter> {
    private T filteredValue;
    private Filters.Filter filter;
    private Object value;
    
    Filter(){}

    /**
     * Creates and validates the filter for the filtered value type
     * @param filteredValue value to be filtered of type {@link T}
     * @param filter        
     * @param value 
     */
    public Filter(T filteredValue, Filters.Filter filter, Object value) {
        if(filteredValue.getType() == ParameterType.NOT_APPLICABLE) throw new RuntimeException("Cannot filter by a NOT_APPLICABLE field");
        if (!filter.getType().getClass().equals(filteredValue.getType().getClass())) {
            throw new RuntimeException("Invalid filter type for " + filteredValue.name());
        }
        this.filteredValue = filteredValue;
        this.filter = filter;
        this.value = value;
    }

    public T getFilteredValue() {
        return filteredValue;
    }

    public Filters.Filter getFilter() {
        return filter;
    }

    public Object getValue() {
        return value;
    }

    public void setFilteredValue(T filteredValue) {
        this.filteredValue = filteredValue;
    }

    public void setFilter(Filters.Filter filter) {
        this.filter = filter;
    }

    public void setValue(Object value) {
        this.value = value;
    }
    
    
}
