package com.demo;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.io.fs.FileUtils;

public class ManualIndex
{
    private static final File DB_PATH = new File( "target/neo4j-store" );
    private static final String USERNAME_KEY = "username";
    private static GraphDatabaseService graphDb;
    private static Index<Node> nodeIndex;

    public static void main( final String[] args ) throws IOException
    {
        FileUtils.deleteRecursively( DB_PATH );

        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook();

        try ( Transaction tx = graphDb.beginTx() )
        {
            nodeIndex = graphDb.index().forNodes( "nodes" );
            for ( int id = 0; id < 100; id++ )
            {
                createAndIndexUser( idToUserName( id ) );
            }

            int idToFind = 45;
            String userName = idToUserName( idToFind );
            Node foundUser = nodeIndex.get( USERNAME_KEY, userName ).getSingle();

            System.out.println( "The username of user " + idToFind + " is "
                    + foundUser.getProperty( USERNAME_KEY ) );


            for ( Node user : nodeIndex.query( USERNAME_KEY, "*" ) )
            {
                nodeIndex.remove(  user, USERNAME_KEY,
                        user.getProperty( USERNAME_KEY ) );
                user.delete();
            }
            tx.success();
        }
        shutdown();
    }

    private static void shutdown()
    {
        graphDb.shutdown();
    }

    private static String idToUserName( final int id )
    {
        return "user" + id;
    }

    private static Node createAndIndexUser( final String username )
    {
        Node node = graphDb.createNode();
        node.setProperty( USERNAME_KEY, username );
        nodeIndex.add( node, USERNAME_KEY, username );
        return node;
    }

    private static void registerShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                shutdown();
            }
        } );
    }
}
