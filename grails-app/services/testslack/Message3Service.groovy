package testslack

import org.springframework.transaction.annotation.Transactional

import testslackplugin.User

/**
 * Created by mx1 on 27/04/17.
 */
class Message3Service {

    def slackService
	@Transactional
    def testService() {
        try {
            User u  = User.get(2L)
            println "-- ${u?.name}"
			
			User uu = new User()
			if (!uu.save(flush:true)) {
				//throw new Exception('eee')
			}
        } catch (Throwable t) {
		

			log.error(t,t)
        }
    }

}
