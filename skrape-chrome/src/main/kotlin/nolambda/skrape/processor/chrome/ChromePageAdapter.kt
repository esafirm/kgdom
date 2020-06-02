package nolambda.skrape.processor.chrome

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonElement
import nolambda.skrape.nodes.Attr
import nolambda.skrape.nodes.Container
import nolambda.skrape.nodes.Page
import nolambda.skrape.nodes.Query
import nolambda.skrape.processor.AbstractPageAdapter
import nolambda.skrape.processor.formatter.addFormatter
import nolambda.skrape.result.QuerySkrapeResult
import nolambda.skrape.result.SkrapeResult
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait

typealias ChromeParserResult = Pair<String, JsonElement>

class ChromePageAdapter(
    private val waitTimeInSecond: Long = DEFAULT_WAIT_TIME,
    driverFactory: () -> ChromeDriver = { ChromeDriver() }
) : AbstractPageAdapter<ChromeElement, ChromeParserResult, SkrapeResult>() {

    companion object {
        private const val DEFAULT_WAIT_TIME = 3L
        const val NO_WAIT_TIME = 0L
    }

    private val driver by lazy(driverFactory)
    private val waiter by lazy {
        if (waitTimeInSecond == NO_WAIT_TIME) {
            NoWait
        } else {
            WebChromeWaiter(WebDriverWait(driver, waitTimeInSecond))
        }
    }

    init {
        addFormatter(ChromeValueFormatter(waiter))
    }

    override fun requestPage(page: Page): ChromeElement {
        driver.get(page.pageInfo.path)
        return ChromeElement.Driver(driver)
    }

    override fun onHandleResult(page: Page, results: List<ChromeParserResult>): SkrapeResult {
        return QuerySkrapeResult(jsonObject(results))
    }

    override fun processQuery(query: Query, element: ChromeElement): ChromeParserResult = with(query) {
        val children = element.findElWait(waiter, cssSelector).map { webEl ->
            jsonObject(children.map {
                processElement(it, ChromeElement.Component(webEl))
            })
        }
        name to jsonArray(children)
    }

    override fun processContainer(container: Container, element: ChromeElement): ChromeParserResult = with(container) {
        val children = children.map {
            processElement(it, element)
        }
        name to jsonObject(children)
    }

    override fun processAttr(attr: Attr, element: ChromeElement): ChromeParserResult = with(attr) {
        name to element.attr(attrName).toJson()
    }

    override fun onEnd() {
        driver.close()
    }
}