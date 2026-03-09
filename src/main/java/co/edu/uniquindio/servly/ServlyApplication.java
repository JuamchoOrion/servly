package co.edu.uniquindio.servly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServlyApplication.class, args);
	}

}
