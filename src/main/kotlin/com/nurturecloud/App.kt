package com.nurturecloud

import com.google.common.io.Resources
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nurturecloud.model.Suburb
import com.nurturecloud.model.SuburbResults
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The mean radius of the Earth in kms
 */
const val EARTH_MEAN_RADIUS = 6371

/**
 * An application to find the nearby (< 10km) and fringe (< 50km) suburbs to the suburb supplied.
 */
fun main() {
    val suburbs = loadSuburbs() // load the suburb object from the provided JSON

    // SuburbMap is needed to link the supplied info to a suburb object
    val suburbMap = HashMap<String, Suburb>()
    // StateMap is used to focus the searching on the likely closest suburb (not as effective on boarder towns though)
    val stateMap = HashMap<String, ArrayList<Suburb>>()
    for (suburb in suburbs) {
        suburbMap[suburb.Locality + "-" + suburb.Pcode] = suburb

        val state = suburb.State
        if (!stateMap.containsKey(state)) {
            stateMap[state] = ArrayList()
        }
        stateMap[state]?.add(suburb)
    }

    // Accept the command line input and process the results
    val command = Scanner(System.`in`)
    processSuburbs(suburbMap, stateMap, command)
    command.close()
}

/**
 * Using the command line input, process the suburbs to find the best matches for nearby and fringe suburbs
 *
 * @param suburbMap the map of the all the suburbs available keyed by the suburb-postcode concatenation
 * @param stateMap  the map of list of suburbs keyed by their state/territory
 * @param command   the command line input scanner (done this way to make unit tests easier/possible)
 */
fun processSuburbs(suburbMap: Map<String, Suburb>, stateMap: Map<String, List<Suburb>>, command: Scanner) {
    var running = true
    while (running) {
        // Ask for user input (added checking included for unit testing)
        print("Please enter a suburb name: ")
        var suburbName = ""
        if (command.hasNext()) {
            suburbName = command.nextLine().toUpperCase()
        }

        print("Please enter the postcode: ")
        var postcode = ""
        if (command.hasNext()) {
            postcode = command.nextLine().toUpperCase()
        }

        // Mostly included for unit testing but also a way to exit the application gracefully
        if (suburbName.isEmpty() && postcode.isEmpty()) {
            running = false
            continue // Effectively end the application
        }

        val home = suburbMap["$suburbName-$postcode"]

        // Check for incorrect details
        if (home == null) {
            println("Incorrect suburb and postcode combination - $suburbName and $postcode\n" +
                    "Please check the details and try again")
            continue
        }

        // Check for non-physical address (no location details - therefore no distance can be determined)
        if (home.Latitude == null || home.Longitude == null) {
            println("The supplied suburb and postcode combination ($suburbName, $postcode) is a non-physical address\n" +
                    "Please check the details and try again")
            continue
        }

        // Get the list of suburbs for the state of the supplied suburb
        val localSuburbs = stateMap.getOrDefault(home.State, ArrayList())

        // Find the closest suburbs
        val results = findCloseSuburbs(home, localSuburbs)

        // Output the findings
        outputResults(suburbName, postcode, results)
    }
}

/**
 * Find the nearby and fringe suburbs given the supplied suburb and the list of suburbs in a similar area
 *
 * @param home         the supplied suburb
 * @param localSuburbs the list of suburbs from the supplied suburb's state
 * @return the map of results containing 2 list of nearby and fringe suburbs
 */
fun findCloseSuburbs(home: Suburb, localSuburbs: List<Suburb>): Map<String, List<SuburbResults>> {
    val results: Map<String, ArrayList<SuburbResults>> = HashMap<String, ArrayList<SuburbResults>>(2).apply {
        put("Nearby", ArrayList())
        put("Fringe", ArrayList())
    }

    for (suburb in localSuburbs) {
        val distance = findDistance(home, suburb)

        val nearbyList = results.getOrDefault("Nearby", ArrayList())
        val fringeList = results.getOrDefault("Fringe", ArrayList())

        // If the distance is between 0 and 10kms then it is a nearby suburb (add it to the list
        if (distance > BigDecimal.ZERO && distance <= BigDecimal.TEN) {
            val nearby = SuburbResults(suburb.Locality, suburb.Pcode, distance)
            nearbyList.add(nearby)
        } else if (distance > BigDecimal(10) && // Fringe suburbs - 10 to 50kms away
                distance <= BigDecimal(50)) {
            val fringe = SuburbResults(suburb.Locality, suburb.Pcode, distance)
            fringeList.add(fringe)
        }

        // Collecting 600 results each to choose from seemed the best balance of time and accuracy
        if (nearbyList.size > 600 && fringeList.size > 600) {
            return results
        }
    }

    return results
}

/**
 * Output the results of the search to the user - nearby and fringe lists of suburbs
 *
 * @param suburbName the name of the suburb supplied
 * @param postcode   the post code of the suburb supplied
 * @param results    the map of results - containing nearby and fringe lists of suburbs
 */
fun outputResults(suburbName: String, postcode: String, results: Map<String, List<SuburbResults>>) {
    val nearbyList = results.getOrDefault("Nearby", ArrayList())
    val fringeList = results.getOrDefault("Fringe", ArrayList())

    if (nearbyList.isEmpty() && fringeList.isEmpty()) {
        println("Nothing found for $suburbName, $postcode!!\n")
    } else {
        // Sort the results to list the closest suburbs first
        var sortedNearby = nearbyList.sortedWith(compareBy { it.distance })
        var sortedFringe = fringeList.sortedWith(compareBy { it.distance })

        // Clear away the unnecessary results to get a maximum of 15
        if (sortedNearby.size > 15) {
            sortedNearby = sortedNearby.subList(0, 15)
        }

        if (sortedFringe.size > 15) {
            sortedFringe = sortedFringe.subList(0, 15)
        }

        // Print out the result to the user
        println("\nNearby Suburbs:")
        sortedNearby.forEach { println("\t${it.suburb}  ${it.postCode}") }

        println("\nFringe Suburbs:")
        sortedFringe.forEach { println("\t${it.suburb}  ${it.postCode}") }
        print("\n\n")
    }
}

/**
 * Use the Haversine formula to determine the distance between 2 points on the Earth
 *
 * @param home   the supplied suburb
 * @param suburb one of the possible nearby (or fringe) suburbs
 * @return the distance in kilometres (kms) between the 2 suburbs
 */
fun findDistance(home: Suburb, suburb: Suburb): BigDecimal {
    /**
     * Formula for distance is:
     *         a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
     *         c = 2 ⋅ atan2( √a, √(1−a) )
     *         d = R ⋅ c
     *         where	φ is latitude, λ is longitude, R is earth’s radius (mean radius = 6,371km)
     *         note that angles need to be in radians to pass to trig functions!
     */
    var lat1 = home.Latitude
    val lon1 = home.Longitude

    var lat2 = suburb.Latitude
    val lon2 = suburb.Longitude

    // If we don't have all the coordinates then we can do the calculation
    if (lat1 == null || lat2 == null || lon1 == null || lon2 == null) {
        return BigDecimal(100) // return a value that will be filtered out
    }

    val earthRadiusKm = BigDecimal(EARTH_MEAN_RADIUS)

    var dLat = lat2.subtract(lat1)
    var dLon = lon2.subtract(lon1)
    dLat = degreesToRadians(dLat)
    dLon = degreesToRadians(dLon)

    lat1 = degreesToRadians(lat1)
    lat2 = degreesToRadians(lat2)

    val a = BigDecimal(sin(dLat.toDouble() / 2) * sin(dLat.toDouble() / 2) +
            sin(dLon.toDouble() / 2) * sin(dLon.toDouble() / 2) * cos(lat1.toDouble()) * cos(lat2.toDouble()))
    val c = BigDecimal(2 * atan2(sqrt(a.toDouble()), sqrt(1 - a.toDouble())))
    return earthRadiusKm.multiply(c).setScale(2, RoundingMode.HALF_EVEN)
}

/**
 * Convert the degrees to radians to use in the calculations
 *
 * @param degrees the coordinate in degrees
 * @return the coordinate in radians
 */
fun degreesToRadians(degrees: BigDecimal): BigDecimal {
    return degrees.multiply(BigDecimal(Math.PI / 180))
}

/**
 * Load the suburbs from the supplied JSON file
 *
 * @return the list of suburbs
 */
fun loadSuburbs(): List<Suburb> {
    return try {
        val url = Resources.getResource("aus_suburbs.json")
        val jsonStr = Resources.toString(url, Charsets.UTF_8)

        // Convert JSON string to a list of Suburb objects
        val gson = Gson()
        val listType = object : TypeToken<List<Suburb>>() {}.type
        gson.fromJson(jsonStr, listType)
    } catch (e: IOException) {
        println("An error has occurred while loading the suburbs")
        ArrayList()
    }
}