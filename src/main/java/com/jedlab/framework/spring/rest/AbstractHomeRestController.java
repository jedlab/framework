package com.jedlab.framework.spring.rest;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jedlab.framework.db.EntityModel;
import com.jedlab.framework.spring.SpringUtil;
import com.jedlab.framework.spring.mvc.EntityWrapper;
import com.jedlab.framework.spring.service.AbstractEntityService;
import com.jedlab.framework.spring.validation.BindingErrorMessage;
import com.jedlab.framework.spring.validation.BindingValidationError;
import com.jedlab.framework.spring.validation.ValidationUtil;

/**
 * @author omidp
 *
 * @param <E>
 */
public abstract class AbstractHomeRestController<E extends EntityModel<?>, T> {

//    @Autowired
//    protected Validator validator;

	@Autowired
	protected MessageSource messageSource;

	AbstractEntityService<E> service;

	EntityModelMapper<T, E> entityModelMapper;

	public AbstractHomeRestController(AbstractEntityService<E> service) {
		this.service = service;
	}

	

	public void setEntityModelMapper(EntityModelMapper<T, E> entityModelMapper) {
		this.entityModelMapper = entityModelMapper;
	}



	@ResponseBody
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseMessage> post(@RequestBody T model, HttpServletRequest request, Errors errors)
			throws BindingValidationError {
//		createInstance(entity, request);
		E entity = toEntity(model);
		validate(entity, errors);
		getService().insert(entity);
		return ResponseEntity.ok(new ResponseMessage(SpringUtil.getMessage("successful", null), 0));
	}

	@ResponseBody
	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseMessage> put(@RequestBody String entity, @PathVariable("id") Long id, 
			HttpServletRequest request, Errors errors) throws BindingValidationError {
		E persistedEntity = service.readForUpdate(getEntityClass(), id, entity);
//		createInstance(persistedEntity, request);
		validate(persistedEntity, errors);
		getService().update(persistedEntity);
		return ResponseEntity.ok(new ResponseMessage(SpringUtil.getMessage("successful", null), 0));
	}

	@ResponseBody
	@DeleteMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseMessage> delete(@PathVariable("id") Long id)
			throws BindingValidationError {
		getService().delete(id);
		return ResponseEntity.ok(new ResponseMessage(SpringUtil.getMessage("successful", null), 0));
	}

//	protected void createInstance(E entity, HttpServletRequest request) {
//
//	}

	protected AbstractEntityService<E> getService() {
		return service;
	}
	
	private E toEntity(T model) {
		if (entityModelMapper == null) {
			try {
				return getEntityClass().getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return entityModelMapper.toModel(model);
	}

	private void validate(E validated, Errors errors) throws BindingValidationError {
		// BeanPropertyBindingResult bindingResult = new
		// BeanPropertyBindingResult(validated, Person.class.getSimpleName());
		// spring validator
//        validator.validate(validated, errors);        
		if(errors == null)
			errors = new BeanPropertyBindingResult(validated, getEntityClass().getName());
		if (getValidator() != null && getValidator().supports(getEntityClass()))
			getValidator().validate(validated, errors);
		if (errors.hasErrors()) {
			throw new BindingValidationError(errors.getAllErrors());
		}
	}

	protected Validator getValidator() {
		return null;
	}

	private Class<E> entityClass;

	public Class<E> getEntityClass() {
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
