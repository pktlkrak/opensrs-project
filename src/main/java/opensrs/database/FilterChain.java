/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

import java.util.List;

/**
 * A group of filters for a certain parameter of type, joined by either an AND or OR.
 * @author piotr
 */
public class FilterChain<T extends GenericParameter> extends Filter<T> {

    /**
     * Type of junction.
     * <p>Determines if one or all filters must be met</p>
     */
    public enum Junction {
        OR, AND;
    }


    private final List<Filter<T>> filters;
    private final Junction junction;

    
    /**
     * Constructor taking in the filter list and joining method.
     * @param junction  determines how the filters are joined.
     * @param filters   list of filters.
     */
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
