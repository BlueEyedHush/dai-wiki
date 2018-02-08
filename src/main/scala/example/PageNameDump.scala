package example

import java.nio.file.Paths

import info.bliki.wiki.dump.{IArticleFilter, Siteinfo, WikiArticle, WikiXMLParser}

object PageNameDump {
    val refTitle = "Quests (Inquisition)"

  class PageNameDumper extends IArticleFilter {
    var pageCount = 0

    override def process(article: WikiArticle, siteinfo: Siteinfo): Unit = {
      println(article.getTitle)
      pageCount += 1
    }
  }

  def main(args: Array[String]): Unit = {
    val dump_path = args(0)
    println(dump_path)
    val p = Paths.get(dump_path).toFile

    val sqcf = new PageNameDumper
    new WikiXMLParser(p, sqcf).parse()
    println(s"\nFound ${sqcf.pageCount} pages in dump")
  }
}
