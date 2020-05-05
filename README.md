
# Graphecule


Graphecule is a GraphQL **client** library which can **auto generate** client **Kotlin** code that matches the **entire graph model** represented by a GraphQL server **schema**. 

The generated client Kotlin code can then be used like a normal Kotlin library performing all the query/mutation operations exposed by the original GraphQL API. All these operations can be done as if calling a normal Kotlin call, without having to know anything about underlying GraphQL or Networking.

![](http://josesamuel.com/graphecule/graphecule.jpg)

### Why Graphecule?

* Auto generates entire GraphQL client model in Kotlin
* Perform any Query or Mutation using auto generated Kotlin classes
* Hides all complexities of GraphQL and networking. Use the auto generated classes like any plain Kotlin library.
* Just like GraphQL, the auto generated Kotlin client library will allow you to specify what exactly needs to be fetched.


Here is an example of querying for user name from the auto generated client code for GitHub GraphQL API

```Kotlin
        val gitHubQuery = Query.QueryRequest
            .Builder()
            //Fetch the user
            .fetchUser(
                User.UserRequest
                    .Builder()
                    .fetchName()      //Fetch name
                    .fetchLocation()  //Fetch location
                    .build(),
                "josesamuel"      //Arg for user query
            )
            
            .build()       //Builds the request
            .sendRequest() //Non blocking suspended call to send the request, that returns the parsed Query type instance

        //Directly access the fields requested
        println(gitHubQuery?.user?.name)
```

The auto generated code above shows that you can 

* Specify exactly what all fields needs to be "fetched"
* Build the request that exactly match what the GraphQL schema required, by providing all the arguments
* Call the API without having to know anything about GraphQL
* Client library internally takes care of GraphQL and network requests, sending the correct requests, and parsing the reply and giving the parsed result back as Kotlin Object instance


### Generating Client model


* Using the Jar
	* Download [Graphecule.jar](https://josesamuel.com/graphecule/dist/Graphecule.jar) 
	* Follow the instructions in [README.txt](https://josesamuel.com/graphecule/dist/README.txt)
* Programmatically
	* 	Include the dependencies for library in gradle (as given below)
	*  Build the client code using "GraphClientBuilder"

```kotlin
        val clientBuilder = GraphClientBuilder(
            graphServer,             // GraphQL server address
            outputLocation,          // Where to generate the client code
            clientLibraryPackageName // Package name under which to generate client
        )

        //Builds the client library
        clientBuilder.buildGraphClient()
```

* For authentication, optionally specify "HttpRequestAdapter"

```Kotlin
        val clientBuilder = GraphClientBuilder(
            graphServer,             // GraphQL server address
            outputLocation,          // Where to generate the client code
            clientLibraryPackageName, // Package name under which to generate client
            //Specify any headers to be send.
            //Can also specify OkHttpClient to add interceptors
            HttpRequestAdapter(mapOf("authorization" to "Bearer $gitHubKey"))
        )
```

### Perform Query or Set operations

* Use the inner XXXRequest.Builder class to generate the query or set requests. 
	* For Query class, the builder exposes "fetch" methods
	* For Mutation class, the builder exposes "invoke" methods, or use the direct invoke methods on the generated class that match the Mutation type

## Getting Graphecule


**Gradle dependency**


```groovy
dependencies {
    implementation 'com.josesamuel:graphecule:1.0.0'
}
```
	
	

## License


    Copyright 2020 Joseph Samuel

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


