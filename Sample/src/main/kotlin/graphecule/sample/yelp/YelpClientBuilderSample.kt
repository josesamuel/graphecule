package graphecule.sample.yelp

import graphecule.client.GraphClientBuilder
import graphecule.common.HttpRequestAdapter
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Sample usage to generate graphql client for Yelp graphql api
 */
fun main() {

    //Notes :
    // 1 - Set git hub key in your local.properties
    //     YELP_KEY=<YOUR_KEY>
    //2 - Github has rate limits, so set the rate limit appropriately

    val properties = Properties()
    properties.load(FileInputStream(File("local.properties")))
    val yelpHubKey = properties["YELP_KEY"]
    if (yelpHubKey == null) {
        println("No 'YELP_KEY' found in local.properties")
        return
    }

    runBlocking {

        val gitHubApiHost = "https://api.yelp.com/v3/graphql"

        val clientBuilder = GraphClientBuilder(
            gitHubApiHost,
            "./Sample/src/main/kotlin",
            "yelp.graph",
            httpRequestAdapter = HttpRequestAdapter(
                requestHeaders = mapOf(
                    "authorization" to "Bearer $yelpHubKey"
                ),
                maxParallelRequests = 1,
                rateLimit = 1500
            )
        )
        clientBuilder.buildGraphClient()
    }
}