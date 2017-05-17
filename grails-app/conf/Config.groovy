import org.apache.log4j.EnhancedPatternLayout

import custom.EventLogAppender

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }


grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']


// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// enable query caching by default
grails.hibernate.cache.queries = true

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    
    //Change 1
    //Override log4j to use new LogAppender
	appenders {
		//EnhancedPatternLayout is needed in order to support the %throwable logging of stacktraces
		appender new EventLogAppender(source:'testslack', name: 'eventLogAppender', layout:new EnhancedPatternLayout(conversionPattern: '%d{DATE} %5p %c{1}:%L - %m%n %throwable{500}'), threshold: org.apache.log4j.Level.ERROR)
		console name:'stdout'
	}
	//Change 2
	root {
		error 'eventLogAppender'
	//	info 'stdout'
	}
    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}

//Change 3
slack {
	webhook = 'https://hooks.slack.com/services/XXXXXXX/YYYYYYYYYYYY'
	
	/**
	 * Generic rules for all classes if no configuration per class method found:
	 */
	botName='bigbadbot'		//user sending actual messages	
	messageFao='vv'	//the user to @to in the room or get attention of
	channel='general' //channel to send to
	/**
	 * Send email alerts when
	 * if alertOnlyOnLimit is false then <= key if true then = key so:
	 *  = 2 alerts  5 minutes before last
	 *  = 10 alerts ensure there was a 10 minute gap before last
	 *
	 * As shown above each config can override these default values
	 */
	alertConfig=[
		2:5,
		10:10,
		100:60,
		1000:1200
	]
	/**
	 * Override above values for a given className,methodName:
	 */
	logConfig = [
		'grails.app.services.testslack.MessageService':[messageFao:'badvad',channel:'random',alertConfig:[2:1,10:2,100:4,1000:10]],
		'grails.app.services.testslack.MessageService':[methodName:'testService',messageFao:'goodNiceBot',channel:'general',alertConfig:[2:3,10:4,100:5,1000:6]],
		'grails.app.services.testslack.Message2Service':[messageFao:'bigBadBot',channel:'random',botName:'badvad']
	]
	
	whiteList=[
		'java.lang.NullPointerException'
	]
}
