package apoc.create;

import apoc.path.Paths;
import apoc.util.TestUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static apoc.result.VirtualNode.ERROR_NODE_NULL;
import static apoc.result.VirtualRelationship.ERROR_END_NODE_NULL;
import static apoc.result.VirtualRelationship.ERROR_START_NODE_NULL;
import static apoc.util.TestUtil.testCall;
import static apoc.util.TestUtil.testResult;
import static org.junit.Assert.*;
import static org.neo4j.graphdb.Label.label;

public class CreateTest {

    public static final Label PERSON = Label.label("Person");

    @Rule
    public DbmsRule db = new ImpermanentDbmsRule();

    @Before public void setUp() throws Exception {
        TestUtil.registerProcedure(db,Create.class, Paths.class);
    }

    @Test
    public void testCreateNode() throws Exception {
        testCall(db, "CALL apoc.create.node(['Person'],{name:'John'})",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals(true, node.hasLabel(Label.label("Person")));
                    assertEquals("John", node.getProperty("name"));
                });
    }

    @Test
    public void testCreateNodeWithArrayProps() throws Exception {
        testCall(db, "CALL apoc.create.node(['Person'],{name:['John','Doe'],kids:[],age:[32,10]})",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals(true, node.hasLabel(Label.label("Person")));
                    assertArrayEquals(new String[]{"John", "Doe"}, (String[]) node.getProperty("name"));
                    assertArrayEquals(new String[]{}, (String[]) node.getProperty("kids"));
                    assertArrayEquals(new long[]{32, 10}, (long[]) node.getProperty("age"));
                });
    }

    @Test
    public void testSetProperty() throws Exception {
        testResult(db, "CREATE (n),(m) WITH n,m CALL apoc.create.setProperty([id(n),m],'name','John') YIELD node RETURN node",
                (result) -> {
                    Map<String, Object> row = result.next();
                    assertEquals("John", ((Node) row.get("node")).getProperty("name"));
                    row = result.next();
                    assertEquals("John", ((Node) row.get("node")).getProperty("name"));
                    assertEquals(false, result.hasNext());
                });
    }

    @Test
    public void testRemoveProperty() throws Exception {
        testCall(db, "CREATE (n:Foo {name:'foo'}) WITH n CALL apoc.create.setProperty(n,'name',null) YIELD node RETURN node",
                (row) -> assertEquals(false, ((Node) row.get("node")).hasProperty("name")));
        testCall(db, "CREATE (n:Foo {name:'foo'}) WITH n CALL apoc.create.removeProperties(n,['name']) YIELD node RETURN node",
                (row) -> assertEquals(false, ((Node) row.get("node")).hasProperty("name")));
    }

    @Test
    public void testRemoveRelProperty() throws Exception {
        testCall(db, "CREATE (n)-[r:TEST {name:'foo'}]->(m) WITH r CALL apoc.create.setRelProperty(r,'name',null) YIELD rel RETURN rel",
                (row) -> assertEquals(false, ((Relationship) row.get("rel")).hasProperty("name")));
        testCall(db, "CREATE (n)-[r:TEST {name:'foo'}]->(m) WITH r CALL apoc.create.removeRelProperties(r,['name']) YIELD rel RETURN rel",
                (row) -> assertEquals(false, ((Relationship) row.get("rel")).hasProperty("name")));
    }

    @Test
    public void testSetRelProperties() throws Exception {
        testResult(db, "CREATE (n)-[r:X]->(m),(m)-[r2:Y]->(n) WITH r,r2 CALL apoc.create.setRelProperties([id(r),r2],['name','age'],['John',42]) YIELD rel RETURN rel",
                (result) -> {
                    Map<String, Object> row = result.next();
                    Relationship r = (Relationship) row.get("rel");
                    assertEquals("John", r.getProperty("name"));
                    assertEquals(42L, r.getProperty("age"));
                    row = result.next();
                    r = (Relationship) row.get("rel");
                    assertEquals("John", r.getProperty("name"));
                    assertEquals(42L, r.getProperty("age"));
                    assertEquals(false, result.hasNext());
                });
    }

    @Test
    public void testSetRelProperty() throws Exception {
        testResult(db, "CREATE (n)-[r:X]->(m),(m)-[r2:Y]->(n) WITH r,r2 CALL apoc.create.setRelProperty([id(r),r2],'name','John') YIELD rel RETURN rel",
                (result) -> {
                    Map<String, Object> row = result.next();
                    assertEquals("John", ((Relationship) row.get("rel")).getProperty("name"));
                    row = result.next();
                    assertEquals("John", ((Relationship) row.get("rel")).getProperty("name"));
                    assertEquals(false, result.hasNext());
                });
    }

    @Test
    public void testSetProperties() throws Exception {
        testResult(db, "CREATE (n),(m) WITH n,m CALL apoc.create.setProperties([id(n),m],['name','age'],['John',42]) YIELD node RETURN node",
                (result) -> {
                    Map<String, Object> row = result.next();
                    assertEquals("John", ((Node) row.get("node")).getProperty("name"));
                    assertEquals(42L, ((Node) row.get("node")).getProperty("age"));
                    row = result.next();
                    assertEquals("John", ((Node) row.get("node")).getProperty("name"));
                    assertEquals(42L, ((Node) row.get("node")).getProperty("age"));
                    assertEquals(false, result.hasNext());
                });
    }

    @Test
    public void testVirtualNode() throws Exception {
        testCall(db, "CALL apoc.create.vNode(['Person'],{name:'John'})",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals(true, node.hasLabel(Label.label("Person")));
                    assertEquals("John", node.getProperty("name"));
                });
    }

    @Test
    public void testVirtualNodeFunction() throws Exception {
        testCall(db, "RETURN apoc.create.vNode(['Person'],{name:'John'}) as node",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals(true, node.hasLabel(Label.label("Person")));
                    assertEquals("John", node.getProperty("name"));
                });
    }

    @Test
    public void testCreateNodes() throws Exception {
        testResult(db, "CALL apoc.create.nodes(['Person'],[{name:'John'},{name:'Jane'}])",
                (res) -> {
                    Node node = (Node) res.next().get("node");
                    assertEquals(true, node.hasLabel(PERSON));
                    assertEquals("John", node.getProperty("name"));

                    node = (Node) res.next().get("node");
                    assertEquals(true, node.hasLabel(PERSON));
                    assertEquals("Jane", node.getProperty("name"));
                });
    }

    @Test
    public void testCreateRelationship() throws Exception {
        testCall(db, "CREATE (n),(m) WITH n,m CALL apoc.create.relationship(n,'KNOWS',{since:2010}, m) YIELD rel RETURN rel",
                (row) -> {
                    Relationship rel = (Relationship) row.get("rel");
                    assertEquals(true, rel.isType(RelationshipType.withName("KNOWS")));
                    assertEquals(2010L, rel.getProperty("since"));
                });
    }

    @Test
    public void testCreateVirtualRelationship() throws Exception {
        testCall(db, "CREATE (n),(m) WITH n,m CALL apoc.create.vRelationship(n,'KNOWS',{since:2010}, m) YIELD rel RETURN rel",
                (row) -> {
                    Relationship rel = (Relationship) row.get("rel");
                    assertEquals(true, rel.isType(RelationshipType.withName("KNOWS")));
                    assertEquals(2010L, rel.getProperty("since"));
                });
    }

    @Test
    public void testCreateVirtualRelationshipFunction() throws Exception {
        testCall(db, "CREATE (n),(m) WITH n,m RETURN apoc.create.vRelationship(n,'KNOWS',{since:2010}, m) AS rel",
                (row) -> {
                    Relationship rel = (Relationship) row.get("rel");
                    assertEquals(true, rel.isType(RelationshipType.withName("KNOWS")));
                    assertEquals(2010L, rel.getProperty("since"));
                });
    }

    @Test
    public void testCreatePattern() throws Exception {
        testCall(db, "CALL apoc.create.vPattern({_labels:['Person'],name:'John'},'KNOWS',{since:2010},{_labels:['Person'],name:'Jane'})",
                (row) -> {
                    Node john = (Node) row.get("from");
                    assertEquals(true, john.hasLabel(PERSON));
                    assertEquals("John", john.getProperty("name"));
                    Relationship rel = (Relationship) row.get("rel");
                    assertEquals(true, rel.isType(RelationshipType.withName("KNOWS")));
                    assertEquals(2010L, rel.getProperty("since"));
                    Node jane = (Node) row.get("to");
                    assertEquals(true, jane.hasLabel(PERSON));
                    assertEquals("Jane", jane.getProperty("name"));
                });
    }

    @Test
    public void testCreatePatternFull() throws Exception {
        testCall(db, "CALL apoc.create.vPatternFull(['Person'],{name:'John'},'KNOWS',{since:2010},['Person'],{name:'Jane'})",
                (row) -> {
                    Node john = (Node) row.get("from");
                    assertEquals(true, john.hasLabel(PERSON));
                    assertEquals("John", john.getProperty("name"));
                    Relationship rel = (Relationship) row.get("rel");
                    assertEquals(true, rel.isType(RelationshipType.withName("KNOWS")));
                    assertEquals(2010L, rel.getProperty("since"));
                    Node jane = (Node) row.get("to");
                    assertEquals(true, jane.hasLabel(PERSON));
                    assertEquals("Jane", jane.getProperty("name"));
                });
    }


    @Test
    public void testVirtualFromNodeFunction() throws Exception {
        testCall(db, "CREATE (n:Person{name:'Vincent', born: 1974} )  RETURN apoc.create.virtual.fromNode(n, ['name']) as node",
                (row) -> {
                    Node node = (Node) row.get("node");

                    assertEquals(true, node.hasLabel(Label.label("Person")));
                    assertEquals("Vincent", node.getProperty("name"));
                    assertNull(node.getProperty("born"));
                });
    }
    
    @Test
    public void testVirtualFromNodeShouldNotEditOriginalOne() {
        db.executeTransactionally("CREATE (n:Person {name:'toUpdate'})");

        testCall(db, "MATCH (n:Person {name:'toUpdate'})\n" +
                        "WITH apoc.create.virtual.fromNode(n, ['name']) as nVirtual\n" +
                        "CALL apoc.create.setProperty(nVirtual, 'ajeje', 0) YIELD node RETURN node\n",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals("toUpdate", node.getProperty("name"));
                    assertEquals(0L, node.getProperty("ajeje"));
                });
        
        testCall(db, "MATCH (n:Person {name:'toUpdate'})\n" +
                        "WITH apoc.create.virtual.fromNode(n, ['name']) as node\n" +
                        "SET node.ajeje = 0 RETURN node",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals("toUpdate", node.getProperty("name"));
                    assertFalse(node.hasProperty("ajeje"));
                });
        
        testCall(db, "MATCH (node:Person {name:'toUpdate'}) RETURN node",
                (row) -> {
                    Node node = (Node) row.get("node");
                    assertEquals("toUpdate", node.getProperty("name"));
                    assertFalse(node.hasProperty("ajeje"));
                });
    }
    
    @Test
    public void testClonePathShouldNotEditOriginalOne() {
        db.executeTransactionally("CREATE (n:Person {name:'toUpdate'})-[:MY_REL]->(:Another {alpha: 0})");
        testCall(db, "MATCH p=(n:Person {name:'toUpdate'})-[:MY_REL]->(:Another {alpha: 0})\n" +
                        "WITH p CALL apoc.create.clonePathToVirtual(p) YIELD path WITH nodes(path) AS nodes, relationships(path) as rels\n" +
                        "WITH nodes[0] as start, nodes[1] as end, rels[0] as rel\n" +
                        "CALL apoc.create.setProperty(start, 'ajeje', 0) YIELD node as startUpdated\n" +
                        "WITH startUpdated, end, rel\n" +
                        "CALL apoc.create.setProperties(end, ['brazorf'], ['abc']) YIELD node as endUpdated\n" +
                        "WITH startUpdated, endUpdated, rel\n" +
                        "CALL apoc.create.setRelProperty(rel, 'foo', 'bar') YIELD rel as relFirstSet\n" +
                        "WITH startUpdated, endUpdated, relFirstSet\n" + 
                        "CALL apoc.create.setRelProperties(relFirstSet, ['baz'], ['bar']) YIELD rel as relUpdated\n" +
                        "RETURN startUpdated, endUpdated, relUpdated",
                (row) -> {
                    Node start = (Node) row.get("startUpdated");
                    assertEquals("toUpdate", start.getProperty("name"));
                    assertEquals(0L, start.getProperty("ajeje"));
                    
                    Node end = (Node) row.get("endUpdated");
                    assertEquals(0L, end.getProperty("alpha"));
                    assertEquals("abc", end.getProperty("brazorf"));
                    
                    Relationship rel = (Relationship) row.get("relUpdated");
                    assertEquals("bar", rel.getProperty("foo"));
                    assertEquals("bar", rel.getProperty("baz"));
                });
        
        testCall(db, "MATCH p=(n:Person {name:'toUpdate'})-[:MY_REL]->(:Another {alpha: 0})\n" +
                        "WITH p CALL apoc.create.clonePathToVirtual(p) YIELD path WITH nodes(path) AS nodes, relationships(path) as rels\n" +
                        "WITH nodes[0] as start, nodes[1] as end, rels[0] as rel\n" +
                        "SET start.ajeje = 0, end.brazorf = 'abc', rel.foo = 'bar'\n" +
                        "RETURN start, end, rel",
                (row) -> {
                    Node start = (Node) row.get("start");
                    assertEquals("toUpdate", start.getProperty("name"));
                    assertFalse(start.hasProperty("ajeje"));
                    Node end = (Node) row.get("end");
                    assertTrue(end.hasLabel(label("Another")));
                    assertEquals(0L, end.getProperty("alpha"));
                    assertFalse(end.hasProperty("brazorf"));
                    
                    Relationship rel = (Relationship) row.get("rel");
                    assertFalse(rel.hasProperty("foo"));
                });

        testCall(db, "MATCH (start:Person {name:'toUpdate'}), (end:Another {alpha: 0})  RETURN start, end",
                (row) -> {
                    Node start = (Node) row.get("start");
                    assertFalse(start.hasProperty("ajeje"));
                    Node end = (Node) row.get("end");
                    assertFalse(end.hasProperty("brazorf"));
                });
    }
    
    @Test
    public void testVirtualPath() {
        db.executeTransactionally("CREATE p=(a:Test {foo: 7})-[:TEST]->(b:Baa:Baz {a:'b'})<-[:TEST_2 {aa:'bb'}]-(:Bar {one:'www'}), \n" +
                "q=(:Omega {alpha: 'beta'})<-[:TEST_3 {aa:'ccc'}]-(:Bar {one:'jjj'})");
        testCall(db, "MATCH p=(a:Test {foo: 7})-[:TEST]->(b:Baa:Baz {a:'b'})<-[:TEST_2 {aa:'bb'}]-(:Bar {one:'www'}) WITH p \n" +
                        "CALL apoc.create.clonePathToVirtual(p) YIELD path RETURN path",
                (row) -> assertionsFirstVirtualPath((Path) row.get("path")));

        testResult(db, "MATCH p=(a:Test {foo: 7})-[:TEST]->(b:Baa:Baz {a:'b'})<-[:TEST_2 {aa:'bb'}]-(:Bar {one:'www'}), \n" +
                        "q=(:Omega {alpha: 'beta'})<-[:TEST_3 {aa:'ccc'}]-(:Bar {one:'jjj'}) WITH [p, q] as paths \n" +
                        "CALL apoc.create.clonePathsToVirtual(paths) YIELD path RETURN path",
                (res) -> {
                    ResourceIterator<Path> paths = res.columnAs("path");
                    Path firstPath = paths.next();
                    assertionsFirstVirtualPath(firstPath);
                    Path secondPath = paths.next();
                    Iterator<Node> nodes = secondPath.nodes().iterator();
                    Node firstNode = nodes.next();
                    assertEquals(List.of(label("Omega")), firstNode.getLabels());
                    assertEquals(Map.of("alpha", "beta"), firstNode.getAllProperties());

                    Node secondNode = nodes.next();
                    assertEquals(List.of(label("Bar")), secondNode.getLabels());
                    assertEquals(Map.of("one", "jjj"), secondNode.getAllProperties());
                    assertFalse(nodes.hasNext());
                    
                    Iterator<Relationship> rels = secondPath.relationships().iterator();
                    Relationship relationship = rels.next();
                    assertEquals("TEST_3", relationship.getType().name());
                    assertEquals(Map.of("aa", "ccc"), relationship.getAllProperties());
                    assertFalse(rels.hasNext());
                    assertFalse(paths.hasNext());
                });
    }

    private void assertionsFirstVirtualPath(Path path) {
        Iterator<Node> nodes = path.nodes().iterator();
        Node firstNode = nodes.next();
        assertEquals(List.of(label("Test")), firstNode.getLabels());
        assertEquals(Map.of("foo", 7L), firstNode.getAllProperties());

        Node secondNode = nodes.next();
        assertEquals(Set.of(label("Baa"), label("Baz")), Iterables.asSet(secondNode.getLabels()));
        assertEquals(Map.of("a", "b"), secondNode.getAllProperties());
        
        Node thirdNode = nodes.next();
        assertEquals(List.of(label("Bar")), thirdNode.getLabels());
        assertEquals(Map.of("one", "www"), thirdNode.getAllProperties());
        assertFalse(nodes.hasNext());

        Iterator<Relationship> rels = path.relationships().iterator();
        Relationship firstRel = rels.next();
        assertEquals("TEST", firstRel.getType().name());
        assertTrue(firstRel.getAllProperties().isEmpty());
        
        Relationship secondRel = rels.next();
        assertEquals("TEST_2", secondRel.getType().name());
        assertEquals(Map.of("aa", "bb"), secondRel.getAllProperties());
        assertFalse(rels.hasNext());
    }

    @Test
    public void testValidationNodes() {
        assertionsError(ERROR_NODE_NULL,"RETURN apoc.create.virtual.fromNode(null, ['name']) as node");
        assertionsError(ERROR_START_NODE_NULL, "CREATE (n) WITH n CALL apoc.create.relationship(null,'KNOWS',{}, n) YIELD rel RETURN rel");
        assertionsError(ERROR_END_NODE_NULL, "CREATE (n) WITH n CALL apoc.create.relationship(n,'KNOWS',{}, null) YIELD rel RETURN rel");
        assertionsError(ERROR_START_NODE_NULL, "CREATE (m) RETURN apoc.create.vRelationship(null,'KNOWS',{}, m) AS rel");
        assertionsError(ERROR_END_NODE_NULL, "CREATE (n) WITH n CALL apoc.create.vRelationship(n,'KNOWS',{}, null) YIELD rel RETURN rel");
    }

    private void assertionsError(String expectedMessage, String query) {
        try {
            testCall(db, query, (row) -> fail("Should fail because of " + expectedMessage));
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            assertEquals(expectedMessage, rootCause.getMessage());
            assertTrue(rootCause instanceof RuntimeException);
        }
    }

}
