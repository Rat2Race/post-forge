package dev.iamrat.crawl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DART_API_KEY", matches = ".+")
class CrawlApplicationTests {

	@Test
	void contextLoads() {
	}

}
