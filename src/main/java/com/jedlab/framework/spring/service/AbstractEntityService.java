package com.jedlab.framework.spring.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

import org.primefaces.model.SortOrder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jedlab.framework.exceptions.ServiceException;
import com.jedlab.framework.spring.dao.AbstractDAO;
import com.jedlab.framework.spring.security.Permission;
import com.jedlab.framework.spring.security.PermissionChecker;
import com.jedlab.framework.util.CollectionUtil;
import com.jedlab.framework.util.StringUtil;
import com.jedlab.framework.web.SortProperty;

/**
 * @author Omid Pourhadi
 *
 */
public abstract class AbstractEntityService<E>
{

    
    public static final ObjectMapper mapper = new ObjectMapper();
    
    private AbstractDAO<E> repository;
    
    protected TrxManager trxManager;
    
    private static final Logger logger = Logger.getLogger(AbstractEntityService.class.getName());

    public AbstractDAO<E> getDao()
    {
    	return repository;
    }

    protected Map<String, String> getHints()
    {
        return null;
    }
    
    public AbstractEntityService(AbstractDAO<E> repository, PlatformTransactionManager ptm)
    {
        this.repository = repository;
        this.trxManager = new TrxManager(ptm);
    }
    

	public Permission getPermission() {
		return PermissionChecker.BasePermission;
	}

	protected Sort applySort(List<SortProperty> sortFields)
    {
        if (CollectionUtil.isEmpty(sortFields))
            return null;
        List<Order> orders = new ArrayList<Sort.Order>();
        for (SortProperty sp : sortFields)
        {
            if (SortOrder.ASCENDING.equals(sp.getSortOrder()))
                orders.add(new Order(Direction.ASC, sp.getName()));
            else
                orders.add(new Order(Direction.DESC, sp.getName()));
        }
        if (CollectionUtil.isEmpty(orders))
            return null;
        return new Sort(orders);
    }

    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
    public void insert(E entity, boolean flush) throws ServiceException
    {
    	if(getPermission().canCreate() == false)
    		throw new ServiceException("Create operation is not allowed");
        boolean success = true;
        try
        {
            beforeInsert(entity);
            getDao().save(entity);
            if (flush)
                getDao().flush();
        }
        catch (ServiceException e)
        {
            success = false;
            throw e;
        }
        catch(DataIntegrityViolationException div)
        {
        	success = false;
        	throw div;
        }
        catch (ConstraintViolationException cve) 
        {
        	success = false;
        	throw cve;
        }
        catch(DataAccessException | TransactionException e)
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
    }

    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
    public void insert(E entity) throws ServiceException
    {
        insert(entity, true);
    }

    public void delete(Long id)
    {
    	if(getPermission().canDelete() == false)
    		throw new ServiceException("Delete operation is not allowed");
        getDao().deleteById(id);
    }

    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
    public void update(E entity, boolean flush) throws ServiceException
    {
    	if(getPermission().canUpdate() == false)
    		throw new ServiceException("Update operation is not allowed");
        boolean success = true;
        try
        {
            beforeUpdate(entity);
            getDao().save(entity);
            if (flush)
                getDao().flush();
        }
        catch (ServiceException e)
        {
            success = false;
            throw e;
        }
        catch(DataIntegrityViolationException div)
        {
        	success = false;
        	throw div;
        }
        catch (ConstraintViolationException cve) 
        {
        	success = false;
        	throw cve;
        }
        catch(DataAccessException | TransactionException e)
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

    }

    public void flush()
    {
        getDao().flush();
    }

    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
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

    @Transactional(readOnly = true)
    public E findById(Class<E> clz, Object id)
    {
        return getEntityManager().find(clz, id);
    }

    public Iterable<E> findAll()
    {
        return getDao().findAll();
    }

    public Iterable<E> findAll(Specification<E> spec)
    {
        return getDao().findAll(spec);
    }

    protected abstract EntityManager getEntityManager();

    @Transactional(readOnly = true)
    public Page<E> load(Pageable pageable, Class<E> clz, JPARestriction restriction)
    {
        return load(pageable, clz, restriction, null);
    }

    @Transactional(readOnly = true)
    public Page<E> load(Pageable pageable, Class<E> clz, JPARestriction restriction, Sort sort)
    {
    	if(getPermission().canRead() == false)
    		throw new ServiceException("Read operation is not allowed");
        List<E> result = new ArrayList<>();
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
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

        TypedQuery<E> createQuery = getEntityManager().createQuery(criteria);
        if(getHints() != null)
        {
        	for(Map.Entry<String, String> hint: getHints().entrySet())
        	{
        		createQuery = createQuery.setHint(hint.getKey(), hint.getValue());
        	}
        }
        createQuery.setFirstResult((int) pageable.getOffset());
        // set 0 for unlimited
        if (pageable.getPageSize() > 0)
            createQuery.setMaxResults(pageable.getPageSize());
        result = createQuery.getResultList();
        return PageableExecutionUtils.getPage(result, pageable, () -> {
            return count(clz, restriction);
        });
    }

    @Transactional(readOnly = true)
    public Long count(Class<E> clz, JPARestriction restriction)
    {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
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
        return (Long) getEntityManager().createQuery(criteria).getSingleResult();
    }

    @Transactional(readOnly = true)
    public List<E> load(int first, int pageSize, List<SortProperty> sortFields,
            Map<String, Object> filters, Class<E> clz, JPARestriction restriction)
    {
    	if(getPermission().canRead() == false)
    		throw new ServiceException("Read operation is not allowed");
        List<E> result = new ArrayList<>();
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
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
        if (CollectionUtil.isNotEmpty(sortFields))
        {
            List<javax.persistence.criteria.Order> orderList = new ArrayList();
            sortFields.forEach(item -> {
                if (SortOrder.ASCENDING.equals(item.getSortOrder()))
                    orderList.add(builder.asc(root.get(item.getName())));
                else
                    orderList.add(builder.desc(root.get(item.getName())));
            });
            criteria.orderBy(orderList);
        }

        TypedQuery<E> createQuery = getEntityManager().createQuery(criteria);
        if(getHints() != null)
        {
        	for(Map.Entry<String, String> hint: getHints().entrySet())
        	{
        		createQuery = createQuery.setHint(hint.getKey(), hint.getValue());
        	}
        }
        createQuery.setFirstResult(first);
        // set 0 for unlimited
        if (pageSize > 0)
            createQuery.setMaxResults(pageSize);

        result = createQuery.getResultList();
        return result;
    }
    
    public E readForUpdate(Class<E> clz, Long id, String jsonContent)
    {
            E entityOp = findById(clz, id);
            if (entityOp == null)
                throw new ServiceException("EntityNotFound");
            E entity = entityOp;
            try
            {
                mapper.readerForUpdating(entity).readValue(jsonContent);
            }
            catch (IOException e)
            {
                logger.info(e.getMessage());
            }
            return entity;
    }

}
