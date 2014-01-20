package nineclue.md2blogger

import uk.co.bigbeeconsultants.http._
import java.net.URL
import java.nio.file.{Path, Paths, Files, LinkOption}
import scala.pickling._
import json._
import play.api.libs.json.{Json, JsObject, JsNumber, JsArray, JsString, JsValue}

object MiniParser {
	val stringJson = new scala.util.matching.Regex("""\s*"(.*?)"\s*:\s*"(.*?)"\s*,?\s*""", "key", "value")

	def parse(resp:response.Response):Map[String, String] = {
		val body = resp.body.toString.split('\n')
		val infoPart = body.slice(1, body.size-1)
		Map.empty[String, String] ++ (for(stringJson(key, value) <- infoPart) yield (key, value))
	}
}

case class Post(title:String, content:String, id:Option[String]=None, url:Option[String]=None, selfLink:Option[String]=None)

object BlogAPI {
  import collection.JavaConversions.asScalaBuffer

	private	val fileName = ".blogger_info"
	private val url = "https://accounts.google.com/o/oauth2/auth?response_type=code&scope=https://www.googleapis.com/auth/blogger&redirect_uri=urn:ietf:wg:oauth:2.0:oob&client_id="
	private val tokenUrl = new URL("https://accounts.google.com/o/oauth2/token")
	private var bloggerData = Map.empty[String, String]

	private val blogID = "blog_id"
	private val blogName = "blog_name"
	private val clientID = "client_id"
	private val clientSecret = "client_secret"
	private val accessToken = "access_token"
	private val refreshToken = "refresh_token"

	def initialize = {
		// get client_id & secret
		println("Initial Setup :\n")
		println("Please enter (paste) your app's client_id : ")
		val id = System.console.readLine
		println("Enter your app's client_secret : ")
		val secret = System.console.readLine

		// get authorization code
		println("\nCopy following line into browser and enter (paste) authorization code.")
		println(url + id)
		println("\nAuthorization code : ")
		val ac = System.console.readLine

		// get access token
		val req = request.RequestBody(Map("code" -> ac,
							clientID -> id, clientSecret -> secret,
							"redirect_uri"->"urn:ietf:wg:oauth:2.0:oob", "grant_type"->"authorization_code"))
		val client = new HttpClient
		val resp = client.post(tokenUrl, Some(req))
		bloggerData = MiniParser.parse(resp).filterKeys(k => (k == accessToken || k == refreshToken)) +
			((clientID, id), (clientSecret, secret))
		println(s"Successfully got tokens.")

		// get user's blog
		val blogs = blogsList
		val bid =
			if (blogs.size == 1) {
				println(s"Mananging blog ${blogs(0)._2}")
				0
			} else {
				println("Found more than one blogs, ")
				(1 to blogs.size).zip(blogs).foreach {
					case (i, b) =>
						println(s"  $i : ${b._2} - ${b._3} posts")
				}
				print("Enter the number of blog you want to manage : ")
				System.console.readLine.toInt - 1
			}
		bloggerData = bloggerData + ((blogID, blogs(bid)._1), (blogName, blogs(bid)._2))
		saveData
	}

	def saveData = {
		val file = Files.newBufferedWriter(Paths.get(fileName), java.nio.charset.Charset.forName("UTF-8"))
    try
      file.write(bloggerData.pickle.value)
    finally
      file.close
	}

	def blogsList:Array[(String, String, Int)] = {
		val client = new HttpClient
		val head = header.Headers(Map("Authorization" -> ("Bearer " + bloggerData.get(accessToken).get)))
		val byUrl = new URL("https://www.googleapis.com/blogger/v3/users/self/blogs")
		val response = client.get(byUrl, head)
		if (response.status.code == 401) {
			updateAccessToken
			blogsList
		} else {
			val json = Json.parse(response.body.toString)
			json.\("items").as[Array[JsValue]].map(b => (b.\("id").as[String], b.\("name").as[String], b.\("posts").\("totalItems").as[Int]))
		}
	}

	def load = {
		val path = Paths.get(fileName).toAbsolutePath
		if (Files.exists(path)) {
      val jsonVal = new String(Files.readAllBytes(path))
      bloggerData = jsonVal.unpickle[Map[String, String]]
		} else {
			initialize
		}
	}

	def updateAccessToken = {
		println("Refreshing access token.")
		val client = new HttpClient
		val head = header.Headers(Map("Authorization" -> ("Bearer " + bloggerData.get(refreshToken).get)))
		val req = request.RequestBody(Map(clientID -> bloggerData.get(clientID).get,
			clientSecret -> bloggerData.get(clientSecret).get,
			refreshToken -> bloggerData.get(refreshToken).get, "grant_type" -> "refresh_token"))
		val resp = client.post(tokenUrl, Some(req))
		bloggerData = bloggerData.updated(accessToken, MiniParser.parse(resp).get(accessToken).get)
		saveData
	}

	def addPost(post:Post):Post = post

	def tokens = bloggerData
}