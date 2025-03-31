package com.ginkgooai.core.identity.domain;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Custom Hibernate type to handle enum arrays in PostgreSQL
 *
 * @param <T> Enum type to be persisted
 */
public abstract class EnumArrayType<T extends Enum<T>> implements UserType<List<T>> {

	private final Class<T> enumClass;

	protected EnumArrayType(Class<T> enumClass) {
		this.enumClass = enumClass;
	}

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public Class<List<T>> returnedClass() {
		return (Class<List<T>>) (Class<?>) List.class;
	}

	@Override
	public boolean equals(List<T> x, List<T> y) {
		return Objects.equals(x, y);
	}

	@Override
	public int hashCode(List<T> x) {
		return Objects.hashCode(x);
	}

	@Override
	public List<T> nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		Array array = rs.getArray(position);
		if (array == null) {
			return new ArrayList<>();
		}

		String[] elements = (String[]) array.getArray();
		List<T> result = new ArrayList<>(elements.length);

		for (String element : elements) {
			result.add(Enum.valueOf(enumClass, element));
		}

		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, List<T> value, int index, SharedSessionContractImplementor session) throws SQLException {
		if (value == null || value.isEmpty()) {
			st.setNull(index, Types.ARRAY);
			return;
		}

		String[] elements = new String[value.size()];
		for (int i = 0; i < value.size(); i++) {
			elements[i] = value.get(i).name();
		}

		Connection connection = session.getJdbcConnectionAccess().obtainConnection();
		Array array = connection.createArrayOf("varchar", elements);
		st.setArray(index, array);
	}

	@Override
	public List<T> deepCopy(List<T> value) {
		if (value == null) {
			return null;
		}
		return new ArrayList<>(value);
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(List<T> value) {
		return (Serializable) deepCopy(value);
	}

	@Override
	public List<T> assemble(Serializable cached, Object owner) {
		return deepCopy((List<T>) cached);
	}

	@Override
	public List<T> replace(List<T> original, List<T> target, Object owner) {
		return deepCopy(original);
	}
}