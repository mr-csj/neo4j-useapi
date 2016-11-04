package com.test;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.io.fs.FileUtils;

public class Friends
{
    public enum RelTypes implements RelationshipType
    {
        HOME_NODE,
        KNOWS,
        CO_WORKER
    }

    private static final File DB_PATH = new File( "target/neo4j-td" );
    private GraphDatabaseService graphDb;
    private long homeNodeId;

    public static void main( String[] args ) throws IOException
    {
        Friends matrix = new Friends();
        matrix.setUp();
        System.out.println( matrix.printFriends() );
        System.out.println( matrix.printWorkers() );
        matrix.shutdown();
    }

    public void setUp() throws IOException
    {
        FileUtils.deleteRecursively( DB_PATH );
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook();
        createNodespace();
    }

    public void shutdown()
    {
        graphDb.shutdown();
    }

    public void createNodespace()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            Node home = graphDb.createNode();
            homeNodeId = home.getId();

            Node user = graphDb.createNode();
            user.setProperty( "name", "User" );

            home.createRelationshipTo( user, RelTypes.HOME_NODE );

            Node one = graphDb.createNode();
            one.setProperty( "name", "One" );

            user.createRelationshipTo( one, RelTypes.KNOWS );

            Node first = graphDb.createNode();
            first.setProperty( "name", "First" );

            user.createRelationshipTo( first, RelTypes.KNOWS );
            first.createRelationshipTo( one, RelTypes.KNOWS );

            Node two = graphDb.createNode();
            two.setProperty( "name", "Two" );

            one.createRelationshipTo( two, RelTypes.KNOWS );
            first.createRelationshipTo( two, RelTypes.KNOWS );

            Node three = graphDb.createNode();
            three.setProperty( "name", "Three" );

            two.createRelationshipTo( three, RelTypes.KNOWS );

            Node four = graphDb.createNode();
            four.setProperty( "name", "Four" );

            three.createRelationshipTo( four, RelTypes.CO_WORKER );

            tx.success();
        }
    }

    private Node getEndNode(Long id)
    {
        return graphDb.getNodeById(id)
                .getSingleRelationship( RelTypes.HOME_NODE, Direction.OUTGOING )
                .getEndNode();
    }

    public String printFriends()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            Node neoNode = getEndNode(homeNodeId);
            int numberOfFriends = 0;
            String output = neoNode.getProperty( "name" ) + "'s friends:\n";
            Traverser friendsTraverser = getFriends( neoNode );
            for ( Path friendPath : friendsTraverser )
            {
                output += "At depth " + friendPath.length() + " => "
                        + friendPath.endNode()
                        .getProperty( "name" ) + "\n";
                numberOfFriends++;
            }
            output += "Number of friends found: " + numberOfFriends + "\n";
            return output;
        }
    }

    //广度优先
    private Traverser getFriends( final Node person )
    {
        TraversalDescription td = graphDb.traversalDescription()
                .breadthFirst()
                .relationships( RelTypes.KNOWS, Direction.OUTGOING )
                .evaluator( Evaluators.excludeStartPosition() );
        return td.traverse( person );
    }

    //深度优先
    private Traverser getFriendsDepth( final Node person )
    {
        TraversalDescription td = graphDb.traversalDescription()
                .depthFirst()
                .relationships( RelTypes.KNOWS, Direction.OUTGOING )
                .evaluator( Evaluators.excludeStartPosition() );
        return td.traverse( person );
    }

    public String printWorkers()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            String output = "co-worker:\n";
            int number = 0;
            Traverser traverser = findWorker(getEndNode(homeNodeId));
            for ( Path hackerPath : traverser )
            {
                output += "At depth " + hackerPath.length() + " => "
                        + hackerPath.endNode()
                        .getProperty( "name" ) + "\n";
                number++;
            }
            output += "Number of co-worker found: " + number + "\n";
            return output;
        }
    }

    private Traverser findWorker( final Node startNode )
    {
        TraversalDescription td = graphDb.traversalDescription()
                .depthFirst()
                .relationships( RelTypes.CO_WORKER, Direction.OUTGOING )
                .relationships( RelTypes.KNOWS, Direction.OUTGOING )
                .evaluator(Evaluators.includeWhereLastRelationshipTypeIs( RelTypes.CO_WORKER ) );
        return td.traverse( startNode );
    }

    private void registerShutdownHook()
    {
        Runtime.getRuntime()
                .addShutdownHook( new Thread()
                {
                    @Override
                    public void run()
                    {
                        graphDb.shutdown();
                    }
                } );
    }
}
