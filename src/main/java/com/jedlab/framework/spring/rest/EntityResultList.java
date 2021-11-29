package com.jedlab.framework.spring.rest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import org.omidbiz.core.axon.internal.Axon;
import org.omidbiz.core.axon.internal.IgnoreElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jedlab.framework.json.JacksonView;

@Axon
@JsonIgnoreProperties(value = { "typeName", "entityClass" })
//@JsonFilter("JsonViewFilter")
@JacksonView
public class EntityResultList<E> extends ResponseMessage implements ParameterizedType {
	private List<E> resultList;
	private long resultCount;
	private int totalPage;
	private Class<E> entityClass;

	public EntityResultList(String message, int code, List<E> resultList, long resultCount) {
		super(message, code);
		this.resultList = resultList;
		this.resultCount = resultCount;
	}
	
	public EntityResultList(ResponseMessage msg, List<E> resultList, long resultCount) {
		super(msg.getMessage(), msg.getCode());
		this.resultList = resultList;
		this.resultCount = resultCount;
	}

	public List<E> getResultList() {
		return resultList;
	}



	public int getTotalPage() {
		return totalPage;
	}

	public long getResultCount() {
		return resultCount;
	}

	@IgnoreElement
	@JsonIgnore
	@Override
	public Type[] getActualTypeArguments() {
		return new Type[] { getEntityClass() };
	}

	@IgnoreElement
	@JsonIgnore
	@Override
	public Type getRawType() {
		return resultList.getClass();
	}

	@IgnoreElement
	@JsonIgnore
	@Override
	public Type getOwnerType() {
		return null;
	}

	@IgnoreElement
	@JsonIgnore
	public Type getType() {
		return this;
	}

	

	@IgnoreElement
	@JsonIgnore
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