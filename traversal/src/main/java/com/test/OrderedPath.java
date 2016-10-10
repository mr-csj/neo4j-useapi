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
    private static final RelationshipType REL1 = withName( "REL1" ), REL2 = withName( "REL2" ), REL3 = withName( "REL3" );
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

            A.createRelationshipTo( C, REL2 );
            C.createRelationshipTo( D, REL3 );
            A.createRelationshipTo( B, REL1 );
            B.createRelationshipTo( C, REL2 );

            A.setProperty( "name", "A" );
            B.setProperty( "name", "B" );
            C.setProperty( "name", "C" );
            D.setProperty( "name", "D" );
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
        final ArrayList<RelationshipType> orderedPathContext = new ArrayList<RelationshipType>();
        orderedPathContext.add( REL1 );
        orderedPathContext.add(REL2);
        orderedPathContext.add(REL3);
        TraversalDescription td = db.traversalDescription()
                .evaluator( new Evaluator()
                {
                    @Override
                    public Evaluation evaluate( final Path path )
                    {
                        if ( path.length() == 0 )
                        {
                            return Evaluation.EXCLUDE_AND_CONTINUE;
                        }
                        RelationshipType expectedType = orderedPathContext.get( path.length() - 1 );
                        boolean isExpectedType = path.lastRelationship()
                                .isType( expectedType );
                        boolean included = path.length() == orderedPathContext.size() && isExpectedType;
                        boolean continued = path.length() < orderedPathContext.size() && isExpectedType;
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
