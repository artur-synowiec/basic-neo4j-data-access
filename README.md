## Neo4j data access application ##

This is a simple wrapper application around Neo4j REST API

** Running the application **

#start Neo4j
cd <neo4j-installation>/bin
./neo4j start

#start the appliation
mvn clean spring-boot:run -Dspring.profiles.active=local


** The generic API endpoints **
The main api endpoint is generic and works off graph nodes in plural naming convention 

http://localhost:8090/api/<node>s.<format>?page=<page>&size=<page-size>
i.e.
http://localhost:8090/api/movies.json

# filtering by fields:
http://localhost:8090/api/movies.json?fields=title,released
# or
http://localhost:8090/api/movies.json?fields=title&fields=released

# searching:
http://localhost:8090/api/movies.json?title=Matrix&fields=title


** Raw API endpoints (as they come from Neo4j REST) **
http://localhost:8090/raw/movies.json

