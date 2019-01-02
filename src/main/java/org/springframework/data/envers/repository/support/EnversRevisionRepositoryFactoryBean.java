/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.persistence.EntityManager;

import org.hibernate.envers.DefaultRevisionEntity;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * {@link FactoryBean} creating {@link RevisionRepository} instances.
 * 
 * @author Oliver Gierke
 * @author Michael Igler
 */
public class EnversRevisionRepositoryFactoryBean<T extends RevisionRepository<S, ID, N>, S, ID extends Serializable, N extends Number & Comparable<N>>
		extends JpaRepositoryFactoryBean<T, S, ID> {

	private Class<?> revisionEntityClass;

	/**
	 * Creates a new {@link EnversRevisionRepositoryFactoryBean} for the given repository interface.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public EnversRevisionRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the revision entity class. Will default to {@link DefaultRevisionEntity}.
	 * 
	 * @param revisionEntityClass
	 */
	public void setRevisionEntityClass(Class<?> revisionEntityClass) {
		this.revisionEntityClass = revisionEntityClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean#createRepositoryFactory(javax.persistence.EntityManager)
	 */
	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new RevisionRepositoryFactory<T, ID, N>(entityManager, revisionEntityClass);
	}

	/**
	 * Repository factory creating {@link RevisionRepository} instances.
	 * 
	 * @author Oliver Gierke
	 */
	private static class RevisionRepositoryFactory<T, ID extends Serializable, N extends Number & Comparable<N>>
			extends JpaRepositoryFactory {

		private final RevisionEntityInformation revisionEntityInformation;
		private final EntityManager entityManager;

		/**
		 * Creates a new {@link RevisionRepositoryFactory} using the given {@link EntityManager} and revision entity class.
		 * 
		 * @param entityManager must not be {@literal null}.
		 * @param revisionEntityClass can be {@literal null}, will default to {@link DefaultRevisionEntity}.
		 */
		public RevisionRepositoryFactory(EntityManager entityManager, Class<?> revisionEntityClass) {

			super(entityManager);
			this.entityManager = entityManager;
			revisionEntityClass = revisionEntityClass == null ? DefaultRevisionEntity.class : revisionEntityClass;
			this.revisionEntityInformation = DefaultRevisionEntity.class.equals(revisionEntityClass)
					? new DefaultRevisionEntityInformation() : new ReflectionRevisionEntityInformation(revisionEntityClass);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactory#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata, javax.persistence.EntityManager)
		 */
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected EnversRevisionRepositoryImpl getTargetRepository(RepositoryInformation information) {

			JpaEntityInformation<T, Serializable> entityInformation = (JpaEntityInformation<T, Serializable>) getEntityInformation(
					information.getDomainType());

			return new EnversRevisionRepositoryImpl<T, ID, N>(entityInformation, revisionEntityInformation, entityManager);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactory#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
		 */
		@Override
		protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
			return EnversRevisionRepositoryImpl.class;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepository(java.lang.Class, java.lang.Object)
		 */
		@Override
		@SuppressWarnings("hiding")
		public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {

			if (RevisionRepository.class.isAssignableFrom(repositoryInterface)) {

				Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(repositoryInterface,
						RevisionRepository.class);
				Class<?> revisionNumberType = typeArguments[2];

				if (!revisionEntityInformation.getRevisionNumberType().equals(revisionNumberType)) {
					throw new IllegalStateException(String.format(
							"Configured a revision entity type of %s with a revision type of %s "
									+ "but the repository interface is typed to a revision type of %s!",
							repositoryInterface, revisionEntityInformation.getRevisionNumberType(), revisionNumberType));
				}
			}

			return super.getRepository(repositoryInterface, customImplementation);
		}
	}
}
