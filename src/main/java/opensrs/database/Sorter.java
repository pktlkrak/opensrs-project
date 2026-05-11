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
public class Sorter<T extends GenericParameter> {
    private final T sortedValue;
    private final Sorters.Sorter sorter;

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
