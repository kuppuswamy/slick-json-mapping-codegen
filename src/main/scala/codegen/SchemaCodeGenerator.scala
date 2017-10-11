package codegen

import slick.jdbc.MySQLProfile
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SchemaCodeGenerator extends App {
  val db = Database.forURL("jdbc:mysql://127.0.0.1/db?autoReconnect=true&nullNamePatternMatchesAll=true", user = "root", password = "access", driver = "com.mysql.cj.jdbc.Driver")

  import slick.codegen.SourceCodeGenerator

  val modelAction = MySQLProfile.createModel(Some(MySQLProfile.defaultTables))
  val modelFuture = db.run(modelAction)
  val codegenFuture = modelFuture.map(model => new SourceCodeGenerator(model) {

    def sqlDateTimeFormats =
      """
implicit object timestampFormat extends json.Format[Timestamp] {
  val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

  def reads(json: JsValue) = {
    val str = json.as[String]
    JsSuccess(new Timestamp(format.parse(str).getTime))
  }

  def writes(ts: Timestamp) = JsString(format.format(ts))
}
implicit object timeFormat extends json.Format[Time] {
  val format = new SimpleDateFormat("HH:mm:ss")

  def reads(json: JsValue) = {
    val str = json.as[String]
    JsSuccess(new Time(format.parse(str).getTime))
  }

  def writes(t: Time) = JsString(format.format(t))
}
implicit object dateFormat extends json.Format[Date] {
  val format = new SimpleDateFormat("yyyy-MM-dd")

  def reads(json: JsValue) = {
    val str = json.as[String]
    JsSuccess(new Date(format.parse(str).getTime))
  }

  def writes(d: Date) = JsString(format.format(d))
}
      """.trim

    override def code = "import play.api.libs.json._" + "\n" +
      "import play.api.libs.json" + "\n" +
      "import java.text.SimpleDateFormat" + "\n" +
      "import java.sql.{Date, Time, Timestamp}" + "\n" +
      sqlDateTimeFormats + "\n" + super.code

    override def Table = new Table(_) {
      override def EntityType = new EntityType {
        override def code = {
          val args = columns.map(c =>
            c.default.map(v =>
              s"${c.name}: ${c.exposedType} = $v"
            ).getOrElse(
              s"${c.name}: ${c.exposedType}"
            )
          ).mkString(", ")
          if (classEnabled) {
            val prns = (parents.take(1).map(" extends " + _) ++ parents.drop(1).map(" with " + _)).mkString("")
            (if (caseClassFinal) "final " else "") +
              s"""case class $name($args)$prns""" + "\n" +
              s"/** JSON automated mapping for $rawName */" + "\n" +
              s"implicit val ${rawName.substring(0, 1).toLowerCase}${rawName.substring(1)}Format: OFormat[$rawName] = Json.format[$rawName]"
          } else {
            s"""
type $name = $types
/** Constructor for $name providing default values if available in the database schema. */
def $name($args): $name = {
  ${compoundValue(columns.map(_.name))}
}
          """.trim
          }
        }
      }
    }
  })
  Await.ready(codegenFuture.map { codegen =>
    codegen.writeToFile(
      "slick.jdbc.MySQLProfile", "src/main/scala", "models", "Tables", "Tables.scala"
    )
  }, Duration.Inf)
}
