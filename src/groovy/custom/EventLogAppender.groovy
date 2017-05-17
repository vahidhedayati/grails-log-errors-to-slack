package custom

import grails.util.Holders

import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.spi.LoggingEvent

//Thanks to http://www.stichlberger.com/software/grails-log-to-database-with-custom-log4j-appender/#codesyntax_4

class EventLogAppender extends AppenderSkeleton implements Appender {
	
    String source
	
    @Override
    protected void append(LoggingEvent event) {
		//inject slack service 
		def slackService = Holders.grailsApplication.mainContext.getBean('slackService')		
        //copied from Log4J's JDBCAppender
        event.getNDC()
        event.getThreadName()
        // Get a copy of this thread's MDC.
        event.getMDCCopy()
        event.getLocationInformation()
        event.getRenderedMessage()
        event.getThrowableStrRep()
        
        String logStatement = getLayout().format(event)
		
		/*
		String message = null
		if(event.locationInformationExists()){
			StringBuilder formatedMessage = new StringBuilder()
			formatedMessage.append(event.getLocationInformation().getClassName())
			formatedMessage.append(".")
			formatedMessage.append(event.getLocationInformation().getMethodName())
			formatedMessage.append(":")
			formatedMessage.append(event.getLocationInformation().getLineNumber())
			formatedMessage.append(" - ")
			formatedMessage.append(event.getMessage().toString())
			message = formatedMessage.toString()
		}else{
			message = event.getMessage().toString()
		}
		*/
		switch(event.getLevel().toInt()){
			case Level.INFO_INT:
				
				break
			case Level.DEBUG_INT:

				break
			case Level.ERROR_INT:
				slackService.logEvent(source,event,logStatement)
				break
			case Level.WARN_INT:

				break
			case Level.TRACE_INT:

				break
			default:

				break
		}
    }
 
    /**
     * Set the source value for the logger (e.g. which application the logger belongs to)
     * @param source
     */
    public void setSource(String source) {
        this.source = source
    }
 
    @Override
    void close() {
        //noop
    }
 
    @Override
    boolean requiresLayout() {
        return true
    }

}
