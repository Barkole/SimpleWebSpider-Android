# SimpleWebSpider-Android
Simple Web Spider for Android

Spider the web and creates traces in the web. Have the URLs only in memory, so read/writes on your space. 

Uses defaults
* Max 102.400 URLs in memory
* Start urls:
    * http://www.uroulette.com/
	* http://linkarena.com/
	* https://www.dmoz.org/
	* https://en.wikipedia.org/wiki/Special:Random
* 4 requests per minutes

Currently possible:
* Max number of URL hold in memory
* Enter your own start URL
* Max requests per minutes

Currently missing:
* App as background service
* Crash handling
* Autostart with device start
* Option to sleep while no WIFI is available
* Convert to Kotlin
* (Permanent) configuration like
	* Startup
		* Start URL(s) (hardcoded)
		* Sleep on WIFI (not available yet)
		* Autostart with device (not available yet)
	* URL database
		* Max URLs in memory (default: 102.400)
	* Parser
		* Max buffer size of HTML parser (hardcoded 10 MB)
		* Max url length (hardcoded 1024 chars)
		* Buffer size of html stream (hardcoded 4 KB)
	* Request Throttler
		* Max requests per minute (default 4)
		* Min new thread spawn wait (hardcoded 100ms)
		* Max number of threads (hardcoded 128, but max number of cores, and min 1)
	* Host Throttler
		* Number of randomly loaded hosts to compare (hardcoded 100)
		* Number of hosts to remember (hardcoded 10.240)
		* Time to live of host (hardcoded 1h)