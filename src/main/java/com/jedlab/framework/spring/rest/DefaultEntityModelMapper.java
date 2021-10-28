package com.jedlab.framework.spring.rest;

import com.jedlab.framework.db.EntityModel;

public class DefaultEntityModelMapper<E extends EntityModel, T> implements EntityModelMapper<E, T> {

	@Override
	public T toModel(E entity) {
		return null;
	}

}
