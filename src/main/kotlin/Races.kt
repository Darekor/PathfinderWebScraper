import org.jsoup.Jsoup
import org.jsoup.nodes.Document

enum class FeatureTypes
{
    EX, SP, SU, RCL, UTP
}

data class Subrace(val name:String,
                   val sources:Map<String,String>,
                   val lore:String,
                   val features:List<RacialFeature>)

interface Feature{
    val name:String
    val description:String
    val type:FeatureTypes
}

data class RacialFeature(override val name:String,
                         val sources:Map<String,String>,
                         override val description:String,
                         val replaces:List<String> = listOf(),
                         override val type:FeatureTypes= FeatureTypes.UTP): Feature

data class Race(val name:String,
                val sources:Map<String,String>,
                val lore:String,
                val baseFeatures:List<RacialFeature>,
                val replacementFeatures:List<RacialFeature>,
                val favouredClassOptions: List<FavouredClassOption>)

data class FavouredClassOption(val race:String,
                               val pclass:String,
                               val benefit:String)

fun scrapeRace(url: String,raceName:String = ""):Race?
{
    val racePage: Document
    try
    {
        racePage = Jsoup.connect(url).get()
    }
    catch (e:Exception)
    {
        return null
    }
    return raceFromData(racePage,raceName)
}

fun raceFromData(raceData: Document, raceName:String = ""):Race {
    val sources: MutableMap<String, String> = mutableMapOf()
    var lore = ""
    val baseFeatures: MutableList<RacialFeature> = mutableListOf()
    val replacementFeatures: MutableList<RacialFeature> = mutableListOf()
    val favouredClassOptions: MutableList<FavouredClassOption> = mutableListOf()

    //var t = raceData.select("div.article-content").html().split("(?=(<h3>.*?<\\/h3>))".toRegex())
    raceData.select("div.article-content").html().split("(?=(<h3>.*?</h3>))".toRegex())
        .map {Jsoup.parse(it)}
        .forEach {
            when{
                (it.select("h3").isEmpty())->
                    it.select("p:not([class])").forEach{
                    lore+=it.text()+"\n" }
                (it.text().startsWith("Standard Racial Traits"))->
                    it.select("li").forEach{
                        val features = """<b>(.*?)</b>:?(.*)""".toRegex().matchEntire(it.html())?.groupValues
                        if (features != null) {
                          baseFeatures.add(RacialFeature(Jsoup.parse(features[1]).text(), mapOf(),Jsoup.parse(features[2]).text()))
                        }
                        //val featureSplit = it.text().split(':')
                        //baseFeatures.add(RacialFeature(featureSplit[0], mapOf(),featureSplit[1]))
                    }
                (it.text().startsWith("Alternate Racial Traits"))->
                    it.select("li").forEach{val features = """<b>(.*?)</b>:?(.*)""".toRegex().matchEntire(it.html())?.groupValues
                        if (features != null) {
                            replacementFeatures.add(RacialFeature(Jsoup.parse(features[1]).text(), mapOf(),Jsoup.parse(features[2]).text()))
                        }

                    }
                (it.text().startsWith("Favored Class Options"))->
                    it.select("li").forEach{val features = """<b>(.*?)</b>:?(.*)""".toRegex().matchEntire(it.html())?.groupValues
                        if (features != null) {
                            favouredClassOptions.add(FavouredClassOption(raceName,Jsoup.parse(features[1]).text(),Jsoup.parse(features[2]).text()))
                        }
            }
                }
        }

    return Race(raceName, sources, lore, baseFeatures, replacementFeatures,favouredClassOptions)
}
