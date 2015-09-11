/*
 * Copyright 2015 the original author or authors.
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

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * Repository implementation for both {@link org.springframework.data.repository.history.RevisionRepository} and
 * {@link org.springframework.data.querydsl.QuerydslPredicateExecutor} interfaces.
 *
 * @author Dmytro Iaroslavskyi
 */
public class QueryDslWithEnversRevisionRepository<T, ID extends Serializable, N extends Number & Comparable<N>>
		extends QuerydslJpaRepository<T, ID> implements EnversRevisionRepository<T, ID, N> {

	private final EnversRevisionRepositoryImpl<T, ID, N> delegateRepository;

	/**
	 * Creates a new {@link QueryDslWithEnversRevisionRepository} using the given {@link JpaEntityInformation},
	 * {@link RevisionEntityInformation} and {@link EntityManager}.
	 *
	 * @param entityInformation must not be {@literal null}.
	 * @param revisionEntityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 */
	public QueryDslWithEnversRevisionRepository(JpaEntityInformation<T, ID> entityInformation,
			RevisionEntityInformation revisionEntityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.delegateRepository = new EnversRevisionRepositoryImpl<T, ID, N>(entityInformation, revisionEntityInformation,
				entityManager);
	}

	@Override
	public Optional<Revision<N, T>> findLastChangeRevision(ID id) {
		return delegateRepository.findLastChangeRevision(id);
	}

	@Override
	public Optional<Revision<N, T>> findRevision(ID id, N revisionNumber) {
		return delegateRepository.findRevision(id, revisionNumber);
	}

	@Override
	public Revisions<N, T> findRevisions(ID id) {
		return delegateRepository.findRevisions(id);
	}

	@Override
	public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {
		return delegateRepository.findRevisions(id, pageable);
	}

}