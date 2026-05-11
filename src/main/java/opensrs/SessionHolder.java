/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import opensrs.database.Database;
import java.util.concurrent.ConcurrentHashMap;
import opensrs.dataobjects.User;

/**
 *
 * @author piotr
 */
public class SessionHolder {
    private static final ConcurrentHashMap<String, SessionInfo> SESSIONS = new ConcurrentHashMap<>();
    private static final String TAG = "SessionHolder";
    
    public static String getNameOfUser(String seskey) {
        if (seskey == null) {
            return null;
        }
        SessionInfo entry = SESSIONS.get(seskey);
        if (entry == null) {
            return null;
        }

        if (entry.isOutdated()) {
            logout(seskey);
            return null;
        }
        entry.reset();
        return entry.getName();
    }

    public static int getUID(String seskey) {
        if(seskey == null) return -1;
        if(SESSIONS.containsKey(seskey) && SESSIONS.get(seskey).reset()){
            return SESSIONS.get(seskey).getUid();
        }else logout(seskey); //Cleanup
        return -1;
    }

    public static int getUIDByName(Database db, String uname) {
        if (uname == null) {
            return -1;
        }
        User u = db.getUserByName(uname);
        return u == null ? -1 : u.getUid();
    }

    public static String login(Database db, String uname, String pass) {
        if (!isCorrectPassword(db, uname, pass)) {
            Log.log(Log.INFO, TAG, "Invalid password from user " + uname);
            return null;
        }
        Log.log(Log.INFO, TAG, "Logged on as " + uname);
        String newKey = Utils.randStr(256);
        SESSIONS.put(newKey, new SessionInfo(uname, db.getUserByName(uname).getUid()));
        return newKey;
    }

    public static boolean isCorrectPassword(Database db, String uname, String pass) {
        User u = db.getUserByName(uname);
        if (u == null) {
            return false;
        }
        pass += ":" + u.getSalt();
        return Utils.getSHA256Hash(pass.getBytes()).equals(u.getHash());
    }

    public static void logout(String key) {
        SESSIONS.remove(key);
    }
    
    private static final class SessionInfo{
        String name;
        int uid;
        long session;
        
        public SessionInfo(String name, int uid){
            this.name = name;
            this.uid = uid;
            this.session = System.currentTimeMillis();
        }
        
        public int getUid(){
            return uid;
        }
        
        public boolean reset(){
            if(isOutdated()) return false;
            this.session = System.currentTimeMillis();
            return true;
        }
        
        public boolean isOutdated(){
            return System.currentTimeMillis() - session > 10*60*1000;
        }

        public String getName() {
            return name;
        }
        
        
    }
}
