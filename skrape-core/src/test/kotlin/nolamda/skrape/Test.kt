package nolamda.skrape

import com.google.gson.Gson
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec
import nolambda.skrape.Skrape
import nolambda.skrape.nodes.*
import nolambda.skrape.processor.jsoup.JsoupDocumentParser
import java.io.File

typealias StringSkrape = Skrape<String>

class SkrapeTest : StringSpec() {
    init {
        val skrape = Skrape(JsoupDocumentParser {
            proxy(null)
            userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
            referrer("google.com")
        })
        val gson = Gson()

        "Parsing local file" {
            val result = requestWithFile(skrape)
            val response = gson.fromJson(result, HackerNewsResponse::class.java)

            result shouldNotBe null
            response shouldNotBe null
            response.stories[0] shouldNotBe null
        }
        "Parsing from url" {
            requestWithUrl(skrape) shouldNotBe null
        }
    }
}

fun requestWithFile(skrape: StringSkrape): String {
    val classLoader = ClassLoader.getSystemClassLoader()
    val file = File(classLoader.getResource("index.html").file)

    return Page(file) {
        "items" to query("td a.storylink") {
            "text" to text()
            "detail" to container {
                "link" to attr("href")
            }
        }
    }.run {
        skrape.request(this)
    }
}


fun requestWithUrl(skrape: StringSkrape) {
    Page("https://news.ycombinator.com/") {
        "athing" to query("span.score") {
            "score" to text()
        }
        "items" to query("td a.storylink") {
            "text" to text()
            "link" to attr("href")
        }
    }.run {
        skrape.request(this)
    }.let {
            print(it)
        }
}