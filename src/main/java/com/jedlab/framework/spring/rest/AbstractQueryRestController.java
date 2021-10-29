package com.jedlab.framework.spring.rest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jedlab.framework.db.EntityModel;
import com.jedlab.framework.spring.mvc.Pager;
import com.jedlab.framework.spring.rest.QueryWhereParser.FilterProperty;
import com.jedlab.framework.spring.service.AbstractEntityService;
import com.jedlab.framework.spring.service.JPARestriction;
import com.jedlab.framework.spring.validation.BindingValidationError;
import com.jedlab.framework.util.StringUtil;

/**
 * @author omidp
 *
 * @param <E>
 */
public abstract class AbstractQueryRestController<E extends EntityModel, T> {

	protected static final int INITIAL_PAGE = 0;
	protected static final int INITIAL_PAGE_SIZE = 200;
	protected static final int BUTTONS_TO_SHOW = 5;

	private AbstractEntityService<E> service;

	private EntityModelMapper<E, T> entityModelMapper;

	Class<T> returnInstance;

	public AbstractQueryRestController(AbstractEntityService<E> service) {
		this.service = service;
	}

	public void setEntityModelMapper(EntityModelMapper<E, T> entityModelMapper) {
		this.entityModelMapper = entityModelMapper;
	}

	@ResponseBody
	@GetMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResultList<T>> get(@RequestParam("pageSize") Optional<Integer> pageSize,
			@RequestParam("page") Optional<Integer> page,
			@RequestParam(value = "filter", required = false) String filter,
			@RequestParam(value = "match", required = false, defaultValue = QueryWhereParser.AND) String match,
			Sort sort, @RequestHeader(value = "X-VIEWNAME", required = false) String viewName,
			HttpServletRequest request)
			throws BindingValidationError, UnsupportedEncodingException {
		int evalPageSize = pageSize.orElse(INITIAL_PAGE_SIZE);
		int evalPage = (page.orElse(0) < 1) ? INITIAL_PAGE : page.get() - 1;

		//
		QueryWhereParser qb = filter != null ? new QueryWhereParser(URLDecoder.decode(filter, "UTF-8"))
				: QueryWhereParser.EMPTY;
		if (qb != null)
			qb.setMatch(match);
		JPARestriction restriction = getRestriction(qb.getFilterProperties(), request);
		Page<E> list = getService().load(PageRequest.of(evalPage, evalPageSize), getEntityClass(),
				restriction, sort);
		Pager pager = new Pager(list.getTotalPages(), list.getNumber(), BUTTONS_TO_SHOW);
		BodyBuilder ok = ResponseEntity.ok();
		if (StringUtil.isNotEmpty(viewName))
			ok.header("X-VIEWNAME", viewName);
		//
		List<T> resultList = list.getContent().stream().map(this::toModel).collect(Collectors.toList());
		//
		Long count = getService().count(getEntityClass(), restriction);
		return ok.body(new ResultList<T>(evalPageSize, new ArrayList<>(resultList), pager.getStartPage(),
				pager.getEndPage(), count,
				list.getTotalPages(), getEntityClass()));
	}

	private T toModel(E entity) {
		if (entityModelMapper == null) {
			try {
				return returnInstance.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return entityModelMapper.toModel(entity);
	}

	protected JPARestriction getRestriction(List<FilterProperty> filterProperties, HttpServletRequest request) {
		return null;
	}

	protected AbstractEntityService<E> getService() {
		return service;
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
