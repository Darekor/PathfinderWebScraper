import org.jsoup.Jsoup

const val PFSRD = "https://www.d20pfsrd.com"

fun main() {

    collectFeatsInfo().forEach { println(it) }
    collectRacesInfo().forEach { println(it) }
    collectClassesInfo().forEach { println(it) }
}

private fun collectFeatsInfo():List<Feat> {
    return Jsoup.connect("$PFSRD/feats").get()
        .select("h4 [id$=Feats] a[href]")
        .mapNotNull{ scrapeFeatPage(it.attr("href"))}
        .flatten()
}

private fun collectRacesInfo():List<Race> {
    return Jsoup.connect("$PFSRD/races")
        .get()
        .select("tbody tr td:first-of-type a")
        .mapNotNull{ scrapeRace(it.attr("href"), it.text()) }
}

private fun collectClassesInfo():List<PClass> {
    return Jsoup.connect("$PFSRD/classes")
        .get()
        .select("tbody tr td:first-of-type b a")
        .mapNotNull{ scrapeClass(it.attr("href")) }
}

