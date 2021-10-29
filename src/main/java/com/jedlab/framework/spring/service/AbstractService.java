package com.jedlab.framework.spring.service;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.validation.ConstraintViolationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jedlab.framework.exceptions.ServiceException;
import com.jedlab.framework.util.CollectionUtil;
import com.jedlab.framework.util.StringUtil;

/**
 * @author Omid Pourhadi
 *
 */
public abstract class AbstractService<E>
{
    private static final Log logger = LogFactory.getLog(AbstractService.class);
    protected TrxManager trxManager;
    protected ObjectMapper mapper;


    public AbstractService(PlatformTransactionManager ptm)
    {
        this.trxManager = new TrxManager(ptm);
        this.mapper = new ObjectMapper();
    }

    protected Map<String, String> getHints()
    {
        return null;
    }

    public void insert(E entity, boolean flush) throws ServiceException
    {
        trxManager.doInTransaction(status -> {
            boolean success = true;
            try
            {
                beforeInsert(entity);
                getEntityManager().persist(entity);
                if (flush)
                    getEntityManager().flush();
            }
            catch (ServiceException e)
            {
                success = false;
                throw e;
            }
            catch (ConstraintViolationException | DataAccessException | TransactionException e)
            {
                success = false;
                throw new ServiceException(e);
            }
            catch (Exception e)
            {
                success = false;
                throw new ServiceException(e);
            }
            finally
            {
                afterInsert(entity, success);
            }
        });

    }

    public void insert(E entity) throws ServiceException
    {
        insert(entity, true);
    }

    public void delete(Long id)
    {
    	E find = getEntityManager().find(getEntityClass(), id);
        getEntityManager().remove(find);
    }

    public void update(E entity, boolean flush) throws ServiceException
    {
        trxManager.doInTransaction(status -> {

            boolean success = true;
            try
            {
                beforeUpdate(entity);
                getEntityManager().merge(entity);
                if (flush)
                    getEntityManager().flush();
            }
            catch (ServiceException e)
            {
                success = false;
                throw e;
            }
            catch (ConstraintViolationException | DataAccessException | TransactionException e)
            {
                success = false;
                throw new ServiceException(e);
            }
            catch (Exception e)
            {
                success = false;
                throw new ServiceException(e);
            }
            finally
            {
                afterUpdate(entity, success);
            }
        });

    }

    public void flush()
    {
        getEntityManager().flush();
    }

    public void update(E entity) throws ServiceException
    {
        update(entity, true);
    }

    protected void afterUpdate(E entity, boolean success)
    {

    }

    protected void beforeUpdate(E entity)
    {

    }

    private void metaData(Class<E> domainClass)
    {
        Metamodel metamodel = getEntityManager().getMetamodel();
        ManagedType<E> type = metamodel.managedType(domainClass);
        if (!(type instanceof IdentifiableType))
        {
            throw new ServiceException("The given domain class does not contain an id attribute!");
        }
        IdentifiableType<E> source = (IdentifiableType<E>) type;
        if (source.hasSingleIdAttribute())
        {
            SingularAttribute<? super E, ?> idAttribute = source.getId(source.getIdType().getJavaType());
        }
        else
        {
            throw new ServiceException("unsupported operator");
        }
    }

    protected void afterInsert(E entity, boolean success)
    {

    }

    protected void beforeInsert(E entity)
    {

    }

    public E findById(Class<E> clz, Object id)
    {
        return trxManager.execute(status -> getEntityManager().find(clz, id));
    }


    protected abstract EntityManager getEntityManager();

    public Page<E> load(Pageable pageable, JPARestriction restriction)
    {
        return load(pageable, getEntityClass(), restriction, null);
    }

    public Page<E> load(Pageable pageable, Class<E> clz, JPARestriction restriction, Sort sort)
    {
        return trxManager.execute(new TransactionCallback<Page<E>>() {

            @Override
            public Page<E> doInTransaction(TransactionStatus status)
            {
                EntityManager entityManager = getEntityManager();
                if (entityManager == null)
                    throw new UnsupportedOperationException("entityManager can nt be null");
                List<E> result = new ArrayList<>();
                CriteriaBuilder builder = entityManager.getCriteriaBuilder();
                CriteriaQuery<E> criteria = builder.createQuery(clz);
                Root<E> root = criteria.from(clz);
                criteria.select(root);
                if (restriction != null)
                {
                    if(restriction.distinct())
                        criteria.distinct(true);
                    Specification listSpec = restriction.listSpec(builder, criteria, root);
                    if (listSpec != null)
                    {
                        Predicate predicate = listSpec.toPredicate(root, criteria, builder);
                        if (predicate != null)
                            criteria.where(predicate);
                    }
                }
                if (sort != null)
                {
                    List<javax.persistence.criteria.Order> orderList = new ArrayList<>();
                    sort.forEach(s -> {
                        if (StringUtil.isNotEmpty(s.getProperty()))
                        {
                            if (s.isAscending())
                                orderList.add(builder.asc(root.get(s.getProperty())));
                            else
                                orderList.add(builder.desc(root.get(s.getProperty())));
                        }
                    });
                    if (CollectionUtil.isNotEmpty(orderList))
                        criteria.orderBy(orderList);
                }

                TypedQuery<E> createQuery = entityManager.createQuery(criteria);
                createQuery.setFirstResult((int) pageable.getOffset());
                // set 0 for unlimited
                if (pageable.getPageSize() > 0)
                    createQuery.setMaxResults(pageable.getPageSize());
                result = createQuery.getResultList();
                return PageableExecutionUtils.getPage(result, pageable, () -> {
                    return count(clz, restriction);
                });
            }
        });

    }

    public Long count(Class<E> clz, JPARestriction restriction)
    {
        return trxManager.execute(status -> {
            EntityManager entityManager = getEntityManager();
            if (entityManager == null)
                throw new UnsupportedOperationException("entityManager can nt be null");
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
            Root<E> root = criteria.from(clz);
            criteria.select(builder.count(root));
            if (restriction != null)
            {
                Specification listSpec = restriction.countSpec(builder, criteria, root);
                if (listSpec != null)
                {
                    Predicate predicate = listSpec.toPredicate(root, criteria, builder);
                    if (predicate != null)
                        criteria.where(predicate);
                }
            }
            return (Long) entityManager.createQuery(criteria).getSingleResult();
        });

    }

    public E readForUpdate(Long id, String jsonContent)
    {
        return trxManager.execute(status -> {
            E entity = getEntityManager().find(getEntityClass(), id);
            if (entity == null)
                throw new ServiceException("EntityNotFound");
            try
            {
                mapper.readerForUpdating(entity).readValue(jsonContent);
            }
            catch (IOException e)
            {
                logger.info("{}", e);
            }
            return entity;
        });
    }
    
    public Class<E> getEntityClass() {
    	Class<E> entityClass = null;
		if (entityClass == null) {
			Type type = getClass().getGenericSuperclass();
			if (type instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) type;
				if (paramType.getActualTypeArguments().length == 2) {
					if (paramType.getActualTypeArguments()[1] instanceof TypeVariable) {
						throw new IllegalArgumentException("Could not guess entity class by reflection");
					} else {
						entityClass = (Class<E>) paramType.getActualTypeArguments()[0];
					}
				} else {
					entityClass = (Class<E>) paramType.getActualTypeArguments()[0];
				}
			} else {
				throw new IllegalArgumentException("Could not guess entity class by reflection");
			}
		}
		return entityClass;
	}
    

}
