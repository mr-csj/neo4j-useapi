from py2neo import Graph
graph = Graph(password="12345678")
print graph.data("MATCH (a:User) RETURN a.name LIMIT 4")