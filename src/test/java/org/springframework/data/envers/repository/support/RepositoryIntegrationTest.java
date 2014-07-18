/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.envers.Config;
import org.springframework.data.envers.sample.Country;
import org.springframework.data.envers.sample.CountryRepository;
import org.springframework.data.envers.sample.License;
import org.springframework.data.envers.sample.LicenseRepository;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for repositories.
 *
 * @author Oliver Gierke
 * @author Alexander Müller
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class RepositoryIntegrationTest {

	@Autowired
	private LicenseRepository licenseRepository;

	@Autowired
	private CountryRepository countryRepository;

	@Autowired
	private JpaTransactionManager transactionManager;

	@Before
	public void setUp() {
		licenseRepository.deleteAll();
		countryRepository.deleteAll();
	}

	@Test
	public void testNumberOfRevisions() {
		// Revision 0: Create initial revision of a license.
		License license = new License("ACME License 1.0");
		licenseRepository.save(license);

		// There should be one revision (rev 0).
		assertEquals(1, licenseRepository.findRevisions(license.getId()).getContent().size());

		// Create two countries.
		Country countryA = new Country("de", "Germany");
		Country countryB = new Country("se", "Sweden");
		countryRepository.save(countryA);
		countryRepository.save(countryB);

		// Get a fresh license.
		License revision0License = licenseRepository.findOne(license.getId());

		// Revision 1: Add the country A and save the license so that a new revision is created.
		revision0License.setCountries(new ArrayList<Country>(Arrays.asList(countryA, countryB)));
		licenseRepository.save(revision0License);

		// Since the countries collection has been replaced (rather than changed), a new revision is created.
		// NOTE: If you use getCountries().add() instead of setCountries(), you will NOT get a new revision.
		assertEquals(2, licenseRepository.findRevisions(license.getId()).getContent().size());

		// There should be two countries.
		License newLicense = licenseRepository.findOne(license.getId());
		assertEquals(2, newLicense.getCountries().size());
	}

	/**
	 * Hibernate Envers writes auditing tables when transactions are synchronized, i.e. saving an entity will not cause
	 * immediate revision information that can be relied on.
	 * <p/>
	 * Due to the nature of Spring AOP proxies (calls to methods annotated with @Transactional in the same class will
	 * not be executed in a transaction!), we are using the transaction template to make sure that transactions are
	 * where we expect them to be. Repositories are transactional, i.e. their return values are in a session-less state.
	 */
	@Test
	public void testTransactionBehavior() {
		// Create initial revision of a license.
		final Long licenseId = licenseRepository.save(new License("ACME License 1.0")).getId();

		// Create two countries.
		final Long countryAId = countryRepository.save(new Country("de", "Germany")).getId();
		final Long countryBId = countryRepository.save(new Country("se", "Sweden")).getId();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				License license = licenseRepository.findOne(licenseId);

				// Associate the countries with the license.
				license.getCountries().addAll(Arrays.asList(countryRepository.findOne(countryAId), countryRepository.findOne(countryBId)));

				// Create a new revision of the license that references the countries.
				License savedLicense = licenseRepository.save(license);
			}
		});

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				License license = licenseRepository.findOne(licenseId);

				Country countryA = license.getCountries().get(0);

				// Change a country...
				countryA.setCode("dk");
				countryA.setName("Denmark");

				// ... and save it so that a new revision of that country is created.
				countryRepository.save(countryA);
			}
		});

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// There should be a revision.
				Revision<Integer, License> lastLicenseChangeRevision = licenseRepository.findLastChangeRevision(licenseId);
				assertThat(lastLicenseChangeRevision, is(notNullValue()));

				// Get a page of revisions with 10 elements.
				Page<Revision<Integer, License>> licenseRevisionsPage = licenseRepository.findRevisions(licenseId, new PageRequest(0, 10));
				Revisions<Integer, License> licenseRevisions = new Revisions<Integer, License>(licenseRevisionsPage.getContent());

				// The latest revision should be the last change revision.
				assertThat(licenseRevisions.getLatestRevision(), is(lastLicenseChangeRevision));
			}
		});

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Get a page of revisions with 10 elements.
				Page<Revision<Integer, License>> licenseRevisionsPage = licenseRepository.findRevisions(licenseId, new PageRequest(0, 10));
				Revisions<Integer, License> licenseRevisions = new Revisions<Integer, License>(licenseRevisionsPage.getContent());

				// There should be two revisions of the license: without countries and with countries (de, se).
				assertEquals(2, licenseRevisions.getContent().size());

				List<Revision<Integer, License>> licenseRevisionLists = licenseRevisions.getContent();

				// Get the old license. There should be no country associated with this license.
				Revision<Integer, License> oldLicenseRevision = licenseRevisionLists.get(0);
				assertEquals(0, oldLicenseRevision.getEntity().getCountries().size());

				Revision<Integer, License> newLicenseRevision = licenseRevisionLists.get(1);
				newLicenseRevision.getEntity().getCountries().size();
				assertEquals(2, newLicenseRevision.getEntity().getCountries().size());
			}
		});


		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Get a list of revisions.
				Revisions<Integer, Country> countryRevisions = countryRepository.findRevisions(countryAId);

				// There should be two revisions of the country (de and dk).
				assertEquals(2, countryRevisions.getContent().size());
			}
		});
	}

	@Test
	public void returnsEmptyRevisionsForUnrevisionedEntity() {
		assertThat(countryRepository.findRevisions(100L).getContent(), is(hasSize(0)));
	}
}
