/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.envers.repository.support;

import org.hibernate.envers.DefaultRevisionEntity;
import org.joda.time.DateTime;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.util.Assert;

/**
 * {@link RevisionMetadata} working with a {@link DefaultRevisionEntity}.
 * 
 * @author Oliver Gierke
 * @author Philip Huegelmeyer
 */
public class DefaultRevisionMetadata implements RevisionMetadata<Integer> {

	private final DefaultRevisionEntity entity;

	/**
	 * Creates a new {@link DefaultRevisionMetadata}.
	 * 
	 * @param entity must not be {@literal null}.
	 */
	public DefaultRevisionMetadata(DefaultRevisionEntity entity) {

		Assert.notNull(entity);
		this.entity = entity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionNumber()
	 */
	public Integer getRevisionNumber() {
		return entity.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	public DateTime getRevisionDate() {
		return new DateTime(entity.getTimestamp());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getDelegate()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDelegate() {
		return (T) entity;
	}
}
