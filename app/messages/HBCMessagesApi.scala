package messages

import java.net.URL
import javax.inject.Inject

import play.api.i18n.Messages.UrlMessageSource
import play.api.i18n.{ DefaultMessagesApi, Lang, Langs, Messages }
import play.api.{ Configuration, Environment }
import play.utils.Resources

import scala.collection.JavaConverters._

class HBCMessagesApi @Inject() (config: Configuration, env: Environment, langs: Langs) extends DefaultMessagesApi(env, config, langs) {

  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = true

  private def joinPaths(first: String, second: String) = {
    new java.io.File(first, second).getPath
  }

  override def loadMessages(file: String): Map[String, String] = {
    val cl = getClass.getClassLoader
    val messagesPath: String = "messages"
    val globalMessages: List[URL] = cl.getResources(joinPaths(messagesPath, file)).asScala.toList
    val bannerMessages: List[URL] = cl.getResources(joinPaths(joinPaths(messagesPath, config.getString("hbc.banner").get), file)).asScala.toList

    (bannerMessages ++ globalMessages).filterNot(Resources.isDirectory(cl, _)).reverse.map { messageFile =>
      Messages.parse(UrlMessageSource(messageFile), messageFile.toString).fold(e => throw e, identity)
    }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }
}
