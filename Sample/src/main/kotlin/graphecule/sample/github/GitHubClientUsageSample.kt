package graphecule.sample.github


//Un comment after running GitHubClientBuilderSample


//
//import github.graph.Query
//import github.graph.model.User
//import graphecule.common.HttpRequestAdapter
//import kotlinx.coroutines.runBlocking
//import java.io.File
//import java.io.FileInputStream
//import java.util.*
//
///**
// * Sample to use GitHub generated client code.
// */
//fun main() {
//
//    //Assumes client codes are already generated using [GitHubClientBuilderSample]
//
//    //Notes :
//    // 1 - Set git hub key in your local.properties
//    //     GIT_HUB_KEY=<YOUR_KEY>
//    //2 - Github has rate limits, so set the rate limit appropriately
//
//
//    val properties = Properties()
//    properties.load(FileInputStream(File("local.properties")))
//    val gitHubKey = properties["GIT_HUB_KEY"]
//    if (gitHubKey == null) {
//        println("No 'GIT_HUB_KEY' found in local.properties")
//        return
//    }
//
//    runBlocking {
//
//        val httpRequestAdapter = HttpRequestAdapter(
//            requestHeaders = mapOf(
//                "authorization" to "Bearer $gitHubKey"
//            ),
//            rateLimit = 1000
//        )
//
//        val queryRequest = Query.QueryRequest
//            .Builder()
//            .fetchUser(
//                User.UserRequest
//                    .Builder()
//                    .fetchName()
//                    .fetchLocation()
//                    .build(),
//                "josesamuel"
//            )
//            .build()
//
//
//        try {
//            val result = queryRequest.sendRequest(httpRequestAdapter)
//
//            println("Name : ${result.user?.name}")
//            println("Location : ${result.user?.location}")
//        } catch (exception: Exception) {
//        }
//        if (queryRequest.errorMessages?.isNotEmpty() == true) {
//            println("Request errors : ${queryRequest.errorMessages}")
//        }
//    }
//}