package com.ibm.codey.loyalty.catalog.dao;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import com.ibm.codey.loyalty.catalog.models.User;

@RequestScoped
public class UserDao {

    @PersistenceContext(name = "jpa-unit")
    private EntityManager em;

    public void createUser(User user) {
        em.persist(user);
    }

    public void updateUser(User user) {
        em.merge(user);
    }

    public User findUserByRegistryId(String subject) {
        try {
            return em.createNamedQuery("User.findUserByRegistryId", User.class)
                .setParameter("subject", subject).getSingleResult();
        } catch(NoResultException e) {
            return null;
        }
    }

}