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

import org.hibernate.envers.DefaultRevisionEntity;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultRevisionMetadata}.
 *
 * @author Benedikt Ritter
 * @author Jens Schauder
 */
public class DefaultRevisionMetadataUnitTests {

	private static final Instant NOW = Instant.now();;

	@Test // #112
	public void createsLocalDateTimeFromTimestamp() {

		DefaultRevisionEntity entity = new DefaultRevisionEntity();
		entity.setTimestamp(NOW.toEpochMilli());

		DefaultRevisionMetadata metadata = new DefaultRevisionMetadata(entity);

		assertThat(metadata.getRevisionDate()).hasValue(LocalDateTime.ofInstant(NOW, ZoneOffset.systemDefault()));
	}
}
