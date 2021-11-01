package com.jedlab.framework.spring.security;

public enum PermissionChecker implements Permission {

	BasePermission{

		@Override
		public boolean canRead() {
			return true;
		}

		@Override
		public boolean canCreate() {
			return true;
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public boolean canDelete() {
			return true;
		}
	};

}
