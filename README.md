# Portable validations for Kotlin

  - **✅ Type-safe DSL**
  - **🔗 Multi-platform support** (JVM, JS, Native)
  - **🐥 Zero dependencies**

## How to get it

The library is currently available under the following repository:

URL: 

    https://dl.bintray.com/kotlinberlin/snapshot

MAVEN: 

    <repository>
        <id>bintray.kotlinBerlin.releases</id>
        <url>https://dl.bintray.com/kotlinberlin/snapshot</url>
    </repository>
    
GRADLE: 

    maven {
        url = uri("https://dl.bintray.com/kotlinberlin/snapshot")
    }

Artifacts for the following platforms are available:

Platform | Artifact
------------ | -------------
multi platform | de.kotlin-berlin:kValidation:1.0-RC1
jvm Version 8 | de.kotlin-berlin:kValidation-jvm8:1.0-RC1
jvm Version 9 | de.kotlin-berlin:kValidation-jvm9:1.0-RC1
js | de.kotlin-berlin:kValidation-js:1.0-RC1
mingwX64 | de.kotlin-berlin:kValidation-mingwX64:1.0-RC1
mingwX86 | de.kotlin-berlin:kValidation-mingwX86:1.0-RC1
androidNativeArm32 | de.kotlin-berlin:kValidation-androidNativeArm32:1.0-RC1
androidNativeArm64 | de.kotlin-berlin:kValidation-androidNativeArm64:1.0-RC1
wasm32 | de.kotlin-berlin:kValidation-wasm32:1.0-RC1
linuxArm64 | de.kotlin-berlin:kValidation-linuxArm64:1.0-RC1
linuxArm32Hfp | de.kotlin-berlin:kValidation-linuxArm32Hfp:1.0-RC1
linuxX64 | de.kotlin-berlin:kValidation-linuxX64:1.0-RC1

Supported but not available on the repository:

* watchosArm32
* watchosArm64
* watchosX86
* iosArm32
* iosArm64
* iosX64
* tvosArm64
* tvosX64
* macosX64
* linuxMips32
* linuxMipsel32

That is due to the fact that I am currently unable to build those targets from my machine. Feel free to clone
the repository yourself and build it for the platforms you need it. To build it just run gradle build in the cloned directory, and the project will be build for all possible platforms.


## How to use it

Suppose you have a data class like this:


    data class UserProfile(
        val fullName: String,
        val age: Int?
    )

Using the Konform type-safe DSL you can quickly write up a validation


    val validateUser = Validation<UserProfile> {
        UserProfile::fullName {
            minLength(2)
            maxLength(100)
        }

        UserProfile::age ifPresent {
            minimum(0)
            maximum(150)
        }
    }

and apply it to your data

    val invalidUser = UserProfile("A", -1)
    val validationResult = validateUser(invalidUser)

since the validation fails the `validationResult` will be of type `Invalid` and you can get a list of validation errors by indexed access:

    validationResult[UserProfile::fullName]
    // yields listOf("must have at least 2 characters")

    validationResult[UserProfile::age]
    // yields listOf("must be at least '0'")

or you can get all validation errors with details as a list:

    validationResult.errors
    // yields listOf(
    //     ValidationError(dataPath=.fullName, message=must have at least 2 characters),
    //     ValidationError(dataPath=.age, message=must be at least '0'
    // )

In case the validation went through successfully you get a result of type `Valid` with the validated value in the `value` field.

    val validUser = UserProfile("Alice", 25)
    val validationResult = validateUser(validUser)
    // yields Valid(UserProfile("Alice", 25))

### Advanced use

You can define validations for nested classes and use them for new validations

    val ageCheck = Validation<UserProfile> {
        UserProfile::age required {
            minimum(18)
        }
    }

    val validateUser = Validation<UserProfile> {
        UserProfile::fullName {
            minLength(2)
            maxLength(100)
        }
    
        run(ageCheck)
    }

It is also possible to validate nested data classes and properties that are collections (List, Map, etc...)

    data class Person(val name: String, val email: String?, val age: Int)

    data class Event(
        val organizer: Person,
        val attendees: List<Person>,
        val ticketPrices: Map<String, Double?>
    )

    val validateEvent = Validation<Event> {
        Event::organizer {
            // even though the email is nullable you can force it to be set in the validation
            Person::email required {
                pattern(".+@bigcorp.com") hint "Organizers must have a BigCorp email address"
            }
        }

        // validation on the attendees list
        Event::attendees {
            maxItems(100)
        }

        // validation on individual attendees
        Event::attendees onEach {
            Person::name {
                minLength(2)
            }
            Person::age {
                minimum(18) hint "Attendees must be 18 years or older"
            }
            // Email is optional but if it is set it must be valid
            Person::email ifPresent {
                pattern(".+@.+\..+") hint "Please provide a valid email address (optional)"
            }
        }

        // validation on the ticketPrices Map as a whole
        Event::ticketPrices {
            minItems(1) hint "Provide at least one ticket price"
        }

        // validations for the individual entries
        Event::ticketPrices onEach {
            // Tickets may be free in which case they are null
            Entry<String, Double?>::value ifPresent {
                minimum(0.01)
            }
        }
    }

Errors in the `ValidationResult` can also be accessed using the index access method. In case of `Iterables` and `Arrays` you use the numerical index and in case of `Maps` you use the key as string.

    // get the error messages for the first attendees age if any
    result[Event::attendees, 0, Person::age]

    // get the error messages for the free ticket if any
    result[Event::ticketPrices, "free"]