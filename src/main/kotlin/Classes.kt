import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

enum class Alignment{
    LG, NG, CG, LN, TN, CN, LE, NE, CE
}

enum class BonusCategory{
    OT, HF, TH,FL
}

data class ClassFeature(
    override val name:String,
    override val description:String,
    override val type:FeatureTypes= FeatureTypes.UTP,
):Feature

data class PClass(
    val name:String,
    val description: String,
    val hitDice: Int,
    val wealthDiceCount: Int,
    val skills: MutableList<String>,
    val skillPoints: Int,
    val bab:BonusCategory,
    val saves: List<BonusCategory>,
    val special: List<Map<String,ClassFeature>>,
    val classFeatures: List<ClassFeature>
)

fun scrapeClass(url: String):PClass?
{
    val classPage: Document
    try
    {
        classPage = Jsoup.connect(url).get()
    }
    catch (e:Exception)
    {
        return null
    }
    return classFromPage(classPage)
}

fun classFromPage(classPage: Document): PClass {
    var name:String = ""
    var description: String = ""
    var hitDice: Int = 0
    var wealthDiceCount: Int = 0
    var skills: MutableList<String> = mutableListOf()
    var skillPoints: Int = 0
    var bab:BonusCategory = BonusCategory.FL
    val saves: MutableList<BonusCategory> = MutableList(3){BonusCategory.OT}

    val special: MutableList<MutableMap<String,ClassFeature>> = mutableListOf()
    val classFeatures: MutableList<ClassFeature> = mutableListOf()

    //Name
    name = classPage.select("h1").text()

    val classData = classPage.select("div.article-content")
    val classTable = classPage.select("table:has(caption:matchesOwn(Table: ${name}))").first()

    //Class features and level distribution
    classTable?.select("tbody tr td:eq(5)")?.forEach {
            special.add(mutableMapOf())
            it.select("a").forEach{
                classData.select("h4 span a[name=${it.attr("href").drop(1)}]").first()?.parent()?.let {
                    var ftype = FeatureTypes.UTP
                    var fname = it.text()
                    """(.+?)\((\w*?)\)""".toRegex().find(fname)?.let {
                        fname = it.groupValues[1].trim()
                        ftype = FeatureTypes.valueOf(it.groupValues[2].uppercase())
                    }
                    var dsc = ""
                    classFeatures.find {
                        it.name == fname}
                        ?.also{ special.last().put(fname,it)}
                        ?:run{
                            var p = it.parent()!!.nextElementSibling()
                            while (p!=null && !p.tagName().matches("h\\d".toRegex())) {
                                dsc += p.text() + "\n"
                                p = p.nextElementSibling()
                            }
                            val cf =ClassFeature(fname,dsc,ftype)
                            classFeatures.add(cf)
                            special.last().put(fname,cf) }
                }
            }
        }

    //BaB, saves
    val ct = classTable?.select("tbody tr:eq(2)")?.first()?.let {
       //Bab
        when(it.select("td:eq(1)").first()?.text()?.toIntOrNull())
        {
            3 -> bab = BonusCategory.FL
            2 -> bab = BonusCategory.TH
            1 -> bab = BonusCategory.HF
        }
        //Saves
        for (i in 0..2)
        {
            when(it.select("td:eq(${i+2})").first()?.text()?.toIntOrNull())
            {
                1 -> saves[i] = BonusCategory.OT
                3 -> saves[i] = BonusCategory.TH
            }
        }
    }

    //Description, hitdie, starting wealth, skills, skillpoints
    classData.html().split("(?=(<h\\d.*?>.*?</h\\d>))".toRegex()).forEach{
        //Description, hitdie, wealth
        if (!it.contains("<h\\d.*?>.*?</h\\d>".toRegex()))
        {
            Jsoup.parse(it, "", Parser.xmlParser()).children().forEach {
                if (it.tagName()=="p")
                {
                    var t = it.text()
                    when
                    {
                        """^hit dic?e""".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(t) -> hitDice = "d(\\d+)".toRegex().find(t)?.groupValues?.get(1)?.toInt() ?: 0
                        t.startsWith("starting wealth",true) -> wealthDiceCount = "(\\d+)d\\d".toRegex().find(t)?.groupValues?.get(1)?.toInt() ?: 0
                        t.startsWith("alignment",true) -> {/*prerequisites should go here*/}
                        else -> description+= t + "\n"
                    }
                }
            }
        }
        //Skills, skillpoints
        else if(Jsoup.parse(it).select("#Class_Skills , #Class_skills").isNotEmpty())
        {
            var t = Jsoup.parse(it, "", Parser.xmlParser()).children().first()
            t = t?.nextElementSibling()
            t?.let {
                "are (.*?)\\.".toRegex().find(it.text())?.groupValues?.get(1)?.let {
                    skills = it.toString().split(",", " and").filter { it.isNotEmpty() }.toMutableList()
                }
            }
            t=t?.nextElementSibling()
            t?.let{
                    skillPoints = "(\\d+) \\+".toRegex().find(it.text())?.groupValues?.get(1)?.toInt() ?: 0
                }
           /* {
                if (it.tagName()=="p")
                {
                    var t = it.text()
                    when
                    {
                        t.startsWith("skill ranks",true) -> skillPoints = "(\\d+) \\+".toRegex().find(t)?.groupValues?.get(1)?.toInt() ?: 0
                        t.startsWith("the",true) ->
                        {
                            "are (.*?)\\.".toRegex().find(t)?.groupValues?.get(1)?.let {
                               skills = it.toString().split(","," and").filter{it.isNotEmpty()}.toMutableList()
                            }
                        }
                    }
                }

            */
            }
        }

    return PClass(name, description, hitDice, wealthDiceCount, skills, skillPoints, bab, saves, special, classFeatures)
}
