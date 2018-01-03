/*
 * Copyright 2012-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

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
import org.springframework.data.history.RevisionSort;
import org.springframework.data.history.Revisions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for repositories.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class RepositoryIntegrationTests {

	@Autowired
	LicenseRepository licenseRepository;
	@Autowired
	CountryRepository countryRepository;

	@Before
	public void setUp() {
		licenseRepository.deleteAll();
		countryRepository.deleteAll();
	}

	@Test
	public void testname() {

		License license = new License();
		license.name = "Schnitzel";

		licenseRepository.save(license);

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		Country se = new Country();
		se.code = "se";
		se.name = "Schweden";

		countryRepository.save(se);

		license.laender = new HashSet<Country>();
		license.laender.addAll(Arrays.asList(de, se));

		licenseRepository.save(license);

		de.name = "Daenemark";

		countryRepository.save(de);

		Optional<Revision<Integer, License>> revision = licenseRepository.findLastChangeRevision(license.id);

		assertThat(revision).hasValueSatisfying(it -> {

			Page<Revision<Integer, License>> page = licenseRepository.findRevisions(license.id, PageRequest.of(0, 10));
			Revisions<Integer, License> revisions = Revisions.of(page.getContent());
			assertThat(revisions.getLatestRevision()).isEqualTo(it);
		});
	}

	@Test
	public void returnsEmptyRevisionsForUnrevisionedEntity() {
		assertThat(countryRepository.findRevisions(100L)).isEmpty();
	}

	/**
	 * @see #31
	 */
	@Test
	public void returnsParticularRevisionForAnEntity() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Germany";

		countryRepository.save(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions).hasSize(2);

		Iterator<Revision<Integer, Country>> iterator = revisions.iterator();
		Revision<Integer, Country> first = iterator.next();
		Revision<Integer, Country> second = iterator.next();

		assertThat(countryRepository.findRevision(de.id, first.getRequiredRevisionNumber())).hasValueSatisfying(it -> {
			assertThat(it.getEntity().name).isEqualTo("Deutschland");
		});

		assertThat(countryRepository.findRevision(de.id, second.getRequiredRevisionNumber())).hasValueSatisfying(it -> {
			assertThat(it.getEntity().name).isEqualTo("Germany");
		});
	}

	/**
	 * @see #55
	 */
	@Test
	public void considersRevisionNumberSortOrder() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		de.name = "Germany";

		countryRepository.save(de);

		Page<Revision<Integer, Country>> page = countryRepository.findRevisions(de.id,
				PageRequest.of(0, 10, RevisionSort.desc()));

		assertThat(page).hasSize(2);
		assertThat(page.getContent().get(0).getRequiredRevisionNumber())
				.isGreaterThan(page.getContent().get(1).getRequiredRevisionNumber());
	}

	/**
	 * @see #21
	 */
	@Test
	public void findsDeletedRevisions() {

		Country de = new Country();
		de.code = "de";
		de.name = "Deutschland";

		countryRepository.save(de);

		countryRepository.delete(de);

		Revisions<Integer, Country> revisions = countryRepository.findRevisions(de.id);

		assertThat(revisions).hasSize(2);
		assertThat(revisions.getLatestRevision().getEntity()) //
				.isNotNull() //
				.extracting(c -> c.name, c -> c.code) //
				.containsExactly(null, null);
	}
}
