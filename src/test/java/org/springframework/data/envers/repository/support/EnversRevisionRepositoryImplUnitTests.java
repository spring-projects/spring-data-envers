/*
 * Copyright 2018-2020 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.service.Service;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * Unit tests for EversRevisionRepositoryImpl.
 *
 * @author Jens Schauder
 */
public class EnversRevisionRepositoryImplUnitTests {

	private static final int NON_EXISTING_ID = -999;

	JpaEntityInformation<?, ?> entityInformation = mock(JpaEntityInformation.class);
	RevisionEntityInformation revisionEntityInformation = mock(RevisionEntityInformation.class);
	SessionImplementor session = mock(SessionImplementor.class, RETURNS_DEEP_STUBS);
	EnversService enversService = mock(EnversService.class, RETURNS_DEEP_STUBS);
	EntityManager entityManager = mock(EntityManager.class);

	@Before
	public void mockHibernateInfrastructure() {

		when(entityInformation.getJavaType()).thenReturn((Class) DummyEntity.class);

		when(enversService.getEntitiesConfigurations().isVersioned(any(String.class))).thenReturn(true);

		when(session.isOpen()).thenReturn(true);
		when((Service) session.getFactory().getServiceRegistry().getService(EnversService.class)).thenReturn(enversService);

		when(entityManager.getDelegate()).thenReturn(session);
	}

	@Test // #146
	public void findRevisionShortCircuitsOnEmptyRevisionList() {

		failOnEmptyRevisions();

		EnversRevisionRepositoryImplUnderTest<?, Object, ?> repository = new EnversRevisionRepositoryImplUnderTest<>(
				entityInformation, revisionEntityInformation, entityManager);

		repository.findRevisions(-999, PageRequest.of(0, 5));
	}

	private void failOnEmptyRevisions() {

		// simulate failure to query with empty revisions list as Postgres does.
		when(enversService.getRevisionInfoQueryCreator().getRevisionsQuery(any(Session.class), eq(Collections.emptySet()))
				.getResultList()).thenThrow(HibernateException.class);
	}

	/**
	 * An extension for the {@link EnversRevisionRepositoryImpl} that skips accessing the AuditReader and always returns
	 * an empty List.
	 */
	private class EnversRevisionRepositoryImplUnderTest<T, ID, N extends Number & Comparable<N>>
			extends EnversRevisionRepositoryImpl<T, ID, N> {

		EnversRevisionRepositoryImplUnderTest(JpaEntityInformation<T, ?> entityInformation,
				RevisionEntityInformation revisionEntityInformation, EntityManager entityManager) {
			super(entityInformation, revisionEntityInformation, entityManager);
		}

		@Override
		List<Number> getRevisions(ID id, Class<T> type, AuditReader reader) {
			return Collections.emptyList();
		}
	}

	private static class DummyEntity {}
}
