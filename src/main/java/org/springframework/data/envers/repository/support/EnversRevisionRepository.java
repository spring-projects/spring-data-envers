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

import java.io.Serializable;

import org.springframework.data.domain.Sort;
import org.springframework.data.history.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;

/**
 * Convenience interface to allow pulling in {@link JpaRepository} and {@link RevisionRepository} functionality in one
 * go.
 *
 * @author Oliver Gierke
 * @author Michael Igler
 */
@NoRepositoryBean
public interface EnversRevisionRepository<T, ID extends Serializable, N extends Number & Comparable<N>> extends
		RevisionRepository<T, ID, N>, JpaRepository<T, ID> {

	/**
	 * Returns the entity with the given ID in the given revision number.
	 *
	 * @param id must not be {@literal null}.
	 * @param revisionNumber must not be {@literal null}.
	 * @return
	 */
	Revision<N, T> findRevision(ID id, N revisionNumber);

    String REVISION_PROPERTY_PREFIX = "revisionProperty.";

    String AUDITED_ENTITY_PROPERTY_PREFIX = "entityProperty.";

    enum SortProperty {
        REVISION_NUMBER("revisionNumber"),
        DYNAMIC_PROPERTY;

        private final String outwardFacing;

        SortProperty(){
            outwardFacing = "";
        }

        SortProperty(String outwardFacing){
            this.outwardFacing = outwardFacing;
        }

        public String getOutwardFacing() {
            return outwardFacing;
        }

        public static SortProperty from(Sort.Order sortOrder){
            SortProperty ret = DYNAMIC_PROPERTY;
            for (SortProperty sortProperty : values()) {
                if (sortProperty.outwardFacing.equals(sortOrder.getProperty())){
                    ret = sortProperty;
                    break;
                }
            }
            return ret;
        }

    }
}
