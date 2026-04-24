/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.FilterChain;
import opensrs.database.Filters;
import opensrs.database.Update;
import opensrs.database.implementation.SQLiteDatabase;
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;

/**
 *
 * @author piotr
 */
public class HalfAss {

    public static final int SYSTEM_SID = 11131996;

    public static void main(String[] args) throws IOException, SQLException, Exception {
        /*Scanner scanner = new Scanner(System.in);
        System.out.println("Set name: ");
        String setName = scanner.nextLine();
        int setCode = setName.hashCode();
        Set set = GlobalStore.getSet(setName);
        System.out.println("UID: ");
        int uid = Integer.valueOf(scanner.nextLine());
        System.out.println("Read file: ");
        ArrayNode root = (ArrayNode) Utils.MAPPER.readTree(new File(scanner.nextLine()));

        Database database = new SQLiteDatabase("/ram/userdata.db");
        
        for(String e : "324 346 341 301 283 319 327".split(" ")){
            Review r = Review.create();
            r.setItemSUID(Integer.valueOf(e));
            r.setQueuedDate(System.currentTimeMillis());
            r.setSetCode(setCode);
            r.setUid(uid);
            r.setUnlockDate(-2);
            r.setSrsWeight(0);
            r.setType(Review.TYPE_LESSON);
            database.createReviews(r);
        }

        for (int i = 0; i < root.size(); i++) {
            JsonNode n = root.get(i);
            if(n.isNull()) continue;
            String qu = URLDecoder.decode(n.get("query").textValue(), "utf-8").replace("-", " ");
            System.out.print("Getting for " + n.get("type").textValue() + " " + qu);
            SRSObject thisObject = set.stream().filter(q -> q.getFamily().equalsIgnoreCase(n.get("type").textValue()) && (q.getQuery().equalsIgnoreCase(qu) || (n.get("type").textValue().equals("radical") ? q.getReviewable().get("name").get(0).equalsIgnoreCase(qu) : false)) ).findFirst().orElse(null);
            if(thisObject == null){
                System.out.println("Errored for " + qu);
                return;
            }
            long unlocked = n.get("unlocked").asLong() * 1000;
            int stage = n.get("stage").intValue();
            Review r = Review.create();
            r.setItemSUID(thisObject.getId());
            r.setQueuedDate(unlocked++);
            r.setSetCode(setCode);
            r.setUid(uid);
            r.setUnlockDate(-2);
            r.setSrsWeight(0);
            r.setType(Review.TYPE_LESSON);
            database.createReviews(r);
            for (int s = 0; s < stage; s++) {
                r = database.updateReviews(
                        new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                                new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                                new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setCode),
                                new Filter<>(Review.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, thisObject.getId()),
                                new FilterChain<>(FilterChain.Junction.OR, Arrays.asList(
                                        new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -1),
                                        new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -2))))),
                        new Update<>(Review.Parameter.UNLOCK_DATE, unlocked++),
                        new Update<>(Review.Parameter.REVIEW_SESSION_ID, SYSTEM_SID),
                        new Update<>(Review.Parameter.SRS_WEIGHT, 1)).get(0);
                int rid = r.getId();
                Result results[] = new Result[thisObject.getReviewable().size()];
                for (int re = 0; re < results.length; re++) {
                    results[re] = new Result();
                    results[re].setId(thisObject.getId());
                    results[re].setSetCode(setCode);
                    results[re].setAmountOfTries(0);
                    results[re].setItemParam(re);
                    results[re].setReviewID(rid);
                    results[re].setUid(uid);
                    results[re].setItemSUID(thisObject.getId());
                }
                database.createResults(results);
                r = Review.create();
                r.setItemSUID(thisObject.getId());
                r.setQueuedDate(unlocked++);
                r.setSetCode(setName.hashCode());
                r.setUid(uid);
                r.setUnlockDate(-1);
                r.setSrsWeight(0);
                r.setType(Review.TYPE_REVIEW);
                database.createReviews(r);
            }
        }
        System.out.println("Data has been successfully half-assed into the database. Hope you can sleep well knowing what you did...");
        Log.prepareForExit();
            
    public void doHalfAss() throws SQLException{
        ResultSet set = query("select * from reviews where review_session_id = ? and srs_weight < 0", 2079462533L);
        Set osrsSet = GlobalStore.getSet("wk.set");
        while(set.next()){
            long id = set.getLong("item_suid");
            ResultSet forThis = query("select * from (select * from reviews where item_suid = ? order by id desc limit 2) order by id asc", id);
            forThis.next();
            long unlock = forThis.getLong("unlock_date");
            if(forThis.getInt("srs_weight") < 0 && forThis.getLong("review_session_id") == 2079462533L){
                forThis.next();
                long thisId = forThis.getLong("id");
                long nextQ = forThis.getLong("queued_date");
                long unD = forThis.getLong("unlock_date");
                if(unD != -1L){
                    System.out.println("Already fixed");
                    continue;
                }
                long diff = nextQ - unlock;
                int i = 1;
                while(diff < (((long)osrsSet.config.getDelayInHours(i)) * 60L * 60L *1000L)) i++;
                i-=2;
                assert i > 0;
                query("update reviews set queued_date = ? where id = ?", unlock + (((long)osrsSet.config.getDelayInHours(i)) * 60L * 60L *1000L), thisId);
            }else System.out.print("No processing for " + id);
        }
    }

        scanner.close();*/
        SQLiteDatabase d = new SQLiteDatabase("/ram/userdata.db");
        //d.doHalfAss();
        Log.prepareForExit();
    }
}
