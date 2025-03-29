package com.ginkgooai.core.identity.config.jpa;

import com.ginkgooai.core.common.utils.ContextUtils;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class SpringSecurityAuditorAware implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {
		return Optional.ofNullable(ContextUtils.getUserId());
	}
}