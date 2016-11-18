from py2neo import Graph
graph = Graph(password="12345678")
print graph.data("MATCH (u:User{name:'user'}) RETURN u")