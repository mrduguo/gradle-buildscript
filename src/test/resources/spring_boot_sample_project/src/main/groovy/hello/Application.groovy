package hello

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable

@SpringBootApplication
@RestController
public class Application {

	@RequestMapping('/')
	def index() {
		'Hello My World'
	}

	@RequestMapping('/api/json')
	def json() {
		[
				foo:'FOO',
				bar:'BAR',
		]
	}

	@RequestMapping("/reverse/{stringToReverse}")
	def reverse(@PathVariable String stringToReverse) {
		return new StringBuilder(stringToReverse).reverse().toString()
	}



	public static void main(String[] args) {
		SpringApplication.run(Application.class, args)
	}

}
