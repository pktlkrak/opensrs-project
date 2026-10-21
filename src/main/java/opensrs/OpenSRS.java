/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import opensrs.database.Database;
import opensrs.database.implementation.PostgresDatabase;
import org.autumn.Logger;
import org.autumn.Server;

/**
 *
 * @author piotr
 */
public class OpenSRS {

    public static final String VERSION = "OpenSRS INITIAL";
    
    private static final String TAG = "PREINIT";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Log.startLoggingThread();
        Log.log(Log.INFO, TAG, "Starting " + VERSION);
        String db = System.getProperty("POSTGRES_DB");
        String username = System.getProperty("POSTGRES_USER");
        String password = System.getProperty("POSTGRES_PASSWORD");
        String dbHost = System.getProperty("POSTGRES_HOST");
        String strDbPort = System.getProperty("POSTGRES_PORT");
        if(strDbPort == null || strDbPort.isEmpty()) strDbPort = "5432";
        int dbPort = Integer.parseInt(strDbPort);
        
        Database database = new PostgresDatabase(dbHost, dbPort, db, username, password);
        
        Server autumn = new Server("opensrs", new Logger(){
            @Override
            public void logException(int severity, String tag, Exception ex, String... info) {
                Log.handleException(severity, tag, ex, info);
            }

            @Override
            public void log(int severity, String tag, String... data) {
                Log.log(severity, tag, data);
            }
            
        }, new Class<?>[]{Database.class}, database);
        autumn.start(9998);
        Runtime.getRuntime().addShutdownHook(new Thread(database::close));
    }
}
