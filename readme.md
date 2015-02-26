# Spring Data Envers #

This project is an extension of the [Spring Data JPA](http://github.com/SpringSource/spring-data-jpa) project to allow access to entity revisions managed by Hibernate Envers. The sources mostly originate from a contribution of Philipp Hügelmeyer [@hygl](https://github.com/hygl).

The core feature of the module consists of an implementation of the `RevisionRepository` of Spring Data Commons.

```java
public interface RevisionRepository<T, ID extends Serializable, N extends Number & Comparable<N>> {

	Revision<N, T> findLastChangeRevision(ID id);

	Revisions<N, T> findRevisions(ID id);

	Page<Revision<N, T>> findRevisions(ID id, Pageable pageable);
}
```

You can pull in this functionality to your repositories by simply additionally extending the interface just mentioned:


```java
interface PersonRepository extends RevisionRepository<Person, Long, Integer>, CrudRepository<Person, Long> {

  // Your query methods go here
}
```

To successfully activate the Spring Data Envers repository factory use the Spring Data JPA repositories namespace element's `factory-class` attribute:

```xml
<jpa:repositories base-package="com.acme.repositories"
                  factory-class="….EnversRevisionRepositoryFactoryBean" />
```

# Contributing to the project

If you're an Eclipse user make sure you activate automatic application of the formatter (located at `etc/eclipse-formatter.xml`) and activate automatic formatting and organizing imports on save.
