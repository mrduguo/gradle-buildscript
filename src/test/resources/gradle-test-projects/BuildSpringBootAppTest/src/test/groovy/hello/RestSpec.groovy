package hello

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class RestSpec extends AbstractSpec {

    void "should return Greetings from Spring Boot!"() {
        when:
        def entity = getForEntity('/', String.class)

        then:
        entity.statusCode == HttpStatus.OK
        entity.body == 'Hello My World'
    }

    void "should reverse request!"() {
        when:
        def entity = getForEntity(path, String.class)

        then:
        entity.statusCode == HttpStatus.OK
        entity.body == reversedString

        where:
        path           || reversedString
        '/reverse/uno' || 'onu'
        '/reverse/ufc' || 'cfu'
    }

    void "should get json!"() {
        when:
        def entity = getForEntity('/api/json', Map.class)

        then:
        entity.statusCode == HttpStatus.OK
        entity.body.foo == 'FOO'
        entity.body.bar == 'BAR'
    }

}