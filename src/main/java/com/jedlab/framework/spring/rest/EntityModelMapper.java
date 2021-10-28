package com.jedlab.framework.spring.rest;

public interface EntityModelMapper<E, T> {
	T toModel(E entity);
}