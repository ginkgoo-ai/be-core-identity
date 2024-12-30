package com.benwk.ginkgoocoreidentity.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA converter to convert between Set<String> and database string representation
 */
@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
    
    private static final String DELIMITER = ",";

    /**
     * Converts Set<String> to database column string
     */
    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return String.join(DELIMITER, attribute);
    }

    /**
     * Converts database column string to Set<String>
     */
    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
