package testslack

/**
 * Created by mx1 on 27/04/17.
 */
class TestController {

    def messageService
	def message2Service
	def message3Service
	
    def index() {
        messageService.testService()
		render 'index1 sent '
    }
	def index2() {		
		message2Service.testService()
		render 'index2 sent'
	}
	def index3() {
		message3Service.testService()
		render 'index3 sent'
	}
}
