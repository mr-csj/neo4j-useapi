package com.demo;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;

public class Knows
{
    private static final File DB_PATH = new File( "target/neo4j-db" );
    private static GraphDatabaseService graphDb;

    public static void main( final String[] args ) throws IOException
    {
        Knows knows = new Knows();
        knows.create();
        knows.remove();
        knows.shutDown();
    }

    public Knows() throws IOException{
        FileUtils.deleteRecursively( DB_PATH );
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook( graphDb );
    }

    public void create() throws IOException
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            Label label = Label.label("Person");
            Node first = graphDb.createNode(label);
            first.setProperty("name", "first");
            Node second = graphDb.createNode(label);
            second.setProperty( "name", "second" );

            Relationship relationship = first.createRelationshipTo(second, RelTypes.KNOWS);

            System.out.println("create node name is " + first.getProperty("name"));
            System.out.println("create node name is " + second.getProperty("name"));
            System.out.println("create relationship type is " + relationship.getType());

            tx.success();
        }
    }

    public void remove()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            Label label = Label.label("Person");
            Node first = graphDb.findNode(label, "name", "first");
            Relationship relationship = first.getSingleRelationship(RelTypes.KNOWS, Direction.OUTGOING);

            System.out.println("delete node name is " + first.getProperty("name")
                    + ", relationship is KNOWS, end node name is "
                    + relationship.getEndNode().getProperty("name"));

            relationship.delete();
            relationship.getEndNode().delete();
            first.delete();

            tx.success();
        }
    }

    public void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        graphDb.shutdown();
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

    private static enum RelTypes implements RelationshipType
    {
        KNOWS
    }

}