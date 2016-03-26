package hello

import org.springframework.beans.factory.annotation.Autowired

class BeanSpec extends AbstractSpec {

    @Autowired
    Application application


    void "should return Greetings from application"() {
        expect:
        application.index() == 'Hello My World'
    }

}