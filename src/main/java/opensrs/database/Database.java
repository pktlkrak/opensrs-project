/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.database;

import opensrs.dataobjects.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import opensrs.dataobjects.SRSObjectState;
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;

/**
 *
 * @author piotr
 */
public abstract class Database {
    
    public abstract User getUserByName(String name);
    public abstract User getUserById(int uid);
    public abstract void createUser(User user);

    public abstract List<Result> fetchResults(Filter<Result.Parameter> filter, Sorter<Result.Parameter> sorter, int limit);
    public Result fetchResult(Filter<Result.Parameter> filter, Sorter<Result.Parameter> sorter) {
        return fetchResults(filter, sorter, 1).stream().findFirst().orElse(null);
    }

    public abstract List<Review> fetchReviews(Filter<Review.Parameter> filter, Sorter<Review.Parameter> sorter, int limit);
    public Review fetchReview(Filter<Review.Parameter> filter, Sorter<Review.Parameter> sorter) {
        return fetchReviews(filter, sorter, 1).stream().findFirst().orElse(null);
    }
    
    public abstract void createReviews(Review... reviews);
    public abstract void createResults(Result... results);
    
    public abstract List<Review> updateReviews(Filter<Review.Parameter> filter, Update<Review.Parameter>... updates);
    public abstract List<Result> updateResults(Filter<Result.Parameter> filter, Update<Result.Parameter>... updates);
    
    public abstract List<SRSObjectState> fetchState(Filter<SRSObjectState.Parameter> filter, int limit);
    
    public abstract void close();
}
