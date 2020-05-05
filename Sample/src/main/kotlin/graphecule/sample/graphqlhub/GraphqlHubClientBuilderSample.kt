package graphecule.sample.graphqlhub

import graphecule.client.GraphClientBuilder
import kotlinx.coroutines.runBlocking

/**
 * Sample usage to generate graphql client for GraphQlHub api
 */
fun main() {

    runBlocking {

        val graphqlHubApiHost = "https://www.graphqlhub.com/graphql"

        val clientBuilder = GraphClientBuilder(
            graphqlHubApiHost,
            "./Sample/src/main/kotlin",
            "graphqlhub.graph"
        )
        clientBuilder.buildGraphClient()
    }
}