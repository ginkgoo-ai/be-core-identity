package com.ginkgooai.core.identity.domain;

import com.ginkgooai.core.identity.domain.enums.LoginMethod;

public class LoginMethodArrayType extends EnumArrayType<LoginMethod> {
	public LoginMethodArrayType() {
		super(LoginMethod.class);
	}
}