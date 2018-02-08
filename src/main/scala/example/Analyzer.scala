package example

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import java.util.regex.Pattern

import boopickle.Default._

object Analyzer {
  def errSep() = Console.err.println("\n*******************")

  def main(args: Array[String]): Unit = {
    val bytes = Files.readAllBytes(Paths.get("data_dump"))
    val articles = Unpickle[List[(String, String)]].fromBytes(ByteBuffer.wrap(bytes))
    Console.err.println(s"Loaded information about ${articles.size} pages")

    val rewardsRegex = Pattern.compile("==[ ]*Rewards?[ ]*==(.+?)(==|$)", Pattern.DOTALL)
    val (rewardsFound, rewardsNotFound) = articles
      .map { case (title, text) => (title, text, rewardsRegex.matcher(text))}
      .partition(_._3.find())

    Console.err.println(s"Found: ${rewardsFound.size}, not found: ${rewardsNotFound.size}")
    errSep()
    rewardsNotFound.foreach { case (title, _, _) => Console.err.println(s"Not found for '$title'")}
    val suspicious = rewardsNotFound.filter(_._2.toLowerCase.contains("reward"))
    errSep()
    Console.err.println(s"Identified ${suspicious.size} suscicious non-matches:")
    suspicious.foreach { case (title, _, _) => Console.err.println(s"- $title") }
    errSep()

    val rewardsSections = rewardsFound
      .map { case (title, _, matcher) => (title, matcher.group(1)) }

    /*val articleIdx = 1
    println(articles(articleIdx)._2)
    println("***********************")
    println(rewardsSections(articleIdx)._2)*/

    val (withXp, withoutXp) = rewardsSections.partition { case(_, txt) => txt.toLowerCase.contains("xp")}
    Console.err.println(s"${withXp.size}/${rewardsSections.size} contain xp string. Those lacking:")
    withoutXp.foreach { case (title, rewards) => Console.err.println(s"### $title ###\n$rewards\n")}
    errSep()

    val xpStrs = withXp
      .flatMap { case (title, rs) => rs.toLowerCase().split("\n").filter(_.contains("xp")).map((title, _))}
    Console.err.println(s"Got ${xpStrs.length} lines containing XP string (from ${withXp.size} sections)")

    val xpRegex = Pattern.compile("(^|[^0-9].*?)(?<expValue>[0-9 \\.,]+)e?xp.*")
    val (xpExtracted, xpNotExtracted) = xpStrs
      .map { case (title, str) => (title, str, xpRegex.matcher(str))}
      .partition { case (title, str, m) => m.matches() }

    Console.err.println(s"Extracted ${xpExtracted.length} experience values")
    xpNotExtracted.foreach { case(title, line, _) => Console.err.println(s"Not extracted: '$title': '$line'")}


    val (xpNonEmpty, xpEmpty) = xpExtracted
      .map { case (title, str, m) => (title, str, m.group("expValue").replace(",", "").replace(".", "").trim) }
      .partition{ case (title, str, intStr) => intStr.nonEmpty}

    errSep()
    Console.err.println(s"Rejected ${xpEmpty.size} due to emptiness")
    xpEmpty.foreach { case (title, line, ext) => Console.err.println(s"Empty: '$title': '$line', ext: '$ext'")}

    Console.err.flush()
    println()

    val xpValues =  xpNonEmpty
      .map { case (title, str, intStr) => (title, str, intStr.toInt)}
      .sortBy(_._3)
      .foreach {
        case (title, str, xp) => println(s"$title: $xp ($str)")
      }

    // @todo search for stats boosts!
  }
}
