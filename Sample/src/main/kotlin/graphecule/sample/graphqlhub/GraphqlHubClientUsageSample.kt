package graphecule.sample.graphqlhub

//Un comment after running GraphqlHubClientBuilderSample

//
//import graphqlhub.graph.GraphQLHubAPI
//import graphqlhub.graph.GraphQLHubMutationAPI
//import graphqlhub.graph.input.SetValueForKeyInput
//import graphqlhub.graph.model.*
//import kotlinx.coroutines.runBlocking
//
///**
// * Sample to use GraphQlHub generated client code.
// */
//fun main() {
//
//    runBlocking {
//        queryTweetSample()
//        performMutationBuilderSample()
//        performMutationDirectSample()
//    }
//}
//
///**
// * Sample to query for tweets using the generated client code
// */
//private suspend fun queryTweetSample() {
//
//    val graphHub =
//        GraphQLHubAPI
//            .GraphQLHubAPIRequest
//            .Builder()
//            .fetchTwitter(
//                TwitterAPI.TwitterAPIRequest.Builder()
//                    .fetchSearch(
//                        Tweet.TweetRequest.Builder()
//                            .fetchId()
//                            .fetchText()
//                            .fetchUser(
//                                TwitterUser.TwitterUserRequest
//                                    .Builder()
//                                    .fetchId()
//                                    .fetchName()
//                                    .build()
//                            )
//                            .build(),
//                        "Trump"
//                    )
//                    .build()
//            )
//            .build()
//            .sendRequest()
//
//    graphHub.twitter?.search?.forEach {
//        println(it?.id)
//        println(it?.text)
//        println(it?.user?.id)
//        println(it?.user?.name)
//    }
//}
//
///**
// * Sample to perform mutations by building it using mutation request builder
// */
//private suspend fun performMutationBuilderSample() {
//    val graphHubReq =
//        GraphQLHubMutationAPI
//            .GraphQLHubMutationAPIRequest
//            .Builder()
//            .invokeKeyValue_setValue(
//                request = SetValueForKeyPayload.SetValueForKeyPayloadRequest.Builder()
//                    .fetchItem(
//                        KeyValueItem.KeyValueItemRequest.Builder()
//                            .fetchId()
//                            .fetchValue().build()
//                    )
//                    .fetchClientMutationId()
//                    .build(),
//                input = SetValueForKeyInput("1", "2", "3")
//            )
//            .build()
//
//    val graphHub = graphHubReq.invoke()
//
//    println(graphHub)
//    println(graphHub.keyValue_setValue?.clientMutationId)
//    println(graphHub.keyValue_setValue?.item?.id)
//    println(graphHub.keyValue_setValue?.item?.value)
//}
//
//
///**
// * Sample to perform a mutation by directly calling it
// */
//private suspend fun performMutationDirectSample() {
//    val result = GraphQLHubMutationAPI.invokeKeyValue_setValue(
//        request = SetValueForKeyPayload.SetValueForKeyPayloadRequest.Builder()
//            .fetchItem(
//                KeyValueItem.KeyValueItemRequest.Builder()
//                    .fetchId()
//                    .fetchValue().build()
//            )
//            .fetchClientMutationId()
//            .build(),
//        input = SetValueForKeyInput("1", "2", "3")
//    )
//
//    println(result?.item?.id)
//    println(result?.item?.value)
//    println(result?.clientMutationId)
//}
//
