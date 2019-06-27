/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.envers.repository.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.envers.DefaultRevisionEntity;
import org.springframework.data.history.RevisionMetadata;

/**
 * {@link RevisionMetadata} working with a {@link DefaultRevisionEntity}.
 * 
 * The entity/delegate itself gets ignored for {@link #equals(Object)} and {@link #hashCode()} since they depend on the
 * way they were obtained.
 *
 * @author Oliver Gierke
 * @author Philip Huegelmeyer
 * @author Jens Schauder
 */
@Value
public class DefaultRevisionMetadata implements RevisionMetadata<Integer> {

	private final @NonNull @Getter(AccessLevel.NONE) DefaultRevisionEntity entity;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionNumber()
	 */
	public Optional<Integer> getRevisionNumber() {
		return Optional.ofNullable(entity.getId());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	@Deprecated
	public Optional<LocalDateTime> getRevisionDate() {
		return getRevisionInstant().map(instant -> LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionInstant()
	 */
	@Override
	public Optional<Instant> getRevisionInstant() {
		return Optional.of(Instant.ofEpochMilli(entity.getTimestamp()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getDelegate()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDelegate() {
		return (T) entity;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultRevisionMetadata that = (DefaultRevisionMetadata) o;
		return getRevisionNumber().equals(that.getRevisionNumber())
				&& getRevisionInstant().equals(that.getRevisionInstant());
	}

	@Override
	public int hashCode() {

		return Objects.hash(getRevisionNumber(), getRevisionInstant());
	}
}
