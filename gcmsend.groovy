@Grapes([
  @Grab("org.codehaus.groovy.modules.http-builder:http-builder:0.7.1") 
])
import groovy.json.JsonSlurper

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
 
def cli = new CliBuilder(usage: "groovy gcmsend [options]",
                         header: "Options", width: 120)
cli.help("Create a file called payload.json next to this script and put the JSON payload for the notification in it. Alternatively you can define the JSON literal on the command line using the --json option.")
cli.k(optionalArg: true, longOpt: "key",   args: 1, argName: "string", "The key of the server from which the notification is sent. The key is used for authentication on the GCM connection server.")
cli.s(optionalArg: true, longOpt: "sid",   args: 1, argName: "string", "The id of the sender from which the notification is sent. Clients must register to that sender id to receive notifications.")
cli.t(required:    true, longOpt: "topic", args: 1, argName: "string", "The topic to send the notification to. For convenience, only the part following /topics/ needs to be given, e.g. foo-bar")
cli._(optionalArg: true, longOpt: "json",  args: 1, argName: "object", "The JSON payload for the notification, e.g. \"{\\\"message\\\": \\\"This is a GCM Topic Message!\\\"}\". NOTE: Remember this is a Java string. So escape characters like quotes or backslashes properly.")
cli.d(optionalArg: true, longOpt: "debug", "whether a more verbose debug output shall be displayed")
def options = cli.parse(args)
if (!options) {
  System.exit(1)
}
DEBUG = options.d

String gcmUrl = "https://gcm-http.googleapis.com/gcm/send"

String serverKey
if (options.k) {
  if (DEBUG) println "reading server key from command line"
  serverKey = options.k
} else {
  if (DEBUG) println "reading server key from secret.properties"
  def props = new Properties()
  new File("secret.properties").withInputStream { props.load(it) }
  serverKey = props.serverKey
}

String senderID
if (options.k) {
  if (DEBUG) println "reading sender id from command line"
  senderID = options.s
} else {
  if (DEBUG) println "reading sender id from secret.properties"
  def props = new Properties()
  new File("secret.properties").withInputStream { props.load(it) }
  senderID = props.senderID
}

def payload = [:]
payload['to'] = "/topics/${options.t}".toString()
if (options.json) {
  if (DEBUG) println "reading JSON payload from command line"
  payload['data'] = new JsonSlurper().parseText(options.json)
} else {
  if (DEBUG) println "reading JSON payload from payload.json"
  payload['data'] = new JsonSlurper().parse(new File("payload.json"))
}

if (DEBUG) {
  payload.each { println it }
}

def http = new HTTPBuilder(gcmUrl)
http.request(Method.POST, ContentType.JSON) {
  headers.'Authorization' = "key=${serverKey}"
  body = payload

  response.success = { resp, reader ->
    assert resp.statusLine.statusCode == 200
    println resp.statusLine
    System.out << reader
  }

  response.failure = { resp ->
    println "Unexpected failure: ${resp.statusLine}"
  }
}