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

## Contributing to Spring Data

Here are some ways for you to get involved in the community:

* Create [Github issues](https://github.com/spring-projects/spring-data-envers/issues) for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://spring.io/blog) to spring.io.

Before we accept a non-trivial patch or pull request we will need you to [sign the Contributor License Agreement](https://cla.pivotal.io/sign/spring). Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. If you forget to do so, you'll be reminded when you submit a pull request. Active contributors might be asked to join the core team, and given the ability to merge pull requests.

If you're an Eclipse user make sure you activate automatic application of the formatter (located at `etc/eclipse-formatter.xml`) and activate automatic formatting and organizing imports on save.
