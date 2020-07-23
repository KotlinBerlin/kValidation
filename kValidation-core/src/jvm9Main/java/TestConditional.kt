@file:Suppress("unused", "SpellCheckingInspection")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.constraints.*
import kotlin.test.Test
import kotlin.test.assertTrue

data class Person(val name: String?, val familyName: String?)

val validator1 = Validation<Person> {
    Person::name.validate {
        isNotNull()
    }
    Person::name.validate {
        isNotNull()
    }
}

val validator2 = Validation<Person> {
    or {
        Person::name {
            isNotNull()
        }
        Person::name {
            isNotNull()
        }
    }
}

val bothNamesOrNoAtAll = Validation<Person> {
    or {
        and {
            Person::name required {
                minLength(1) hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
            Person::familyName required {
                minLength(1) hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
        }
        and {
            Person::name.validate {
                isNull() hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
            Person::familyName.validate {
                isNull() hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
        }
    }
}

val bothNamesOrNoAtAll2 = Validation<Person> {
    or {
        and {
            Person::name required {
                minLength(1) hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
            Person::familyName required {
                minLength(1) hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
        }
        and {
            Person::name.validate {
                isNull() hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
            Person::familyName.validate {
                isNull() hint "Bitte füllen sie entweder name und Familienname oder keins von beidem"
            }
        }
    }
}


/**
 * Validator Node (or)
 *      - Validator Node (and)
 *          - Path Node (required)
 *              -
 *      - Validator Node (and)
 *
 */

val conditionValidation = Validation<Person> {

}

val bothNamesOrNoneOrOnlyFamilyName = Validation<Person> {
    Person::familyName validateIf {
        Person::name required {
            minLength(1)
        }
    } required {
        minLength(1)
    }
}

class TestConditional {

    @Test
    fun test1() {
        val validate = bothNamesOrNoAtAll(Person(null, null))
        println(validate)
        assertTrue("Should be valid") { validate is Valid }
        val validate1 = bothNamesOrNoAtAll(Person("1", "1"))
        println(validate1)
        assertTrue("Should be valid") { validate1 is Valid }
        val validate2 = bothNamesOrNoAtAll(Person("1", null))
        println(validate2)
        assertTrue("Should be invalid") { validate2 is Invalid }
        val validate3 = bothNamesOrNoAtAll(Person(null, "1"))
        println(validate3)
        assertTrue("Should be invalid") { validate3 is Invalid }
    }

    @Test
    fun test2() {
        val validate = bothNamesOrNoneOrOnlyFamilyName.invoke(Person(null, null))
        println(validate)
        assertTrue("Should be valid") { validate is Valid }
        val validate1 = bothNamesOrNoneOrOnlyFamilyName(Person("1", "1"))
        println(validate1)
        assertTrue("Should be valid") { validate1 is Valid }
        val validate2 = bothNamesOrNoneOrOnlyFamilyName(Person("1", null))
        println(validate2)
        assertTrue("Should be invalid") { validate2 is Invalid }
        val validate3 = bothNamesOrNoneOrOnlyFamilyName(Person(null, "1"))
        println(validate3)
        assertTrue("Should be valid") { validate3 is Valid }
    }
}

class TestShortCircuit {

    @Test
    fun test1() {
        val tempValidation = Validation<Person> {
            Person::name required {
                or {
                    nonShortCircuit()
                    maxLength(200)
                    maxLength(100)
                    type<Number>()
                }
            }
        }

        val validate = tempValidation(Person("1000", null))

        println(validate)
    }
}