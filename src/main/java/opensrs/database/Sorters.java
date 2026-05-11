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
public class Sorters {
    public interface Sorter{
        public String getOperator();
    }
    public static enum NumberSorter implements Sorter{
        ASCENDING("ASC"), 
        DEFAULT(""),
        DESCENDING("DESC");
        
        private final String operator;
        private NumberSorter(String operator){
            this.operator = operator;
        }
        
        @Override
        public String getOperator(){
            return operator;
        }
    }
    
    public static enum TextSorter implements Sorter{
        ASCENDING("ASC"), 
        DEFAULT(""),
        DESCENDING("DESC");
        
        private final String operator;
        private TextSorter(String operator){
            this.operator = operator;
        }
        
        @Override
        public String getOperator(){
            return operator;
        }
    }
}
