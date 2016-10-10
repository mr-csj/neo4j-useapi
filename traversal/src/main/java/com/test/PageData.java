package com.test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PageData {
    private static final File DB_PATH = new File( "target/neo4j-page.db" );
    private static GraphDatabaseService graphDb;


    public PageData() {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook();
    }

    public static void main( String[] args ) throws IOException
    {
        FileUtils.deleteRecursively(DB_PATH);
        //create data
        PageData pageData = new PageData();
        for(int i=0; i<20; i++) {
           pageData.createNode("test"+i);
        }

        //list page
        Map<String, Object> params = new HashMap<>();
        params.put( "name", "(?i)test.*");
        params.put("skip", 0*10);
        params.put("limit", 10);
        Map<String, Object> page = pageData.findPage(params);
        System.out.println("==page list==");
        for(Map<String, Object> list : (List<Map<String, Object>>)page.get("content")){
            System.out.println("id="+list.get("id")+";name="+list.get("name")+";create="+list.get("create"));
        }
        System.out.println("page total="+page.get("totalElements"));

        pageData.shutdown();
    }

    public Map<String, Object> findPage(Map<String, Object> params){
        Map<String, Object> page = new HashMap<>();
        try ( Transaction tx = graphDb.beginTx() ) {
            String query = "MATCH (n) WHERE n.name =~ {name} RETURN n skip {skip} limit {limit}";
            Result result = graphDb.execute(query, params);
            Iterator<Node> n_column = result.columnAs("n");

            List<Map<String, Object>> content = new ArrayList<>();
            while(n_column.hasNext()){
                Node node = n_column.next();

                Map<String, Object> data = new HashMap<>();
                data.put("id",node.getId());
                data.put("name", node.getProperty("name"));
                data.put("create", new Date((Long) node.getProperty("create")));

                content.add(data);
            }

            query = "MATCH (n) WHERE n.name =~ {name} RETURN count(n) as count";
            result = graphDb.execute(query, params);

            if(result.hasNext()){
                Map<String,Object> row = result.next();
                page.put("totalElements", row.get("count"));
            }

            page.put("content", content);

            tx.close();
        }
        return page;
    }

    public void createNode(String name){
        try ( Transaction tx = graphDb.beginTx() )
        {
            Node node = graphDb.createNode();
            node.setProperty("name", name);
            node.setProperty("create", new Date().getTime());
            System.out.println("create node id="+node.getId());
            tx.success();
        }
    }

    public void shutdown()
    {
        graphDb.shutdown();
    }

    private void registerShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
}
