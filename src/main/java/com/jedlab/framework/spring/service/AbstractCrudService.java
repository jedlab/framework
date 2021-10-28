package com.jedlab.framework.spring.service;

import java.util.Date;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.jedlab.framework.spring.dao.AbstractCrudDAO;
import com.jedlab.framework.spring.dao.AbstractDAO;

/**
 * @author Omid Pourhadi
 *
 */
@Transactional
public abstract class AbstractCrudService<E> extends AbstractEntityService<E>
{

    public AbstractCrudService(AbstractDAO<E> repository, PlatformTransactionManager ptm) {
		super(repository, ptm);
	}

	@Transactional
    public void deleteSoft(Long id)
    {
        ((AbstractCrudDAO)getDao()).deleteSoft(id, new Date());
    }
    
}
