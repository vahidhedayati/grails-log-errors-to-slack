###Grails logging error logs to slack


This project is running on grails 2.0.1, it has a copy of the grails 2 slack plugin with modification to BuildConfig to load a more earlier version of rest and changes to slack Send service to work with older rest methods.



Refer to src/groovy/custom/EventLogAppend.groovy which changes behaviour and forwards error logs to slack service



Slack service intelligence built in to watch what is coming in, then refer to the Config.groovy which for a given class you can fine tune how many alerts it should sent and how long it should wait before sending the next alert when it hits the threshold

```
'grails.app.services.testslack.MessageService':[messageFao:'badvad',channel:'random',alertConfig:[2:1,10:2,100:4,1000:10]],
		'grails.app.services.testslack.MessageService':[methodName:'testService',messageFao:'goodNiceBot',channel:'general',alertConfig:[2:3,10:4,100:5,1000:6]],
		'grails.app.services.testslack.Message2Service':[messageFao:'bigBadBot',channel:'random',botName:'badvad']
```

In the case of middle config when a log.error arrives from grails.app.services.testslack.MessageService and was from method : testService it will send to @goodNiceBot in room: general 
alertConfig means

if 1st log entry send through upon 2nd up until 10th logs wait 3 minutes before sending

if over 10 received and anything up to 100 log entries from this specific call a 5 minute delay before logging to slack


anything about 10000 send every 6 minutes

so a quick config of 5:10 means up to 5 will be instant to slack after 5 it waits 5 minutes a time before sending to slack



As powerful as nagios and yet simple effective  enjoy !



