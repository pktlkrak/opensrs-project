/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import opensrs.database.Database;
import opensrs.dataobjects.User;

/**
 *
 * @author piotr
 */
public class SessionHolder {
    private static final String TAG = "SessionHolder";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_UID = "uid";
    private static final JWTVerifier JWT_VERIFIER;
    private static final Algorithm JWT_ALGORITHM;
    
    static {
        Algorithm alg = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            KeyPair kp = kpg.generateKeyPair();
            alg = Algorithm.RSA256((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate());
        } catch (NoSuchAlgorithmException ex) {
            // Cannot happen
            ex.printStackTrace();
        }
        JWT_ALGORITHM = alg;
        JWT_VERIFIER = JWT.require(alg).build();
    }
    
    public static String getNameOfUser(String seskey) {
        if(seskey == null) return null;
        try{
            DecodedJWT jwt = JWT_VERIFIER.verify(seskey);
            return jwt.getClaim(CLAIM_USERNAME).asString();
        }catch(RuntimeException ex){
            Log.handleException(TAG, ex, "getNameOfUser");
            return null;
        }
    }
    
    public static int getUID(String seskey) {
        if(seskey == null) return -1;
        try{
            DecodedJWT jwt = JWT_VERIFIER.verify(seskey);
            return jwt.getClaim(CLAIM_UID).asInt();
        }catch(RuntimeException ex){
            Log.handleException(TAG, ex, "getUID");
            return -1;
        }
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
        String jwt = JWT.create()
                .withClaim(CLAIM_USERNAME, uname)
                .withClaim(CLAIM_UID, db.getUserByName(uname).getUid())
                .sign(JWT_ALGORITHM);
        return jwt;
    }

    public static boolean isCorrectPassword(Database db, String uname, String pass) {
        User u = db.getUserByName(uname);
        if (u == null) {
            return false;
        }
        pass += ":" + u.getSalt();
        return Utils.getSHA256Hash(pass.getBytes()).equals(u.getHash());
    }
}
