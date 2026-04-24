/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

import java.util.List;

/**
 *
 * @author piotr
 */
public class FilterChain<T extends GenericParameter> extends Filter<T> {

    public enum Junction {
        OR, AND;
    }

    private final List<Filter<T>> filters;
    private final Junction junction;

    public FilterChain(Junction junction, List<Filter<T>> filters) {
        this.junction = junction;
        this.filters = filters;
    }

    public List<Filter<T>> getFilters() {
        return filters;
    }

    public Junction getJunction() {
        return junction;
    }

}
