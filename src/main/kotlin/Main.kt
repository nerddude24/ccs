import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

// TODO Maybe make it clear the terminal on each menu if that's possible

const val ver = "1.0.0"
val gson = Gson()
var verbose = false
var contacts: HashSet<Contact> = hashSetOf()

fun main(args: Array<String>) {
    /* ARG STUFF */
    if ("-q" !in args && "--quiet" !in args) {
        println("CLI Contacts Simulator ${ver}v by nerddude (GNU GPL v3).")
    }

    if ("-h" in args || "--help" in args) {
        println("=== OPTIONS ===")
        println("-q --quiet   --> Hides the app information at the start.")
        println("-v --verbose --> Shows more detail about what's going on in the background.")
        println("-h --help    --> Shows this screen.")
        return
    }

    if ("-v" in args || "--verbose" in args) {
        verbose = true
    }

    /* Loading Contacts */
    if (!Files.exists(Paths.get("contacts.json"))) {
        if (verbose)
            println("No contacts.json exists, creating one...")

        // if contacts.json doesn't exist, create it and write to it an empty list.
        Files.createFile(Paths.get("contacts.json"))
        Files.write(Path.of("contacts.json"), "[]".toByteArray())
    }

    // contacts.json file as string
    val contactsJsonString = File("contacts.json").readText()

    // this is needed so that gson reads the contacts.json as a collection
    // (or HashSet to be exact) of Contact.
    val contactsListType = object : TypeToken<HashSet<Contact>>() {}.type

    // load contacts.json into contacts variable
    contacts = try {
        gson.fromJson(contactsJsonString, contactsListType)
    } catch (e: Exception) {
        println("ERR: loading contacts failed!")

        if (verbose)
            println("More info: $e")

        // if an error occurs, set the value of contacts to an empty hashset.
        hashSetOf()
    }


    // Start the program
    mainMenu()
}

fun mainMenu() {
    println("Select an option: ")
    val input = getInputWithChoices(arrayOf("Manage Contacts", "Messages", "Quit"))

    when (input) {
        "1" -> manageContacts()
        "2" -> manageMessages()
        // this exists the application with exit code 1 (it means success or something)
        "3" -> exitProcess(1)
        else -> {
            println("Invalid input.")
            mainMenu()
        }
    }
}

fun manageContacts() {
    val input = getInputWithChoices(
        arrayOf(
            "Show all contacts",
            "Add a new contact",
            "Search contacts",
            "Delete a contact",
            "Go to main menu"
        )
    )

    when (input) {
        "1" -> showContacts()
        "2" -> addContact()
        "3" -> searchContact()
        "4" -> deleteContact()
        "5" -> mainMenu()
        else -> println("Invalid input.")
    }

    manageContacts()
}

fun manageMessages() {
    val input = getInputWithChoices(arrayOf("Show all messages", "Send new message", "Go to main menu"))

    when (input) {
        "1" -> showMessages()
        "2" -> sendMessage()
        "3" -> mainMenu()
        else -> println("Invalid input.")
    }

    manageMessages()
}

fun showContacts() {
    if (contacts.isEmpty())
        println("No contacts exist!")
    else {
        println("All contacts:")
        for ((i, contact) in contacts.withIndex()) {
            // i + 1 to make it start at 1
            println("${i + 1}. ${contact.getInfo()}")
        }
    }
}

fun addContact() {
    val input = getInput("Enter information (name, email, phone number)")

    try {
        // split input into a list, and create a new contact
        // where the first element [0] is the name, [1] is the email,
        // and [3] is the phone number.
        val info = input.split(",")
        val newContact = Contact(info[0].trim(), info[1].trim(), info[2].trim())

        if (newContact.name == "") {
            println("Name can't be empty!")
            return
        }

        if (findContact(newContact.name) != null) {
            println("Contact with that name already exists!")
            return
        }

        // if all goes well, add the new contact to contacts var.
        contacts.add(newContact)
    } catch (e: Exception) {
        println("ERR: Invalid information")
        return
    }

    if (saveToJson())
        println("Contact successfully created and saved!")
}

fun searchContact() {
    val input = getInput("Enter contact name or phone number to search")
    val contactsFound: List<Contact> = findContacts(input)

    if (contactsFound.isEmpty())
        println("No contacts with name '$input' found!")
    else {
        println("Found ${contactsFound.size} contacts with name '$input': ")
        for ((i, contact) in contactsFound.withIndex()) {
            println("${i + 1}. ${contact.getInfo()}")
        }
    }
}

fun deleteContact() {
    var input = getInput("Enter contact name or phone number to delete")
    val contactToDelete: Contact? = findContact(input)

    if (contactToDelete == null) {
        println("No contacts found")
        return
    }

    input = getInput("Are you sure you want to delete '${contactToDelete.getInfo()}' ? [y/N]")
    if (input !in listOf("y", "Y")) {
        println("Aborting deletion...")
        return
    }

    contacts.remove(contactToDelete)
    if (saveToJson())
        println("Contact deleted successfully")
    else {
        println("ERR occurred, reverting changes...")
        contacts.add(contactToDelete)
    }
}

fun showMessages() {
    val messages = mutableSetOf<Message>()

    for (contact in contacts) {
        if (contact.messages.isNotEmpty()) {
            for (message in contact.messages) {
                messages.add(message)
            }
        }
    }

    if (messages.isEmpty()) {
        println("No messages found!")
        return
    }

    println("Found ${messages.size} messages: ")
    for ((i, message) in messages.withIndex()) {
        println("${i + 1}. ${message.getDetails()} \n")
    }
}

fun sendMessage() {
    var input = getInput("Enter recipient name or phone number")
    val recipientContact: Contact? = findContact(input)

    if (recipientContact == null) {
        println("No contacts found.")
        return
    }

    input = getInput("Enter message to send")
    val id = recipientContact.messages.size
    recipientContact.messages.add(Message(input, recipientContact.name, id))

    if (saveToJson())
        println("Message sent successfully!")
}


fun getInputWithChoices(choices: Array<String>): String {
    // this is just to make it look less cramped
    println()

    // prints the choices available
    for ((i, choice) in choices.withIndex()) {
        println("${i + 1}. $choice")
    }

    // then returns the input
    print("> ")
    return readln().trim()
}


fun getInput(msg: String): String {
    println(msg)
    print("> ")
    return readln().trim()
}

fun saveToJson(): Boolean {
    // convert contacts to json string
    val jsonContacts = gson.toJson(contacts)

    return try {
        // write jsonContacts to file
        // this will override the file or create a new one if it doesn't exist.
        Files.write(Paths.get("contacts.json"), jsonContacts.toByteArray())

        // return true if successful, or else false
        true
    } catch (e: Exception) {
        println(
            "ERR couldn't save contacts to file, \n" +
                    "this could be because of permission issues."
        )
        if (verbose)
            println("MORE INFO: $e")
        else
            println("Use -v or --verbose option to see more info about the error.")

        false
    }
}

fun findContact(nameOrPhone: String): Contact? {
    return contacts.find { nameOrPhone in listOf(it.name, it.phone) }
}

fun findContacts(nameOrPhone: String): List<Contact> {
    return contacts.filter { nameOrPhone in listOf(it.name, it.phone) }
}

