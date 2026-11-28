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
import opensrs.dataobjects.Image;
import opensrs.dataobjects.SRSObjectState;
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;

/**
 * Abstract database class.
 * Defines all the required database interactions.
 * @author piotr
 */
public abstract class Database {
    
    /**
     * gets the {@link User} by name.
     * @see User
     * @param name  the name of the user.
     * @return      the user, or <code>null</code> when not found
     */
    public abstract User getUserByName(String name);
    
    /**
     * gets the {@link User} by id.
     * @see User
     * @param uid   the uid of the user
     * @return      the user, or <code>null</code> when not found
     */
    public abstract User getUserById(int uid);
    
    /**
     * creates the user in the database based on a {@link User} object
     * @see User
     * @param user    the {@link User} we want to create
     * @param setUid  whether or not the DB should write this user's UID
     */
    public abstract void createUser(User user, boolean setUid);
    
    /**
     * updates the user in the database based on a {@link User} object
     * @see User
     * @param user  the {@link User} we want to create
     */
    public abstract void updateUser(User user);
    
    /**
     * Stores a new image along with its contents.
     * @param image     the image
     * @param contents  the image's contents
     */
    public abstract void createImage(Image image, byte[] contents);

    /**
     * Gets the contents of an image.
     * @param imageId   the image's ID
     * @return          the contents, or null when the image doesn't exist
     */
    public abstract byte[] getImageContents(int imageId);

    /**
     * Lists images ordered by ID.
     * @param after     the number of images to skip
     * @param limit     the maximum number of images returned
     * @return          the images, or null on failure
     */
    public abstract List<Image> listImages(int after, int limit);

    /**
     * Fetches the images that meet the specified filters.
     * @param filter    the criteria the images have to meet
     * @param sorter    the order of the images
     * @param limit     the maximum number of images returned
     * @return          the images, or null on failure
     */
    public abstract List<Image> fetchImages(Filter<Image.Parameter> filter, Sorter<Image.Parameter> sorter, int limit);

    /**
     * Fetches the first image that meets the specified filters.
     * @param filter    the criteria the image has to meet
     * @param sorter    the order of the images
     * @return          the image, or null when none matches
     */
    public Image fetchImage(Filter<Image.Parameter> filter, Sorter<Image.Parameter> sorter) {
        return fetchImages(filter, sorter, 1).stream().findFirst().orElse(null);
    }

    /**
     * Assigns an image to the user in a set, replacing the previous one.
     * @param uid       the user's ID
     * @param setCode   the set's code
     * @param imageId   the image's ID
     * @return          false when the user or image doesn't exist
     */
    public abstract boolean assignUserImage(int uid, int setCode, int imageId);

    /**
     * Gets the image assigned to the user in a set.
     * @param uid       the user's ID
     * @param setCode   the set's code
     * @return          the image, or null when none is assigned
     */
    public abstract Image getUserImage(int uid, int setCode);

    /**
     * Gets the first image assigned to the user in any set.
     * @param uid       the user's ID
     * @return          the image, or null when none is assigned
     */
    public abstract Image getUserImage(int uid);

    /**
     * Fetches the list of all the results that meet the specified filters.
     * @see Filter
     * @see Sorter
     * @see Result.Parameter
     * @param filter    determines the criteria that need to be met by the returned instanced of result   .      
     * @param sorter    determines by which parameters to order the results by.
     * @param limit     the maximum numbers of items returned.
     * @return          returns an ordered list of result objects that met the query, up to a specified limit.
     */
    public abstract List<Result> fetchResults(Filter<Result.Parameter> filter, Sorter<Result.Parameter> sorter, int limit);
    
    /**
     * Fetches the first instance of result that meets the specified filters.
     * @see Filter
     * @see Sorter
     * @see Result.Parameter
     * @param filter    determines the criteria that need to be met by the returned instanced of result.
     * @param sorter    determines by which parameters to order the results by.
     * @return          the first instance of a result object that met the query. Returns <code>null</code> when empty.
     */
    public Result fetchResult(Filter<Result.Parameter> filter, Sorter<Result.Parameter> sorter) {
        return fetchResults(filter, sorter, 1).stream().findFirst().orElse(null);
    }

    /**
     * Fetches the list of all reviews that meet the specified filters.
     * @see Filter
     * @see Sorter
     * @see Review.Parameter
     * @param filter    determines the criteria that need to be met by the returned instanced of review.         
     * @param sorter    determines by which parameters to order the review by.
     * @param limit     the maximum numbers of items returned. 
     * @return          returns an ordered list of review objects that met the query, up to a specified limit.
     */
    public abstract List<Review> fetchReviews(Filter<Review.Parameter> filter, Sorter<Review.Parameter> sorter, int limit);
    
    /**
     * Fetches the first instance of a review that meets the specified filters.
     * @see Filter
     * @see Sorter
     * @see Review.Parameter
     * @param filter    determines the criteria that need to be met by the returned instanced of review.
     * @param sorter    determines by which parameters to order the review by.
     * @return          the first instance of a review object that met the query. Returns <code>null</code> when empty.
     */
    public Review fetchReview(Filter<Review.Parameter> filter, Sorter<Review.Parameter> sorter) {
        return fetchReviews(filter, sorter, 1).stream().findFirst().orElse(null);
    }
    
    /**
     * Create reviews in the database based on the provided {@link Review} objects
     * @see Review
     * @param reviews   One or more {@link Review} to be created in the database
     */
    public abstract void createReviews(Review... reviews);
    
        
    /**
     * Create results in the database based on the provided {@link Result} objects
     * @see Result
     * @param results   One or more {@link Result} to be created in the database
     */
    public abstract void createResults(Result... results);
    
    /**
     * Updates all reviews in the database that match the query.
     * @see Review
     * @param filter    determines the criteria that need to be met by the updated instanced of review.
     * @param updates   determines which fields should be updated with which values.
     * @return          returns the updated reviews.
     */
    public abstract List<Review> updateReviews(Filter<Review.Parameter> filter, Update<Review.Parameter>... updates);
    
    /**
     * Updates all results in the database that match the query.
     * @see Result
     * @param filter    determines the criteria that need to be met by the updated instanced of result.
     * @param updates   determines which fields should be updated with which values.
     * @return          returns the updated results.
     */
    public abstract List<Result> updateResults(Filter<Result.Parameter> filter, Update<Result.Parameter>... updates);
    
    /**
     * Fetches the current state of all objects that match the query
     * @see SRSObjectState
     * @param filter    determines the criteria that need to be met by the instanced of SRSObjectState.
     * @param limit     the maximum numbers of items returned.
     * @return          returns a list of SRSObjectState that met the query, up to a specified limit.
     */
    public abstract List<SRSObjectState> fetchState(Filter<SRSObjectState.Parameter> filter, int limit);
    
    /**
     * Deletes reviews which match the filters
     * @param filter    determines the criteria that need to be met by the instanced of SRSObjectState.
     */
    public abstract void deleteReviews(Filter<Review.Parameter> filter);
       
    /**
     * Deletes results which match the filters
     * @param filter    determines the criteria that need to be met by the instanced of SRSObjectState.
     */
    public abstract void deleteResults(Filter<Result.Parameter> filter);

    /**
     * Closes the database
     */
    public abstract void close();
}
