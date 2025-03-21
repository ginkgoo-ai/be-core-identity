package com.ginkgooai.core.identity.specification;

import com.ginkgooai.core.identity.domain.UserInfo;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<UserInfo> findByEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return null; // No filtering if email is null or empty
            }
            return criteriaBuilder.equal(root.get("email"), email);
        };
    }

    public static Specification<UserInfo> hasNameLike(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return null; // No filtering if name is null or empty
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%");
        };
    }
}
