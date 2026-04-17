/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import entities.exceptions.NonexistentEntityException;
import entities.exceptions.PreexistingEntityException;
import entities.exceptions.RollbackFailureException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.UserTransaction;
import java.util.List;

/**
 *
 * @author krasi
 */
public class OrdersJpaController implements Serializable {

    public OrdersJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Orders orders) throws PreexistingEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Car carId = orders.getCarId();
            if (carId != null) {
                carId = em.getReference(carId.getClass(), carId.getCarId());
                orders.setCarId(carId);
            }
            Customer customerId = orders.getCustomerId();
            if (customerId != null) {
                customerId = em.getReference(customerId.getClass(), customerId.getCustomerId());
                orders.setCustomerId(customerId);
            }
            em.persist(orders);
            if (carId != null) {
                carId.getOrdersCollection().add(orders);
                carId = em.merge(carId);
            }
            if (customerId != null) {
                customerId.getOrdersCollection().add(orders);
                customerId = em.merge(customerId);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findOrders(orders.getOrderId()) != null) {
                throw new PreexistingEntityException("Orders " + orders + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Orders orders) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Orders persistentOrders = em.find(Orders.class, orders.getOrderId());
            Car carIdOld = persistentOrders.getCarId();
            Car carIdNew = orders.getCarId();
            Customer customerIdOld = persistentOrders.getCustomerId();
            Customer customerIdNew = orders.getCustomerId();
            if (carIdNew != null) {
                carIdNew = em.getReference(carIdNew.getClass(), carIdNew.getCarId());
                orders.setCarId(carIdNew);
            }
            if (customerIdNew != null) {
                customerIdNew = em.getReference(customerIdNew.getClass(), customerIdNew.getCustomerId());
                orders.setCustomerId(customerIdNew);
            }
            orders = em.merge(orders);
            if (carIdOld != null && !carIdOld.equals(carIdNew)) {
                carIdOld.getOrdersCollection().remove(orders);
                carIdOld = em.merge(carIdOld);
            }
            if (carIdNew != null && !carIdNew.equals(carIdOld)) {
                carIdNew.getOrdersCollection().add(orders);
                carIdNew = em.merge(carIdNew);
            }
            if (customerIdOld != null && !customerIdOld.equals(customerIdNew)) {
                customerIdOld.getOrdersCollection().remove(orders);
                customerIdOld = em.merge(customerIdOld);
            }
            if (customerIdNew != null && !customerIdNew.equals(customerIdOld)) {
                customerIdNew.getOrdersCollection().add(orders);
                customerIdNew = em.merge(customerIdNew);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = orders.getOrderId();
                if (findOrders(id) == null) {
                    throw new NonexistentEntityException("The orders with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Orders orders;
            try {
                orders = em.getReference(Orders.class, id);
                orders.getOrderId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The orders with id " + id + " no longer exists.", enfe);
            }
            Car carId = orders.getCarId();
            if (carId != null) {
                carId.getOrdersCollection().remove(orders);
                carId = em.merge(carId);
            }
            Customer customerId = orders.getCustomerId();
            if (customerId != null) {
                customerId.getOrdersCollection().remove(orders);
                customerId = em.merge(customerId);
            }
            em.remove(orders);
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Orders> findOrdersEntities() {
        return findOrdersEntities(true, -1, -1);
    }

    public List<Orders> findOrdersEntities(int maxResults, int firstResult) {
        return findOrdersEntities(false, maxResults, firstResult);
    }

    private List<Orders> findOrdersEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Orders.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Orders findOrders(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Orders.class, id);
        } finally {
            em.close();
        }
    }

    public int getOrdersCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Orders> rt = cq.from(Orders.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
