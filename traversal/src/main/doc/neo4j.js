var neo4j = require('neo4j-driver').v1;

var driver = neo4j.driver("bolt://192.168.1.214", neo4j.auth.basic("neo4j", "12345678"));
var session = driver.session();
session
  .run( "MATCH (u:User) WHERE u.name = 'Adam' RETURN u.name AS name" )
  .then( function( result ) {
    console.log( result.records[0].get("name") );
    session.close();
    driver.close();
  }, function (err) {
  console.error(err);
})