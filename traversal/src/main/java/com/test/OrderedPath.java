package com.test;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Paths;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.graphdb.RelationshipType.withName;

public class OrderedPath
{
    private static final RelationshipType R1 = withName( "R1" ), R2 = withName( "R2" ),
            R3 = withName( "R3" ), R4 = withName( "R4" ), R5 = withName( "R5" );
    static final File DB_PATH = new File( "target/neo4j-orderedpath-db" );
    static GraphDatabaseService db;

    public OrderedPath () throws IOException
    {
        FileUtils.deleteRecursively(DB_PATH);
        db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
    }

    public static void main( String[] args ) throws Exception {
        OrderedPath op = new OrderedPath();
        System.out.println(op.printPaths(op.findPaths(), op.createTheGraph()));
        op.shutdownGraph();
    }

    public Node createTheGraph()
    {
        try ( Transaction tx = db.beginTx() )
        {

            Node A = db.createNode();
            Node B = db.createNode();
            Node C = db.createNode();
            Node D = db.createNode();
            Node E = db.createNode();

            A.setProperty( "name", "A" );
            B.setProperty( "name", "B" );
            C.setProperty( "name", "C" );
            D.setProperty( "name", "D" );
            E.setProperty("name", "E");

            A.createRelationshipTo( B, R1 );
            B.createRelationshipTo( C, R2 );
            C.createRelationshipTo( D, R3 );
            A.createRelationshipTo( C, R2 );
            C.createRelationshipTo( E, R5 );
            D.createRelationshipTo(E, R4);

            tx.success();
            return A;
        }
    }

    public void shutdownGraph()
    {
        try
        {
            if ( db != null )
            {
                db.shutdown();
            }
        }
        finally
        {
            db = null;
        }
    }

    public TraversalDescription findPaths()
    {
        final ArrayList<RelationshipType> orderPaths = new ArrayList<RelationshipType>();
        orderPaths.add(R1);
        orderPaths.add(R2);
        orderPaths.add(R3);
        TraversalDescription td = db.traversalDescription().evaluator( new Evaluator(){
                    @Override
                    public Evaluation evaluate( final Path path )
                    {
                        if ( path.length() == 0 )
                        {
                            return Evaluation.EXCLUDE_AND_CONTINUE;
                        }
                        RelationshipType expectedType = orderPaths.get( path.length() - 1 );
                        boolean isExpectedType = path.lastRelationship()
                                .isType( expectedType );
                        boolean included = path.length() == orderPaths.size() && isExpectedType;
                        boolean continued = path.length() < orderPaths.size() && isExpectedType;
                        return Evaluation.of( included, continued );
                    }
                } )
                .uniqueness( Uniqueness.NODE_PATH );
        return td;
    }

    String printPaths( TraversalDescription td, Node A )
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String output = "";
            Traverser traverser = td.traverse( A );
            PathPrinter pathPrinter = new PathPrinter( "name" );
            for ( Path path : traverser )
            {
                output += Paths.pathToString( path, pathPrinter );
            }
            output += "\n";
            return output;
        }
    }

    static class PathPrinter implements Paths.PathDescriptor<Path>
    {
        private final String nodePropertyKey;

        public PathPrinter( String nodePropertyKey )
        {
            this.nodePropertyKey = nodePropertyKey;
        }

        @Override
        public String nodeRepresentation( Path path, Node node )
        {
            return "(" + node.getProperty( nodePropertyKey, "" ) + ")";
        }

        @Override
        public String relationshipRepresentation( Path path, Node from, Relationship relationship )
        {
            String prefix = "--", suffix = "--";
            if ( from.equals( relationship.getEndNode() ) )
            {
                prefix = "<--";
            }
            else
            {
                suffix = "-->";
            }
            return prefix + "[" + relationship.getType().name() + "]" + suffix;
        }
    }
}
