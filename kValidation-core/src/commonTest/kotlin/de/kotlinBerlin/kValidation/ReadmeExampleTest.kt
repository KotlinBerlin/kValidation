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
            UserProfile::fullName.validate {
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

        assertEquals(2, validationResult.errors.size)
        assertEquals("'A' must have at least 2 characters", validationResult.errors.first().message)
        assertEquals("'-1' must be at least '0'", validationResult.errors.last().message)
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
            Event::organizer.validate {
                // even though the email is nullable you can force it to be set in the validation
                Person::email required {
                    pattern("\\w+@bigcorp.com") hint "Organizers must have a BigCorp email address"
                }
            }

            // validation on the attendees list
            Event::attendees.validate {
                maxItems(100)
            }

            // validation on individual attendees
            Event::attendees onEachIterable {
                Person::name.validate {
                    minLength(2)
                }
                Person::age.validate {
                    minimum(18) hint "Attendees must be 18 years or older"
                }
                // Email is optional but if it is set it must be valid
                Person::email ifPresent {
                    pattern("\\w+@\\w+\\.\\w+") hint "Please provide a valid email address (optional)"
                }
            }

            // validation on the ticketPrices Map as a whole
            Event::ticketPrices.validate {
                minItems(1) hint "Provide at least one ticket price"
            }

            // validations for the individual entries
            Event::ticketPrices onEachMap {
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
        assertEquals(3, countFieldsWithErrors(tempResult))
        assertEquals("Attendees must be 18 years or older", tempResult[Event::attendees, 0, Person::age]!![0])
    }

}
