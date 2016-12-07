package com.test;

import org.neo4j.driver.v1.*;

import java.util.Map;
import java.util.Set;

public class ConnectServer {
    public static void main( final String[] args ) throws Exception{
        Driver driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "12345678"));
        Session session = driver.session();

        session.run( "CREATE (u:User {name:'one', email:'one@com.cn'})" );

        StatementResult result = session.run( "MATCH (u) WHERE u.name = 'one' RETURN u" );
        while ( result.hasNext() )
        {
            Record record = result.next();
            Map<String, Object> map = record.get("u").asMap();
            Set<Map.Entry<String, Object>> set = map.entrySet();
            for(Map.Entry one : set){
                System.out.print(one.getKey() + "=" + one.getValue() + ";");
            }
            System.out.println();
        }

        session.close();
        driver.close();
    }
}
