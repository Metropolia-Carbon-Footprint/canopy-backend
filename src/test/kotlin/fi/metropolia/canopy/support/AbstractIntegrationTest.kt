package fi.metropolia.canopy.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbc.execute(
            """
            TRUNCATE TABLE
                trip_segments,
                trips,
                campuses,
                users
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }
}
