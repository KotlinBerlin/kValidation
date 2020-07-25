package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.constraints.inRange
import de.kotlinBerlin.kValidation.constraints.isNotNull
import de.kotlinBerlin.kValidation.constraints.minLength
import kotlin.collections.Map.Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ComplexValidationsTest {

    @Test
    fun simpleOr() {
        data class Person(val name: String?, val familyName: String?)

        val orValidation = Validation.or<Person> {
            Person::name required {}
            Person::familyName required {}
        }

        val tempValidPerson = Person("", null)
        val tempInvalidPerson = Person(null, null)

        assertType<Valid<Person>>(orValidation(tempValidPerson)) {
            assertSame(tempValidPerson, it.value)
        }
        assertType<Invalid<Person>>(orValidation(tempInvalidPerson)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(Person::name).first())
            checkMessage("is required", it.errorsAt(Person::familyName).first())
        }
    }

    @Test
    fun nestedOr() {
        data class Person(val name: String?, val familyName: String?, val age: Int)

        val nestedOrValidation = Validation<Person> {
            Person::age required {
                inRange(1..100)
            }
            or {
                Person::name required {}
                Person::familyName required {}
            }
        }

        val tempValidPerson = Person("", null, 10)
        val tempInvalidPerson1 = Person(null, null, 200)
        val tempInvalidPerson2 = Person("", null, 200)
        val tempInvalidPerson3 = Person(null, null, 10)

        assertType<Valid<Person>>(nestedOrValidation(tempValidPerson)) {
            assertSame(tempValidPerson, it.value)
        }
        assertType<Invalid<Person>>(nestedOrValidation(tempInvalidPerson1)) {
            assertEquals(3, it.flatErrors.size)
            checkMessage("must be at least '1' and not greater than '100'", it.errorsAt(Person::age).first())
            checkMessage("is required", it.errorsAt(Person::name).first())
            checkMessage("is required", it.errorsAt(Person::familyName).first())
        }
        assertType<Invalid<Person>>(nestedOrValidation(tempInvalidPerson2)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("must be at least '1' and not greater than '100'", it.errorsAt(Person::age).first())
        }
        assertType<Invalid<Person>>(nestedOrValidation(tempInvalidPerson3)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(Person::name).first())
            checkMessage("is required", it.errorsAt(Person::familyName).first())
        }
    }

    @Test
    fun shortCircuitAnd() {
        data class Person(val name: String?, val familyName: String?, val age: Int)

        val shortCircuitAndValidation = Validation<Person> {
            shortCircuit()
            Person::age required {
                inRange(1..100)
            }
            or {
                Person::name required {}
                Person::familyName required {}
            }
        }

        val tempInvalidPerson1 = Person(null, null, 200)
        val tempInvalidPerson2 = Person(null, null, 10)

        assertType<Invalid<Person>>(shortCircuitAndValidation(tempInvalidPerson1)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("must be at least '1' and not greater than '100'", it.errorsAt(Person::age).first())
        }
        assertType<Invalid<Person>>(shortCircuitAndValidation(tempInvalidPerson2)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(Person::name).first())
            checkMessage("is required", it.errorsAt(Person::familyName).first())
        }
    }

    @Test
    fun conditionalValidation() {
        data class Person(val name: String?, val familyName: String?)

        val tempConditionalValidation = Validation<Person> {
            Person::familyName validateIf {
                Person::name {
                    isNotNull()
                }
            } required {
                minLength(1)
            }
        }

        val tempValidPerson1 = Person("FirstName", "FamilyName")
        val tempValidPerson2 = Person(null, "FamilyName")
        val tempValidPerson3 = Person(null, null)
        val tempInvalidPerson1 = Person("FirstName", null)
        val tempInvalidPerson2 = Person("FirstName", "")

        assertType<Valid<Person>>(tempConditionalValidation(tempValidPerson1)) {
            assertSame(tempValidPerson1, it.value)
        }
        assertType<Valid<Person>>(tempConditionalValidation(tempValidPerson2)) {
            assertSame(tempValidPerson2, it.value)
        }
        assertType<Valid<Person>>(tempConditionalValidation(tempValidPerson3)) {
            assertSame(tempValidPerson3, it.value)
        }
        assertType<Invalid<Person>>(tempConditionalValidation(tempInvalidPerson1)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(Person::familyName).first())
        }
        assertType<Invalid<Person>>(tempConditionalValidation(tempInvalidPerson2)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("must have at least 1 characters", it.errorsAt(Person::familyName).first())
        }
    }

    @Test
    fun filteredIterableValidation() {
        data class Person(val name: String?, val familyName: String?)

        val tempPersonListValidation = Validation<Collection<Person>> {
            thisPath.allIndicesInIterable(0, 2) {
                Person::name required {
                    minLength(1)
                }
            }
        }

        val tempValidPerson1 = Person("Name1", null)
        val tempValidPerson2 = Person("Name2", "FamilyName2")
        val tempInvalidPerson1 = Person(null, null)
        val tempInvalidPerson2 = Person(null, "FamilyName2")

        val tempFullValidList = listOf(tempValidPerson1, tempValidPerson2)
        val tempMixedValidList = listOf(tempValidPerson1, tempInvalidPerson1, tempValidPerson2, tempInvalidPerson2)
        val tempFullInvalidList = listOf(tempInvalidPerson1, tempInvalidPerson2)
        val tempMixedInvalidList = listOf(tempInvalidPerson1, tempValidPerson1, tempInvalidPerson2, tempValidPerson2)

        emptyList<Person>().let {
            assertType<Valid<List<Person>>>(tempPersonListValidation(it)) { tempResult ->
                assertSame(it, tempResult.value)
            }
        }
        assertType<Valid<List<Person>>>(tempPersonListValidation(tempFullValidList)) {
            assertSame(tempFullValidList, it.value)
        }
        assertType<Valid<List<Person>>>(tempPersonListValidation(tempMixedValidList)) {
            assertSame(tempMixedValidList, it.value)
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempFullInvalidList)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Person::name).first())
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempMixedInvalidList)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Person::name).first())
            checkMessage("is required", it.errorsAt(2, Person::name).first())
        }
    }

    @Test
    fun filteredArrayValidation() {
        data class Person(val name: String?, val familyName: String?)

        val tempPersonListValidation = Validation<Array<Person>> {
            thisPath.allIndicesInArray(0, 2) {
                Person::name required {
                    minLength(1)
                }
            }
        }

        val tempValidPerson1 = Person("Name1", null)
        val tempValidPerson2 = Person("Name2", "FamilyName2")
        val tempInvalidPerson1 = Person(null, null)
        val tempInvalidPerson2 = Person(null, "FamilyName2")

        val tempFullValidList = arrayOf(tempValidPerson1, tempValidPerson2)
        val tempMixedValidList = arrayOf(tempValidPerson1, tempInvalidPerson1, tempValidPerson2, tempInvalidPerson2)
        val tempFullInvalidList = arrayOf(tempInvalidPerson1, tempInvalidPerson2)
        val tempMixedInvalidList = arrayOf(tempInvalidPerson1, tempValidPerson1, tempInvalidPerson2, tempValidPerson2)

        emptyArray<Person>().let {
            assertType<Valid<Array<Person>>>(tempPersonListValidation(it)) { tempResult ->
                assertEquals(it, tempResult.value)
            }
        }
        assertType<Valid<Array<Person>>>(tempPersonListValidation(tempFullValidList)) {
            assertSame(tempFullValidList, it.value)
        }
        assertType<Valid<Array<Person>>>(tempPersonListValidation(tempMixedValidList)) {
            assertSame(tempMixedValidList, it.value)
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempFullInvalidList)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Person::name).first())
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempMixedInvalidList)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Person::name).first())
            checkMessage("is required", it.errorsAt(2, Person::name).first())
        }
    }

    @Test
    fun filteredMapValidation() {
        data class Person(val name: String?, val familyName: String?)

        val tempPersonListValidation = Validation<Map<Int, Person>> {
            thisPath.allKeysInMap(0, 2) {
                Entry<Int, Person>::value {
                    Person::name required {
                        minLength(1)
                    }
                }
            }
        }

        val tempValidPerson1 = Person("Name1", null)
        val tempValidPerson2 = Person("Name2", "FamilyName2")
        val tempInvalidPerson1 = Person(null, null)
        val tempInvalidPerson2 = Person(null, "FamilyName2")

        val tempFullValidList = mapOf(0 to tempValidPerson1, 1 to tempValidPerson2)
        val tempMixedValidList =
            mapOf(0 to tempValidPerson1, 1 to tempInvalidPerson1, 2 to tempValidPerson2, 3 to tempInvalidPerson2)
        val tempFullInvalidList = mapOf(0 to tempInvalidPerson1, 1 to tempInvalidPerson2)
        val tempMixedInvalidList =
            mapOf(0 to tempInvalidPerson1, 1 to tempValidPerson1, 2 to tempInvalidPerson2, 3 to tempValidPerson2)

        emptyMap<Int, Person>().let {
            assertType<Valid<Map<Int, Person>>>(tempPersonListValidation(it)) { tempResult ->
                assertSame(it, tempResult.value)
            }
        }
        assertType<Valid<Map<Int, Person>>>(tempPersonListValidation(tempFullValidList)) {
            assertSame(tempFullValidList, it.value)
        }
        assertType<Valid<Map<Int, Person>>>(tempPersonListValidation(tempMixedValidList)) {
            assertSame(tempMixedValidList, it.value)
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempFullInvalidList)) {
            assertEquals(1, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Entry<Int, Person>::value, Person::name).first())
        }
        assertType<Invalid<List<Person>>>(tempPersonListValidation(tempMixedInvalidList)) {
            assertEquals(2, it.flatErrors.size)
            checkMessage("is required", it.errorsAt(0, Entry<Int, Person>::value, Person::name).first())
            checkMessage("is required", it.errorsAt(2, Entry<Int, Person>::value, Person::name).first())
        }
    }
}