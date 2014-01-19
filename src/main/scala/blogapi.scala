package nineclue.md2blogger

import uk.co.bigbeeconsultants.http._
import java.net.URL
import java.nio.file.{Path, Paths, Files, LinkOption}
import scala.pickling._
import json._
import play.api.libs.json.Json

object MiniParser {
	val stringJson = new scala.util.matching.Regex("""\s*"(.*?)"\s*:\s*"(.*?)"\s*,?\s*""", "key", "value")

	def parse(resp:response.Response):Map[String, String] = {
		val body = resp.body.toString.split('\n')
		val infoPart = body.slice(1, body.size-1)
		Map.empty[String, String] ++ (for(stringJson(key, value) <- infoPart) yield (key, value))
	}
}

object BlogAPI {
  import collection.JavaConversions.asScalaBuffer

	private	val fileName = ".blogger_info"
	private val url = "https://accounts.google.com/o/oauth2/auth?response_type=code&scope=https://www.googleapis.com/auth/blogger&redirect_uri=urn:ietf:wg:oauth:2.0:oob&client_id="
	private val tokenUrl = new URL("https://accounts.google.com/o/oauth2/token")
	private var bloggerData = Map.empty[String, String]

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
		val req = request.RequestBody(Map("code"->ac,
							"client_id"->id, "client_secret"->secret,
							"redirect_uri"->"urn:ietf:wg:oauth:2.0:oob", "grant_type"->"authorization_code"))
		val client = new HttpClient
		val resp = client.post(tokenUrl, Some(req))
		bloggerData = MiniParser.parse(resp).filterKeys(k => (k == "access_token" || k == "refresh_token")) + 
			(("client_id", id), ("secret", secret))
		println(s"Successfully got tokens.")

		// get user's blog
		saveData
	}

	def saveData = {
		val file = Files.newBufferedWriter(Paths.get(fileName), java.nio.charset.Charset.forName("UTF-8"))
    try 
      file.write(bloggerData.pickle.value)
    finally
      file.close		
	}

	def listBlogs:Unit = {
		val client = new HttpClient
		val head = header.Headers(Map("Authorization" -> ("Bearer " + bloggerData.get("access_token").get)))
		val byUrl = new URL("https://www.googleapis.com/blogger/v3/users/self/blogs")
		val response = client.get(byUrl, head)
		if (response.status.code == 401) {
			updateAccessToken
			listBlogs
		}
		val json = Json.parse(response.body.toString)
	}

	def load = {
		val path = Paths.get(fileName).toAbsolutePath
		if (Files.exists(path)) {
      val jsonVal = new String(Files.readAllBytes(path))
      bloggerData = jsonVal.unpickle[Map[String, String]]
		} else {
			initialize
		}
		println(bloggerData)
		listBlogs
	}

	def updateAccessToken = {
		println("Refreshing access token.")
		val client = new HttpClient
		val head = header.Headers(Map("Authorization" -> ("Bearer " + bloggerData.get("refresh_token").get)))
		val req = request.RequestBody(Map("client_id" -> bloggerData.get("client_id").get,
			"client_secret" -> bloggerData.get("client_secret").get, 
			"refresh_token" -> bloggerData.get("refresh_token").get, "grant_type" -> "refresh_token"))
		val resp = client.post(tokenUrl, Some(req))
		bloggerData = bloggerData.updated("access_token", MiniParser.parse(resp).get("access_token").get)
		saveData
	}

	def tokens = bloggerData
}