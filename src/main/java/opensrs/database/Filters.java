/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

/**
 * A class containing multiple implementations of filters for different data types.
 * @author piotr
 */
public class Filters {
    /**
     * Interface implemented by all the Filter enums.
     */
    public interface Filter{
        public ParameterType getType();
        public String getSymbol();
    }
    /**
     * Filters for operating on text, matching different SQL operations.
     */
    public enum Text implements Filter{
        EQUAL("="), ALIKE("LIKE"), NOT_ALIKE("NOT LIKE"), NOT_EQUAL("!=");
        private final String symbol;
        private Text(String symbol){
            this.symbol = symbol;
        }
        @Override
        public String getSymbol(){
            return symbol;
        }

        @Override
        public ParameterType getType() {
            return ParameterType.TEXT;
        }
    }
    public enum Number implements Filter{
        EQUAL("="), LESSER("<"), GREATER(">"), ELESSER("<="), EGREATER(">="), NOT_EQUAL("!=");

        private final String symbol;
        private Number(String symbol){
            this.symbol = symbol;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }
        
        @Override
        public ParameterType getType() {
            return ParameterType.NUMBER;
        }
    }
    public enum EqOnly implements Filter{
        EQUAL("="),
        NOT_EQUAL("!=");
        private final String symbol;
        private EqOnly(String symbol){
            this.symbol = symbol;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }
        
        @Override
        public ParameterType getType() {
            return ParameterType.EQ_ONLY;
        }

    }
    
    public enum Array implements Filter{
        CONTAINS("IN"),
        NOT_CONTAINS("NOT IN");
        
        private final String symbol;
        private Array(String symbol){
            this.symbol = symbol;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }
        
        @Override
        public ParameterType getType() {
            return ParameterType.ARRAY;
        }
    }
    
}
