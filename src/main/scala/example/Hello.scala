package example

import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.regex.Pattern

import info.bliki.wiki.dump.{IArticleFilter, Siteinfo, WikiArticle, WikiXMLParser}
import boopickle.Default._
import com.softwaremill.sttp._
import com.sun.net.httpserver.Authenticator.Success

import scala.util.Success

object Hello {
  val refTitle = "Quests (Inquisition)"

  class SideQuestCategoryFinder extends IArticleFilter {
    var sideQuestCategoryArticle: List[WikiArticle] = List()

    override def process(article: WikiArticle, siteinfo: Siteinfo): Unit = {
      if (article.getTitle equals refTitle) {
        sideQuestCategoryArticle ::= article
      }
    }
  }

  class ArticleFilter(val titlesToKeep: Set[String]) extends IArticleFilter {
    var savedArticles: List[WikiArticle] = List()

    override def process(article: WikiArticle, siteinfo: Siteinfo): Unit = {
      if (titlesToKeep.contains(article.getTitle)) {
        savedArticles ::= article
      }
    }
  }

  def wikiTitleToUrl(title: String) = title.replace(" ", "_")

  def main(args: Array[String]): Unit = {
    val dump_path = args(0)
    println(dump_path)
    val p = Paths.get(dump_path).toFile

    val sqcf = new SideQuestCategoryFinder
    new WikiXMLParser(p, sqcf).parse()

    assert(sqcf.sideQuestCategoryArticle.nonEmpty)
    /*
    sqcf.sideQuestCategoryArticle.zipWithIndex.foreach { case (article, idx) =>
      println(s"XXXX $idx: '${article.getTitle}' XXXX\n${article.getText}")
    }
    */
    val titlesPageText = sqcf.sideQuestCategoryArticle(0).getText
    val lines = titlesPageText.split("\n")
    val filteredLines = lines.iterator.drop(7)

    val regex = Pattern.compile("\\{\\{:(.+?)\\|.*")
    val questNames = filteredLines
      .map(regex.matcher(_))
      .filter(_.matches())
      .map(_.group(1))
      .toSet

    println(s"Found ${questNames.size} quest names")
    /*questNames.foreach(println)*/

    val af = new ArticleFilter(questNames)
    new WikiXMLParser(p, af).parse()
    println(s"Found ${af.savedArticles.length} pages")
    val dumpArticlesText: List[(String, String)] = af.savedArticles.map(a => (a.getTitle, a.getText))

    val foundTitles = af.savedArticles.map(_.getTitle)
    var missingPages = questNames -- foundTitles
    println(s"${missingPages.size} missing pages")
    /*missingPages.foreach(println)*/

    /* for some reason not all pages are present in dump - download them by querying server for rawtext */
    implicit val backend = HttpURLConnectionBackend()
    val (found, notFound) = missingPages
      .map(title => {
        val rq = sttp.get(uri"http://dragonage.wikia.com/wiki/${wikiTitleToUrl(title)}?action=raw")
        val resp = rq.send()

        (title, resp.body)
      })
      .partition { case(t, body) => body.isRight }

    println(s"Scraping failed for ${notFound.size} pages")
    /*notFound.foreach { case (title, Left(errorMsg)) => println(s"'$title' failed due to: $errorMsg")}*/
    /*found.foreach { case(title, Right(body)) => println(s"\nScrapping successful for $title:\n$body")}*/

    val downloadedAriclesText: List[(String, String)] = found.map { case(title, Right(body)) => (title, body) }.toList

    val allText = dumpArticlesText ++ downloadedAriclesText
    println(s"Saving data about ${allText.size} pages")
    val bytes = Pickle.intoBytes(allText)
    val f = new FileOutputStream("data_dump")
    f.write(bytes.array())
    f.close()
  }
}
