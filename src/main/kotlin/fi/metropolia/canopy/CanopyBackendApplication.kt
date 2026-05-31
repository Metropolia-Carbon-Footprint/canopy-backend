package fi.metropolia.canopy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CanopyBackendApplication

fun main(args: Array<String>) {
    runApplication<CanopyBackendApplication>(*args)
}
