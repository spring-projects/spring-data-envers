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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.envers.Config;
import org.springframework.data.envers.sample.Country;
import org.springframework.data.envers.sample.CountryRepository;
import org.springframework.data.envers.sample.License;
import org.springframework.data.envers.sample.LicenseRepository;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Integration tests for repositories.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class RepositoryIntegrationTest {

	@ImportResource("classpath:application-context.xml")
	@Configuration
	static class RepoConfig extends Config {

	}

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

		Revision<Integer, License> revision = licenseRepository.findLastChangeRevision(license.id);
		assertThat(revision, is(notNullValue()));

		Page<Revision<Integer, License>> revisions = licenseRepository.findRevisions(license.id, new PageRequest(0, 10));
		Revisions<Integer, License> wrapper = new Revisions<Integer, License>(revisions.getContent());
		assertThat(wrapper.getLatestRevision(), is(revision));
	}

	@Test
	public void artficialTestDataTes() {
		// Repository is empty.
		Assert.assertEquals(0, countryRepository.findRevisions(100L).getContent().size());

	}
}
