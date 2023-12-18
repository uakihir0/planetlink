package work.socialhub

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

class Sample {

    fun request() {
        runBlocking {
            val client = HttpClient()
            val response = client.get("https://ktor.io/")
            println(response.status)
            client.close()
        }
    }
}