package com.nhl.link.etl.mapper;

import java.util.Map;

import org.apache.cayenne.exp.Expression;

/**
 * A mapper that does transparent conversion between object key value and a
 * map-friendly key.
 * 
 * @since 1.1
 */
public class SafeMapKeyMapper<T> implements Mapper<T> {

	private Mapper<T> delegate;
	private KeyAdapter keyAdapter;

	public SafeMapKeyMapper(Mapper<T> delegate, KeyAdapter keyAdapter) {
		this.delegate = delegate;
		this.keyAdapter = keyAdapter;
	}

	@Override
	public Object keyForTarget(T target) {
		return keyAdapter.toMapKey(delegate.keyForTarget(target));
	}

	@Override
	public Object keyForSource(Map<String, Object> source) {
		return keyAdapter.toMapKey(delegate.keyForSource(source));
	}

	@Override
	public Expression expressionForKey(Object key) {
		key = keyAdapter.fromMapKey(key);
		return delegate.expressionForKey(key);
	}
}