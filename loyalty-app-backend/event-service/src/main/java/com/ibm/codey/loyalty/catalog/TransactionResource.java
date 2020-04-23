package com.ibm.codey.loyalty.catalog;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.codey.loyalty.BaseResource;
import com.ibm.codey.loyalty.catalog.dao.TransactionDao;
import com.ibm.codey.loyalty.catalog.dao.UserDao;
import com.ibm.codey.loyalty.catalog.json.CreateTransactionDefinition;
import com.ibm.codey.loyalty.catalog.json.EventDefinition;
import com.ibm.codey.loyalty.catalog.json.RewardTransactionDefinition;
import com.ibm.codey.loyalty.catalog.models.Category;
import com.ibm.codey.loyalty.catalog.models.Event;
import com.ibm.codey.loyalty.catalog.models.Transaction;
import com.ibm.codey.loyalty.catalog.models.User;
import com.ibm.codey.loyalty.interceptor.LoggingInterceptor;
import com.ibm.codey.loyalty.interceptor.binding.RequiresAuthorization;

@RequestScoped
@Interceptors(LoggingInterceptor.class)
@Path("v1/transactions")
public class TransactionResource extends BaseResource {

    // TODO: Use rest to user microservice instead
    @Inject
    private UserDao userDao;

    @Inject
    private TransactionDao transactionDao;

    /**
     * This method creates a transaction.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createTransaction(CreateTransactionDefinition createTransactionDefinition) {

        Transaction newTransaction = new Transaction();
        // create new uuid for new transaction
        String transactionId = UUID.randomUUID().toString();

        // get subject
        String subject = this.getCallerSubject();
        // get user
        User user = userDao.findUserByRegistryId(subject);
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User is not registered").build();
        }
        if (!user.isConsentGiven()) {
            return Response.status(Response.Status.CONFLICT).entity("User has not consented to program").build();
        }

        newTransaction.setTransactionId(transactionId);
        newTransaction.setUserId(user.getUserId());
        newTransaction.setTransactionName(createTransactionDefinition.getTransactionName());
        newTransaction.setCategory(createTransactionDefinition.getCategory());
        newTransaction.setAmount(createTransactionDefinition.getAmount());
        newTransaction.setProcessed(false);
        newTransaction.setDate(OffsetDateTime.now());
        transactionDao.createTransaction(newTransaction);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * This method gets the transactions of a user.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getTransactions() {
        // get subject
        String subject = this.getCallerSubject();
        // get user
        User user = userDao.findUserByRegistryId(subject);
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User is not registered").build();
        }
        if (!user.isConsentGiven()) {
            return Response.status(Response.Status.CONFLICT).entity("User has not consented to program").build();
        }

        List<Transaction> transactions = transactionDao.findTransactionsByUser(user.getUserId());
        return Response.status(Response.Status.OK).entity(transactions).build();
    }

    /**
     * This method gets the transactions of a user.
     */
    @GET
    @Path("spending")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getCategory() {
        // get subject
        String subject = this.getCallerSubject();
        // get user
        User user = userDao.findUserByRegistryId(subject);
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User is not registered").build();
        }
        if (!user.isConsentGiven()) {
            return Response.status(Response.Status.CONFLICT).entity("User has not consented to program").build();
        }

        List<Category> categories = transactionDao.groupCategoriesForUser(user.getUserId());
        return Response.status(Response.Status.OK).entity(categories).build();
    }

    // TODO: require admin scope
    /**
     * This method updates a transaction.
     */
    @PUT
    @Path("reward/{transactionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateTransaction(@PathParam("transactionId") String transactionId, RewardTransactionDefinition rewardTransactionDefinition) {
        // Validate UUID is formatted correctly.
        try {
            UUID.fromString(transactionId);
        } catch(IllegalArgumentException iae) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid transaction id").build();
        }
        String userId = rewardTransactionDefinition.getUserId();
        try {
            UUID.fromString(userId);
        } catch(IllegalArgumentException iae) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid user id").build();
        }

        Transaction transaction = transactionDao.findTransactionById(transactionId, userId);
        if (transaction == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Transaction not found").build();
        }
        
        if (transaction.isProcessed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Transaction already processed").build();
        }

        transaction.setCategory(rewardTransactionDefinition.getCategory());
        transaction.setPointsEarned(rewardTransactionDefinition.getPointsEarned());
        transaction.setProcessed(true);
        transactionDao.updateTransaction(transaction);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
