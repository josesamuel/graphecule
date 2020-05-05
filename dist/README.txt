
To generate the GraphQl client for a given GraphQl server, run the following

java -jar Graphecule.jar <GRAPHQL_API_HOST> <PROPERTY_FILE>

GRAPHQL_API_HOST      : Provides the URL to the GraphQl Api Server
PROPERTY_FILE         : Optional. Provide any additional parameters as needed.
                        If file is not specified, by default looks for 'graphecule.properties'


Additional properties that can be provided through the above property file:

Optional: OutputLocation : Specifies where to generate the client code. Default current directory
OutputLocation=./out/


Optional: PackageName : Specifies under which package to generate the client code. Default 'graph'
PackageName=graphqlhub.graph.api


Optional: RateLimit : Specifies any throttling limit between graphql requests in ms. Default no limit
RateLimit=500

Optional: MaxParallelRequests : Specifies max parallel requests that can be send to graphql server. Default 8
MaxParallelRequests=4

#Any other key=value pairs will be added as http headers

#eg: To add a http header for authorization
authorization=Bearer XYZ



eg:

java -jar Graphecule.jar https://www.graphqlhub.com/graphql
