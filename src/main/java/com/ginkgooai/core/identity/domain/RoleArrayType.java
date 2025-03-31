package com.ginkgooai.core.identity.domain;

import com.ginkgooai.core.common.enums.Role;

public class RoleArrayType extends EnumArrayType<Role> {
	public RoleArrayType() {
		super(Role.class);
	}
}