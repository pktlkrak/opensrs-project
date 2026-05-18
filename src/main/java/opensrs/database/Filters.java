
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * A class containing multiple implementations of filters for different data types.
 * <p>This class allows the access to all the filter types, grouped by the {@link ParameterType} they filter.</p>
 * <p>This class only contains the filter types. To actually filter something you need to use {@link Filter}</p>
 * @see Filter
 * @see ParameterType
 * @author piotr
 */
public class Filters {
    /**
     * Interface implemented by all the Filters enums.
     */
    public interface Filter{
        /**
         * Returns the type of parameter this filter is executed on.
         * @see ParameterType
         * @see Filters.Text
         * @see Filters.Number
         * @see Filters.EqOnly
         * @see Filters.Array
         * @return  The parameter type.
         */
        public ParameterType getType();
        
        public String getSymbol();
    }
    
    /**
     * Filters for operating on {@link ParameterType#TEXT}, matching different SQL operations.
     */
    public enum Text implements Filter{
        /**
         * checks if both texts are exactly equal.
         */
        EQUAL("="), 
        
        /**
         * checks if texts are similar (SQL: <code>LIKE</code>).
         */
        ALIKE("LIKE"), 
        
        /**
         * checks if texts are not similar (SQL: <code>NOT LIKE</code>).
         */
        NOT_ALIKE("NOT LIKE"), 
        
        /**
         * checks if both texts aren't exactly equal.
         */
        NOT_EQUAL("!=");
        private final String symbol;
        private Text(String symbol){
            this.symbol = symbol;
        }
        
        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getSymbol(){
            return symbol;
        }

        /**
         * returns the type of parameter this filter is executed on.
         * <p>In this case this is {@link ParameterType#TEXT}</p>
         * @return  {@link ParameterType#TEXT}
         */
        @Override
        public ParameterType getType() {
            return ParameterType.TEXT;
        }
    }
    
    /**
     * Filters for operating on {@link ParameterType#NUMBER}, matching different SQL operations.
     */
    public enum Number implements Filter{
        /**
         * checks if both numbers are exactly equal.
         */
        EQUAL("="), 
        
        /**
         * checks if the number is lesser than the other.
         */
        LESSER("<"), 
        
        /**
         * checks if the number is greater than the other.
         */
        GREATER(">"), 
        
        /**
         * checks if the number is lesser or equal than the other.
         */
        ELESSER("<="), 
        
        /**
         * checks if the number is greater or equal than the other.
         */
        EGREATER(">="), 
        
        /**
         * check if the number is not equal to the other.
         */
        NOT_EQUAL("!=");

        private final String symbol;
        private Number(String symbol){
            this.symbol = symbol;
        }

        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * returns the type of parameter this filter is executed on.
         * <p>In this case this is {@link ParameterType#NUMBER}</p>
         * @return  {@link ParameterType#NUMBER}
         */
        @Override
        public ParameterType getType() {
            return ParameterType.NUMBER;
        }
    }
    
    /**
     * Filters for operating on {@link ParameterType#EQ_ONLY}, matching different SQL operations.
     */
    public enum EqOnly implements Filter{
        /**
         * checks if both values are exactly equal.
         */
        EQUAL("="),
        
        /**
         * checks if both values are exactly not equal.
         */
        NOT_EQUAL("!=");
        private final String symbol;
        private EqOnly(String symbol){
            this.symbol = symbol;
        }

        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * returns the type of parameter this filter is executed on.
         * <p>In this case this is {@link ParameterType#EQ_ONLY}</p>
         * @return  {@link ParameterType#EQ_ONLY}
         */
        @Override
        public ParameterType getType() {
            return ParameterType.EQ_ONLY;
        }

    }
    
    /**
     * Filters for operating on {@link ParameterType#ARRAY}, matching different SQL operations.
     */
    public enum Array implements Filter{
        /**
         * Checks if the value is in the specified array
         */
        CONTAINS("IN"),
        
        /**
         * Checks if the value is not in the specified array
         */
        NOT_CONTAINS("NOT IN");
        
        private final String symbol;
        private Array(String symbol){
            this.symbol = symbol;
        }

        /**
         * returns the SQL symbol of the filter.
         * @return  the SQL symbol of the filter.
         */
        @Override
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * returns the type of parameter this filter is executed on.
         * <p>In this case this is {@link ParameterType#ARRAY}</p>
         * @return  {@link ParameterType#ARRAY}
         */
        @Override
        public ParameterType getType() {
            return ParameterType.ARRAY;
        }
    }
    
}
