import de.kotlinBerlin.kValidation.Invalid
import de.kotlinBerlin.kValidation.Validation
import de.kotlinBerlin.kValidation.constraints.minLength
import kotlin.test.Test

class TestExtendedJvm {

    data class Person(val name: String?, val familyName: String?, val parent: Person?)

    @Test
    fun test1() {
        val singleValidation = Validation<Person> {
            Person::name required {
                minLength(1).asWarning()
            }
            Person::name required {
                minLength(2)
            }
        }

        val validation = Validation.and<Person> {
            Person::parent ifPresent {
                run(singleValidation)
            }
            run(singleValidation)
        }
        val validate = validation.invoke(Person("", null, Person("", null, null)))
        if (validate is Invalid<*>) {
            println(validate.print())
        }
    }
}