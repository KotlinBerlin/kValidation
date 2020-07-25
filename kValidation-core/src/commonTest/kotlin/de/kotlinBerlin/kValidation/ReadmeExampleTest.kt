package de.kotlinBerlin.kValidation


import de.kotlinBerlin.kValidation.constraints.*
import kotlin.collections.Map.Entry
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadmeExampleTest {

    @Test
    fun simpleValidation() {
        data class UserProfile(
            val fullName: String,
            val age: Int?
        )

        val validateUser = Validation<UserProfile> {
            UserProfile::fullName.invoke {
                minLength(2)
                maxLength(100)
            }

            UserProfile::age ifPresent {
                minimum(0)
                maximum(150)
            }
        }

        val invalidUser = UserProfile("A", -1)
        val validationResult = validateUser(invalidUser)
        assertType<Invalid<UserProfile>>(validationResult) {
            assertEquals(2, it.flatErrors.size)
            assertEquals("must have at least 2 characters", it.flatErrors.first().message)
            assertEquals("must be at least '0'", it.flatErrors.last().message)
        }
    }

    @Test
    fun complexValidation() {
        data class Person(val name: String, val email: String?, val age: Int)

        data class Event(
            val organizer: Person,
            val attendees: List<Person>,
            val ticketPrices: Map<String, Double?>
        )

        val validateEvent = Validation<Event> {
            Event::organizer.invoke {
                // even though the email is nullable you can force it to be set in the validation
                Person::email required {
                    pattern("\\w+@bigcorp.com") hint "Organizers must have a BigCorp email address"
                }
            }

            // validation on the attendees list
            Event::attendees.invoke {
                maxItems(100)
                thisPath allInIterable {
                    Person::name.invoke {
                        minLength(2)
                    }
                    Person::age.invoke {
                        minimum(18) hint "Attendees must be 18 years or older"
                    }
                    // Email is optional but if it is set it must be valid
                    Person::email ifPresent {
                        pattern("\\w+@\\w+\\.\\w+") hint "Please provide a valid email address (optional)"
                    }
                }
            }

            // validation on the ticketPrices Map as a whole
            Event::ticketPrices.invoke {
                minItems(1) hint "Provide at least one ticket price"
            }

            // validations for the individual entries
            Event::ticketPrices allInMap {
                // Tickets may be free
                Entry<String, Double?>::value ifPresent {
                    minimum(0.01)
                }
            }
        }

        val validEvent = Event(
            organizer = Person("Organizer", "organizer@bigcorp.com", 30),
            attendees = listOf(
                Person("Visitor", null, 18),
                Person("Journalist", "hello@world.com", 35)
            ),
            ticketPrices = mapOf(
                "diversity-ticket" to null,
                "early-bird" to 200.0,
                "regular" to 400.0
            )
        )

        assertEquals(Valid(validEvent), validateEvent(validEvent))


        val invalidEvent = Event(
            organizer = Person("Organizer", "organizer@smallcorp.com", 30),
            attendees = listOf(
                Person("Youngster", null, 17)
            ),
            ticketPrices = mapOf(
                "we-pay-you" to -100.0
            )
        )

        val tempResult = validateEvent(invalidEvent)
        tempResult.flatErrors.forEach {
            println("${it.dataPath}: ${it.message}")
        }
        assertEquals(3, countFieldsWithErrors(tempResult))
        assertType<Invalid<Event>>(tempResult) {
            assertEquals(
                "Attendees must be 18 years or older",
                it.errorsAt(Event::attendees, 0, Person::age)[0].message
            )
        }
    }

}
