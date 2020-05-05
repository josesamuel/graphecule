package graphecule.sample.github

import graphecule.client.GraphClientBuilder
import graphecule.common.HttpRequestAdapter
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Sample usage to generate graphql client for github graphql api
 */
fun main() {

    //Notes :
    // 1 - Set git hub key in your local.properties
    //     GIT_HUB_KEY=<YOUR_KEY>
    //2 - Github has rate limits, so set the rate limit appropriately

    val properties = Properties()
    properties.load(FileInputStream(File("local.properties")))
    val gitHubKey = properties["GIT_HUB_KEY"]
    if (gitHubKey == null) {
        println("No 'GIT_HUB_KEY' found in local.properties")
        return
    }

    runBlocking {

        val gitHubApiHost = "https://api.github.com/graphql"

        val clientBuilder = GraphClientBuilder(
            gitHubApiHost,
            "./Sample/src/main/kotlin",
            "github.graph",
            httpRequestAdapter = HttpRequestAdapter(
                requestHeaders = mapOf(
                    "authorization" to "Bearer $gitHubKey"
                ),
                rateLimit = 1000
            )
        )
        clientBuilder.buildGraphClient()
    }
}