/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * A sorter for a certain parameter.
 * The parameter should extend {@link GenericParameter}.
 * <p>Specific sorters for specific data types are defined in {@link Sorters}</p>
 * @see GenericParameter
 * @see Sorters
 * @author piotr
 */
public class Sorter<T extends GenericParameter> {
    private final T sortedValue;
    private final Sorters.Sorter sorter;
    
    /**
     * Constructor that creates the sorting method.
     * @param sortedValue   Value that needs to be sorted of type {@link T}.
     * @param sorter        The sorting method to be used ({@link Sorters}).
     */
    public Sorter(T sortedValue, Sorters.Sorter sorter) {
        this.sortedValue = sortedValue;
        this.sorter = sorter;
    }

    public T getSortedValue() {
        return sortedValue;
    }

    public Sorters.Sorter getSorter() {
        return sorter;
    }
}
