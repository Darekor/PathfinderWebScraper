import org.jsoup.Jsoup
import org.jsoup.nodes.Document

data class Feat(override val name:String,
                val source:String?,
                val sourceSite:String?,
                val flavor: String?,
                val prerequisites: MutableList<String>,
                override val description: String,
                val normal: String?,
                val special: String?,
                val tags: MutableList<String>,
                override val type:FeatureTypes= FeatureTypes.UTP):Feature


fun featFromPage(featData:Document):Feat {
    var name = ""
    var source = ""
    var sourceSite = ""
    var flavor = ""
    var prerequisites = mutableListOf<String>()
    var description = ""
    var normal = ""
    var special = ""
    val tags = mutableListOf<String>()

    val header = featData.select("h1:first-of-type").text().split('(',',',')').filter {it.isNotEmpty() }.map{it.trim()}
    if (header.isNotEmpty())
    {
        name = header[0]
        tags.addAll(header.drop(1))
    }

    var state = 0

    featData.select("div.article-content>p, div.article-content>div>p").forEach{
        if(it.hasClass("description"))
            flavor+=it.text()
        else
        {
            val text = it.text()
            when
            {
                """^Prerequisite\(?s?\)?:""".toRegex().containsMatchIn(text)-> {prerequisites = text.skipFirst().replace(".","").split(';',',').toMutableList(); state=1}
                """^Benefit\(?s?\)?:""".toRegex().containsMatchIn(text) -> {description = text.skipFirst();state =2}
            text.startsWith("Normal:") -> {normal = text.skipFirst(); state =3}
            text.startsWith("Special:") -> {special = text.skipFirst(); state =4}
            state == 0 -> flavor+=it.text()
            state == 1 -> prerequisites+=it.text()
            state == 2 -> description+="\n"+it.text()
            state == 3 -> normal+="\n"+it.text()
            state == 4 -> special+="\n"+it.text()
            }
        }
    }
    val src = featData.select("div.section15 a")
    sourceSite = src.attr("href")
    source = src.text()

    return Feat(name, source, sourceSite, flavor, prerequisites, description, normal, special, tags)
}

fun String.skipFirst():String
{
    return this.split(" ",ignoreCase = true,limit=2)[1]
}


fun scrapeFeat(url: String):Feat?
{
    val featPage: Document
    try
    {
        featPage = Jsoup.connect(url).get()
    }
    catch (e:Exception)
    {
        return null
    }
    return featFromPage(featPage)
}

fun scrapeFeatPage(url: String) : List<Feat> {
    val featPage: Document
    try
    {
        featPage = Jsoup.connect(url).get()
    }
    catch (e:Exception)
    {
        return emptyList()
    }
    if (featPage.select(".ogn-childpages").isNotEmpty())
    {
       return featPage.select("ul.ogn-childpages li a")
           .parallelStream()
           .map{ scrapeFeat(it.attr("href"))}
           .toList()
           .mapNotNull { it }
    }
    else if (featPage.select("tr th:contains(feat)").isNotEmpty())
    {
        return featPage.select("table")
            .mapNotNull{ element ->
                element.select("tr td:first-of-type a")
                    .parallelStream()
                    .map{scrapeFeat(it.attr("href"))}
                    .toList()
                    .mapNotNull { it }
            }.flatten()
    }
    return emptyList()
}