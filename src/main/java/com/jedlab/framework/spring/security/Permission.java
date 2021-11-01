package com.jedlab.framework.spring.security;

public interface Permission {

	public boolean canRead();

	public boolean canCreate();

	public boolean canUpdate();

	public boolean canDelete();

}
