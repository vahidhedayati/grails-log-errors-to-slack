import custom.EventLogAppender

class BootStrap {

    def init = { servletContext ->
		//EventLogAppender.appInitialized = true
    }
    def destroy = {
    }
}
