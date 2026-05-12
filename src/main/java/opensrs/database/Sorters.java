/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * A class containing multiple implementations of sorters for different data types.
 * <p>This class allows the access to all the sorter types.</p>
 * <p>This class only contains the sorter types. To actually sort something you need to use {@link Sorter}</p>
 * @see Sorter
 * @author piotr
 */
public class Sorters {
    /**
     * Interface implemented by all the Sorters enums.
     */
    public interface Sorter{
        public String getOperator();
    }
    
    /**
     * Sorters for operating on {@link ParameterType#NUMBER}, matching different SQL operations.
     */
    public static enum NumberSorter implements Sorter{
        /**
         * Sorts the numbers in an ascending order (lowest to highest).
         */
        ASCENDING("ASC"), 
        
        /**
         * Uses the default sorting method of the database (does not specify the sort order).
         */
        DEFAULT(""),
        
        /**
         * Sorts the numbers in an ascending order (highest to lowest).
         */
        DESCENDING("DESC");
        
        private final String operator;
        private NumberSorter(String operator){
            this.operator = operator;
        }
        
        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getOperator(){
            return operator;
        }
    }
    
    /**
     * Sorters for operating on {@link ParameterType#TEXT}, matching different SQL operations.
     */
    public static enum TextSorter implements Sorter{
        /**
         * Sorts the numbers in an ascending order (lowest to highest).
         */
        ASCENDING("ASC"), 
        
        /**
         * Uses the default sorting method of the database (does not specify the sort order).
         */
        DEFAULT(""),
        
        /**
         * Sorts the numbers in an ascending order (highest to lowest).
         */
        DESCENDING("DESC");
        
        private final String operator;
        private TextSorter(String operator){
            this.operator = operator;
        }
             
        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getOperator(){
            return operator;
        }
    }
}
