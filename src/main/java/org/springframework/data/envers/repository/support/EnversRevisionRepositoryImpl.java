/*
 * Copyright 2012-2015 the original author or authors.
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

import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.history.AnnotationRevisionMetadata;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static org.hibernate.envers.internal.tools.EntityTools.getTargetClassIfProxied;

/**
 * Repository implementation using Hibernate Envers to implement revision specific query methods.
 *
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 * @author Michael Igler
 */
public class EnversRevisionRepositoryImpl<T, ID extends Serializable, N extends Number & Comparable<N>>
		extends SimpleJpaRepository<T, ID> implements EnversRevisionRepository<T, ID, N> {

	private final EntityInformation<T, ?> entityInformation;
	private final RevisionEntityInformation revisionEntityInformation;
	private final EntityManager entityManager;

	/**
	 * Creates a new {@link EnversRevisionRepositoryImpl} using the given {@link JpaEntityInformation},
	 * {@link RevisionEntityInformation} and {@link EntityManager}.
	 *
	 * @param entityInformation         must not be {@literal null}.
	 * @param revisionEntityInformation must not be {@literal null}.
	 * @param entityManager             must not be {@literal null}.
	 */
	public EnversRevisionRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
			RevisionEntityInformation revisionEntityInformation, EntityManager entityManager) {

		super(entityInformation, entityManager);

		Assert.notNull(revisionEntityInformation);

		this.entityInformation = entityInformation;
		this.revisionEntityInformation = revisionEntityInformation;
		this.entityManager = entityManager;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionRepository#findLastChangeRevision(java.io.Serializable)
	 */
	@SuppressWarnings("unchecked") public Revision<N, T> findLastChangeRevision(ID id) {

		Class<T> type = entityInformation.getJavaType();
		AuditReader reader = AuditReaderFactory.get(entityManager);

		List<Number> revisions = reader.getRevisions(type, id);

		if (revisions.isEmpty()) {
			return null;
		}

		N latestRevision = (N) revisions.get(revisions.size() - 1);

		Class<?> revisionEntityClass = revisionEntityInformation.getRevisionEntityClass();

		Object revisionEntity = reader.findRevision(revisionEntityClass, latestRevision);
		RevisionMetadata<N> metadata = (RevisionMetadata<N>) getRevisionMetadata(revisionEntity);
		return new Revision<N, T>(metadata, reader.find(type, id, latestRevision));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.envers.repository.support.EnversRevisionRepository#findRevision(java.io.Serializable, java.lang.Number)
	 */
	@Override public Revision<N, T> findRevision(ID id, N revisionNumber) {

		Assert.notNull(id, "Identifier must not be null!");
		Assert.notNull(revisionNumber, "Revision number must not be null!");

		return getEntityForRevision(revisionNumber, id, AuditReaderFactory.get(entityManager));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionRepository#findRevisions(java.io.Serializable)
	 */
	@SuppressWarnings("unchecked") public Revisions<N, T> findRevisions(ID id) {

		Class<T> type = entityInformation.getJavaType();
		AuditReader reader = AuditReaderFactory.get(entityManager);
		List<? extends Number> revisionNumbers = reader.getRevisions(type, id);

		return revisionNumbers.isEmpty() ?
				new Revisions<N, T>(Collections.EMPTY_LIST) :
				getEntitiesForRevisions((List<N>) revisionNumbers, id, reader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionRepository#findRevisions(java.io.Serializable, org.springframework.data.domain.Pageable)
	 */
	@SuppressWarnings("unchecked") public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {

		Class<T> type = entityInformation.getJavaType();
		AuditReader reader = AuditReaderFactory.get(entityManager);
		List<Number> revisionNumbers = reader.getRevisions(type, id);

		int totalNumberOfRevisions = revisionNumbers.size();

		if (pageable.getOffset() > totalNumberOfRevisions) {
			return new PageImpl<Revision<N, T>>(Collections.<Revision<N, T>>emptyList(), pageable, 0);
		}

		// fetch only a page of revision numbers ordered properly
		Class cls = getTargetClassIfProxied(type);
		AuditQuery auditQuery = reader.createQuery().forRevisionsOfEntity(cls, cls.getName(), false, true)
				.addProjection(AuditEntity.revisionNumber()).add(AuditEntity.id().eq(id)).setFirstResult(pageable.getOffset())
				.setMaxResults(pageable.getPageSize());

		Revisions<N, T> revisions = getEntitiesForRevisions(addSort(auditQuery, pageable).getResultList(), id, reader);

		return new PageImpl<Revision<N, T>>(revisions.getContent(), pageable, revisionNumbers.size());
	}

	/**
	 * Updates the audit query to include sorting based upon
	 * properties of the revision or properties of the revised entity
	 *
	 * @param auditQuery to be modified
	 * @param pageable   the pagination spec containing sort rules
	 * @return the modified audit query
	 */
	private AuditQuery addSort(AuditQuery auditQuery, Pageable pageable) {
		if (auditQuery != null && pageable != null) {
			Sort sort = pageable.getSort();
			if (sort != null) {
				for (Sort.Order order : sort) {
					AuditProperty auditProperty = null;
					switch (SortProperty.from(order)) {
						case REVISION_NUMBER:
							auditProperty = AuditEntity.revisionNumber();
							break;
						case DYNAMIC_PROPERTY:
							// this is a sort property that depends upon the revision or entity
							String prop = order.getProperty();
							if (!StringUtils.isEmpty(prop)) {
								if (StringUtils.startsWithIgnoreCase(prop, REVISION_PROPERTY_PREFIX)) {
									auditProperty = AuditEntity.revisionProperty(prop.substring(REVISION_PROPERTY_PREFIX.length()));
								} else if (StringUtils.startsWithIgnoreCase(prop, AUDITED_ENTITY_PROPERTY_PREFIX)) {
									auditProperty = AuditEntity.property(prop.substring(AUDITED_ENTITY_PROPERTY_PREFIX.length()));
								}
							}
							break;
					}
					if (auditProperty != null) {
						auditQuery.addOrder(order.isAscending() ? auditProperty.asc() : auditProperty.desc());
					}
				}
			}
		}
		return auditQuery;
	}

	/**
	 * Returns the entities in the given revisions for the entitiy with the given id.
	 *
	 * @param revisionNumbers
	 * @param id
	 * @param reader
	 * @return
	 */
	@SuppressWarnings("unchecked") private Revisions<N, T> getEntitiesForRevisions(List<N> revisionNumbers, ID id,
			AuditReader reader) {

		Class<T> type = entityInformation.getJavaType();
		Map<N, T> revisions = new HashMap<N, T>(revisionNumbers.size());

		Class<?> revisionEntityClass = revisionEntityInformation.getRevisionEntityClass();
		Map<Number, Object> revisionEntities = (Map<Number, Object>) reader
				.findRevisions(revisionEntityClass, new HashSet<Number>(revisionNumbers));

		for (Number number : revisionNumbers) {
			revisions.put((N) number, reader.find(type, id, number));
		}

		return new Revisions<N, T>(toRevisions(revisions, revisionEntities));
	}

	/**
	 * Returns an entity in the given revision for the given entity-id.
	 *
	 * @param revisionNumber
	 * @param id
	 * @param reader
	 * @return
	 */
	@SuppressWarnings("unchecked") private Revision<N, T> getEntityForRevision(N revisionNumber, ID id,
			AuditReader reader) {

		Class<?> type = revisionEntityInformation.getRevisionEntityClass();

		T revision = (T) reader.findRevision(type, revisionNumber);
		Object entity = reader.find(entityInformation.getJavaType(), id, revisionNumber);

		return new Revision<N, T>((RevisionMetadata<N>) getRevisionMetadata(revision), (T) entity);
	}

	@SuppressWarnings("unchecked")
	private List<Revision<N, T>> toRevisions(Map<N, T> source, Map<Number, Object> revisionEntities) {

		List<Revision<N, T>> result = new ArrayList<Revision<N, T>>();

		for (Entry<N, T> revision : source.entrySet()) {

			N revisionNumber = revision.getKey();
			T entity = revision.getValue();
			RevisionMetadata<N> metadata = (RevisionMetadata<N>) getRevisionMetadata(revisionEntities.get(revisionNumber));
			result.add(new Revision<N, T>(metadata, entity));
		}

		Collections.sort(result);
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the {@link RevisionMetadata} wrapper depending on the type of the given object.
	 *
	 * @param object
	 * @return
	 */
	private RevisionMetadata<?> getRevisionMetadata(Object object) {
		if (object instanceof DefaultRevisionEntity) {
			return new DefaultRevisionMetadata((DefaultRevisionEntity) object);
		} else {
			return new AnnotationRevisionMetadata<N>(object, RevisionNumber.class, RevisionTimestamp.class);
		}
	}
}
