/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.set;

import java.util.HashSet;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.CacheMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.query.Query;
import org.hibernate.stat.CollectionStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/set/Mappings.hbm.xml"
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"
				),
				@Setting(
						name = AvailableSettings.USE_QUERY_CACHE, value = "true"
				)
		}
)
public class PersistentSetTest {

	@Test
	public void testWriteMethodDirtying(SessionFactoryScope scope) {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		scope.inTransaction(
				session -> {
					session.save( parent );
					session.flush();
					// at this point, the set on parent has now been replaced with a PersistentSet...
					PersistentSet children = (PersistentSet) parent.getChildren();

					assertFalse( children.add( child ) );
					assertFalse( children.isDirty() );

					assertFalse( children.remove( otherChild ) );
					assertFalse( children.isDirty() );

					HashSet otherSet = new HashSet();
					otherSet.add( child );
					assertFalse( children.addAll( otherSet ) );
					assertFalse( children.isDirty() );

					assertFalse( children.retainAll( otherSet ) );
					assertFalse( children.isDirty() );

					otherSet = new HashSet();
					otherSet.add( otherChild );
					assertFalse( children.removeAll( otherSet ) );
					assertFalse( children.isDirty() );

					assertTrue( children.retainAll( otherSet ) );
					assertTrue( children.isDirty() );
					assertTrue( children.isEmpty() );

					children.clear();
					session.delete( child );
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.delete( parent );
				}
		);
	}

	@Test
	public void testCollectionMerging(SessionFactoryScope scope) {
		Parent p = new Parent( "p1" );
		scope.inTransaction(
				session -> {
					Child child = new Child( "c1" );
					p.getChildren().add( child );
					child.setParent( p );
					session.save( p );
				}
		);


		CollectionStatistics stats = scope.getSessionFactory().getStatistics()
				.getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		Parent merged = (Parent) scope.fromTransaction(
				session ->
						session.merge( p )
		);

		assertEquals( 1, merged.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, "p1" );
					assertEquals( 1, parent.getChildren().size() );
					session.delete( parent );
				}
		);
	}

	@Test
	public void testCollectiondirtyChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( "p1" );
					Child child = new Child( "c1" );
					parent.getChildren().add( child );
					child.setParent( parent );
					session.save( parent );
				}
		);


		CollectionStatistics stats = scope.getSessionFactory().getStatistics()
				.getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.get( Parent.class, "p1" );
					assertEquals( 1, p.getChildren().size() );
					return p;
				}
		);


		assertEquals( 1, parent.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		scope.inTransaction(
				session -> {
					assertEquals( 1, parent.getChildren().size() );
					session.delete( parent );
				}
		);
	}

	@Test
	public void testCompositeElementWriteMethodDirtying(SessionFactoryScope scope) {
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		Container.Content c2 = new Container.Content( "c2" );

		scope.inTransaction(
				session -> {
					session.save( container );
					session.flush();
					// at this point, the set on container has now been replaced with a PersistentSet...
					PersistentSet children = (PersistentSet) container.getContents();

					assertFalse( children.add( c1 ) );
					assertFalse( children.isDirty() );

					assertFalse( children.remove( c2 ) );
					assertFalse( children.isDirty() );

					HashSet otherSet = new HashSet();
					otherSet.add( c1 );
					assertFalse( children.addAll( otherSet ) );
					assertFalse( children.isDirty() );

					assertFalse( children.retainAll( otherSet ) );
					assertFalse( children.isDirty() );

					otherSet = new HashSet();
					otherSet.add( c2 );
					assertFalse( children.removeAll( otherSet ) );
					assertFalse( children.isDirty() );

					assertTrue( children.retainAll( otherSet ) );
					assertTrue( children.isDirty() );
					assertTrue( children.isEmpty() );

					children.clear();
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.delete( container );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-2485")
	public void testCompositeElementMerging(SessionFactoryScope scope) {
		Container container = new Container( "p1" );
		scope.inTransaction(
				session -> {
					Container.Content c1 = new Container.Content( "c1" );
					container.getContents().add( c1 );
					session.save( container );
				}
		);


		CollectionStatistics stats = scope.getSessionFactory().getStatistics()
				.getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		container.setName( "another name" );

		scope.inTransaction(
				session -> {
					session.merge( container );
				}
		);

		assertEquals( 1, container.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		scope.inTransaction(
				session -> {
					Container c = session.get( Container.class, container.getId() );
					assertEquals( 1, container.getContents().size() );
					session.delete( container );
				}
		);

	}

	@Test
	@FailureExpected(jiraKey = "HHH-2485")
	public void testCompositeElementCollectionDirtyChecking(SessionFactoryScope scope) {
		Container c = new Container( "p1" );
		scope.inTransaction(
				session -> {
					Container.Content c1 = new Container.Content( "c1" );
					c.getContents().add( c1 );
					session.save( c );
				}
		);

		CollectionStatistics stats = scope.getSessionFactory().getStatistics()
				.getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		scope.inTransaction(
				session -> {
					Container c1 = session.get( Container.class, c.getId() );
					assertEquals( 1, c1.getContents().size() );
				}
		);


		assertEquals( 1, c.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		scope.inTransaction(
				session -> {
					Container c1 = session.get( Container.class, c.getId() );
					assertEquals( 1, c1.getContents().size() );
					session.delete( c1 );
				}
		);
	}

	@Test
	public void testLoadChildCheckParentContainsChildCache(SessionFactoryScope scope) {
		Parent p1 = new Parent( "p1" );
		Child c1 = new Child( "c1" );
		c1.setDescription( "desc1" );
		p1.getChildren().add( c1 );
		c1.setParent( p1 );
		Child otherChild = new Child( "c2" );
		otherChild.setDescription( "desc2" );
		p1.getChildren().add( otherChild );
		otherChild.setParent( p1 );

		scope.inTransaction(
				session -> session.save( p1 )
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, p1.getName() );
					assertTrue( parent.getChildren().contains( c1 ) );
					assertTrue( parent.getChildren().contains( otherChild ) );
				}
		);

		scope.inTransaction(
				session -> {
					Child child = session.get( Child.class, c1.getName() );
					assertTrue( child.getParent().getChildren().contains( child ) );
					session.clear();

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Child> criteria = criteriaBuilder.createQuery( Child.class );
					Root<Child> root = criteria.from( Child.class );
					criteria.where( criteriaBuilder.equal( root.get( "name" ), "c1" ) );
					child = session.createQuery( criteria ).uniqueResult();
//		child = ( Child ) session.createCriteria( Child.class, child.getName() )
//				.setCacheable( true )
//				.add( Restrictions.idEq( "c1" ) )
//				.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );
					assertTrue( child.getParent().getChildren().contains( otherChild ) );
					session.clear();

					CriteriaQuery<Child> criteriaQuery = criteriaBuilder.createQuery( Child.class );
					Root<Child> childRoot = criteriaQuery.from( Child.class );
					criteriaQuery.where( criteriaBuilder.equal( childRoot.get( "name" ), "c1" ) );
					Query<Child> query = session.createQuery( criteriaQuery );
					query.setCacheable( true );
					child = query.uniqueResult();

//		child = ( Child ) session.createCriteria( Child.class, child.getName() )
//				.setCacheable( true )
//				.add( Restrictions.idEq( "c1" ) )
//				.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );
					assertTrue( child.getParent().getChildren().contains( otherChild ) );
					session.clear();

					child = (Child) session.createQuery( "from Child where name = 'c1'" )
							.setCacheable( true )
							.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );

					child = (Child) session.createQuery( "from Child where name = 'c1'" )
							.setCacheable( true )
							.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );

					session.delete( child.getParent() );
				}
		);
	}

	@Test
	public void testLoadChildCheckParentContainsChildNoCache(SessionFactoryScope scope) {
		Parent p = new Parent( "p1" );
		Child c1 = new Child( "c1" );
		p.getChildren().add( c1 );
		c1.setParent( p );
		Child otherChild = new Child( "c2" );
		p.getChildren().add( otherChild );
		otherChild.setParent( p );

		scope.inTransaction(
				session -> session.save( p )
		);

		scope.inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					Parent parent = session.get( Parent.class, p.getName() );
					assertTrue( parent.getChildren().contains( c1 ) );
					assertTrue( parent.getChildren().contains( otherChild ) );
				}
		);

		scope.inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );

					Child child = session.get( Child.class, c1.getName() );
					assertTrue( child.getParent().getChildren().contains( child ) );
					session.clear();

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Child> criteria = criteriaBuilder.createQuery( Child.class );
					Root<Child> root = criteria.from( Child.class );
					criteria.where( criteriaBuilder.equal( root.get( "name" ), "c1" ) );

					child = session.createQuery( criteria ).uniqueResult();
//		child = ( Child ) session.createCriteria( Child.class, child.getName() )
//				.add( Restrictions.idEq( "c1" ) )
//				.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );
					assertTrue( child.getParent().getChildren().contains( otherChild ) );
					session.clear();

					child = (Child) session.createQuery( "from Child where name = 'c1'" ).uniqueResult();
					assertTrue( child.getParent().getChildren().contains( child ) );

					session.delete( child.getParent() );
				}
		);
	}
}
