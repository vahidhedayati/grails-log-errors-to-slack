package slack


import slack.builder.SlackMessageBuilder
import slack.exception.SlackMessageException
import grails.plugins.rest.client.RestBuilder
import grails.util.Holders
import grails.converters.JSON
import groovy.time.TimeCategory

import org.apache.log4j.spi.LoggingEvent
import org.springframework.http.converter.StringHttpMessageConverter

import custom.SlackNotified



class SlackService {

	private final static int LOG_MAXSIZE = 1000

	def grailsApplication
	
	private static List whiteList=Holders.grailsApplication.config.slack.whiteList?:[]
	
	/**
	 * Eternally self preserving map of 10000 entries containing all logs passing through
	 */
	public static final int MAX_LOGS_QUEUE = 10000
	public Map<SlackNotified, Date> loggedLogs = createLRUMap(MAX_LOGS_QUEUE)
	
	/**
	 * when an alert is sent to slack it is stored in here for comparison
	 */
	public static final int MAX_LOGS_SENT = 1000
	public Map<SlackNotified, Date> sentLogs = createLRUMap(MAX_LOGS_SENT)
	
	
	/**
	 * LRUMap that self removes older entries keeping a max limited map
	 * @param maxEntries
	 * @return
	 */
	public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
		return new LinkedHashMap(maxEntries*3/2, 0.7f, true) { //(maxEntries*10/7, 0.7f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > maxEntries;
			}
		};
	}
	
	def logEvent(String source, LoggingEvent event,String logStatement) {
		// work out if error message is of interest
		String errorType = event.message		
		if (!whiteList.contains(errorType)) {
			// if not of interest don't bother executing rest
			return
		}

		//limit actual error message string to a given length ?
		//def limit = { string, maxLength -> string.substring(0, Math.min(string.length(), maxLength))}
		
		String className=event.getLoggerName()
		
		/**
		 * load up default global channel and person etc
		 */
		String messageFao="${grailsApplication.config.slack.messageFao?:'vv'}"
		String channelName="${grailsApplication.config.slack.channel?:'general'}"
		String botName="${grailsApplication.config.slack.botName?:'bigbadbot'}"
		def alertConfig = (getConfig('alertConfig') as Map)
		
		/**
		* lookup configuration for given className matching logging class with error
		*/
		String methodName
		def throwableEvent = event.getThrowableStrRep()
		if (throwableEvent.size()>1) {
			def methodLine = throwableEvent[1]
			if (methodLine && methodLine.contains('at')) {
				def aa = methodLine.substring(methodLine.indexOf('at ')+3,,methodLine.indexOf('('))
				def fmn=methodLine.substring(methodLine.indexOf('at ')+3,methodLine.indexOf('('))?.split("\\.")
				methodName=fmn[fmn.length-1]
			}
		}
		
		/**
		 * work out if configuration has a specific config under: 
		 * logConfig for this given className/methodName
		 * if found override global
		 */
		def found = bindLogging(className,methodName)
		if (found) {
			if (found?.botName) {
				botName=found.botName
			}
			if (found?.channel) {
				channelName=found.channel
			}
			if (found?.messageFao) {
				messageFao=found.messageFao
			}
			if (found?.alertConfig) {
				alertConfig=found?.alertConfig
			}
		}

		if (logStatement.trim().size()>0) {
			
			/**
			 * Only if we have an actual log proceed
			 */
	
			
			//Generate a SlackNotified object the key for loggedLogs and sentLogs maps
			SlackNotified sn = new SlackNotified()
			sn.className=className
			sn.methodName=methodName
			
			//add to overall logs
			loggedLogs.put(sn,new Date())
			//work out overall logs for this specific call
			int counter = loggedLogs.findAll{SlackNotified k,v -> k.className==className && k.methodName==methodName}?.size()
			//is counter => than alertConfig key ? (alerts hit)
			def delay = alertConfig?.find{counter>=it.key}
			
			def newDate
			boolean goAhead = true
			def currentDate=new Date()
			List lastSent1
			StringBuilder delayIntervals= new StringBuilder()
			def specificSent
			def lastSent
			
			if (delay) {
				//work out last sent vs wait time of delay key	
				int waitMinutes = delay.value
				specificSent = sentLogs.findAll{SlackNotified k,v -> k.className==className && k.methodName==methodName}
				lastSent1 = specificSent*.value
				if (lastSent1) {
					goAhead=false
					lastSent=lastSent1.last()
					int min=delay.value
					use (TimeCategory) {
						newDate=lastSent+min.minute
					}
					if (currentDate > newDate ) {
						//alerts above delay threshHold - can alert
						goAhead = true
					}
				}
			}			
			if (goAhead) {
				
				// Write alert footer of each key and duration:
				alertConfig?.each{ k,v ->
					delayIntervals << ("When logCount >= ${k} ensure there is a ${v} minute delay from last sent alert\n")
				}
				
				//add new entry to sentLogs
				sentLogs.put(sn,new Date())
				
				//specific strings to add to message
				String addOn=''
				if (methodName) {
					addOn=" | *Method:* ${methodName} "
				}
				String lastSentText=''
				if (lastSent) {
					lastSentText=" | *Last sent:* ${lastSent}" // .format('dd MMM yyyy HH:mm')}"
				}
				String specificSentText=''
				if (specificSent) {
					specificSentText=" | *Sent:* ${specificSent?.size()}"
				}
				
				//return event time from timeStamp
				Date issueDate=new Date(event.timeStamp)
				
				//return
				// Restrict log size ?
				// `${limit(logStatement.trim() ?: "Source not set", LOG_MAXSIZE)}`
				
				send {
					text """@${messageFao} | *Application:* ${source}  | *Class:*  ${className} $addOn | *Log Level:* ${event.level} | *Date:* ${issueDate.format('dd MMM yyyy HH:mm:ss')}
```${logStatement}```
*Logged:* ${counter} ${specificSentText} ${lastSentText}
```${delayIntervals.toString()}```"""
					username "${botName}"
					channel "#${channelName}"
					markdown true
				}
			}
		}
	}

	/**
	 * Returns relevant configuration for a given class and or class and methodName
	 * i.e. MessageService.doSomething() where MessageService is className and doSomething is the methodName
	 * @param className
	 * @return
	 */
	def bindLogging(String className, String methodName=null) {
		def results=[:]
		def configProp =getConfigClasses(className, methodName)
		if (configProp) {
			results = configProp
		}
		return results
	}

	/**
	 * Will lookup end config.groovy for slack.logConfig
	 * This contains a map iteration for a given className an example below
	 *
	 *  logConfig = [
		'grails.app.services.testslack.MessageService':[messageFao:'badvad',channel:'random',alertConfig:[2:1,10:2,100:4,1000:10]],
		'grails.app.services.testslack.MessageService':[methodName:'testService',messageFao:'goodNiceBot',channel:'general',alertConfig:[2:3,10:4,100:5,1000:6]],
		'grails.app.services.testslack.Message2Service':[messageFao:'bigBadBot',channel:'random',botName:'badvad'],
	 ]
	 * @param className
	 * @param methodName (optional)
	 * @return
	 */
	Map getConfigClasses(String className, String methodName=null) {
		def results = (getConfig('logConfig') as Map).findAll{k,v-> k.toString()==className}
		def foundResult
		if (methodName) {
			foundResult=results.find{k,v -> v.methodName && v.methodName==methodName}?.value
		}
		if (!foundResult && results) {
			foundResult=results.find()?.value
		}
		return 	foundResult
	}
	
	def getConfig(String configProperty) {
		grailsApplication.config.slack[configProperty] ?: ''
	}

	void send(Closure closure) throws SlackMessageException {

		def message = buildMessage(closure)

		def webhook = grailsApplication.config.slack.webhook

		if (!webhook) throw new SlackMessageException("Slack webhook is not valid")

		try {
			webhook.toURL()
		} catch (Exception ex) {
			throw new SlackMessageException("Slack webhook is not valid")
		}

		String jsonMessage = (message as JSON).toString()

		log.debug "Sending message : ${jsonMessage}"

		def rest = new RestBuilder()

		//rest.restTemplate.setMessageConverters([new StringHttpMessageConverter(Charset.forName("UTF-8"))])
		rest.restTemplate.setMessageConverters([new StringHttpMessageConverter()])

		def resp = rest.post(webhook.toString()) {
			header('Content-Type', 'application/json;charset=UTF-8')
			json jsonMessage
		}

		if (resp.status != 200 || resp.text != 'ok') {
			throw new SlackMessageException("Error while calling Slack -> ${resp.text}")
		}

	}

	private SlackMessage buildMessage(Closure closure) throws SlackMessageException {

		def builder = new SlackMessageBuilder()
		closure.delegate = builder
		closure.resolveStrategy = Closure.DELEGATE_FIRST
		closure.call(builder)

		def message = builder?.message

		if (!message) throw new SlackMessageException("Cannot send empty message")

		return message

	}
	

}
