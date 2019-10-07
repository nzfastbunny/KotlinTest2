package com.nurturecloud

import com.nurturecloud.model.Suburb
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashMap

class AppTest {
    private lateinit var sysOut: PrintStream
    private var outContent: ByteArrayOutputStream = ByteArrayOutputStream()

    @Before
    fun setUpStreams() {
        sysOut = System.out
        System.setOut(PrintStream(outContent))
    }

    @After
    fun revertStreams() {
        System.setOut(sysOut)
    }

    @Test
    fun testProcessSuburbs_invalidSuburb() {
        val input = "Atlantis\n2000\n"
        processSuburbs(HashMap(), HashMap(), Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Incorrect suburb and postcode combination - ATLANTIS and 2000\n" +
                "Please check the details and try again"))
    }

    @Test
    fun testProcessSuburbs_invalidPostcode() {
        val input = "Test\n232323\n"
        processSuburbs(HashMap(), HashMap(), Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Incorrect suburb and postcode combination - TEST and 232323\n" +
                "Please check the details and try again"))
    }

    @Test
    fun testProcessSuburbs_invalidCombo() {
        val input = "Rosebery\n2000\n"
        processSuburbs(HashMap(), HashMap(), Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Incorrect suburb and postcode combination - ROSEBERY and 2000\n" +
                "Please check the details and try again"))
    }

    @Test
    fun testProcessSuburbs_noEntries() {
        val input = "\n\n"
        processSuburbs(HashMap(), HashMap(), Scanner(input))
        val output = outContent.toString()
        Assert.assertEquals("Please enter a suburb name: Please enter the postcode: ", output)
    }

    @Test
    fun testProcessSuburbs_nonPhysicalAddress() {
        val input = "chatswood\n2057\n" // also tests case insensitivity
        val suburbMap = HashMap<String, Suburb>()
        val suburb = Suburb(2057, "CHATSWOOD", "NSW", "", "")
        suburbMap["CHATSWOOD-2057"] = suburb

        processSuburbs(suburbMap, HashMap(), Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("The supplied suburb and postcode combination (CHATSWOOD, 2057) is a non-physical address\n" +
                "Please check the details and try again"))
    }

    @Test
    fun testProcessSuburbs() {
        val input = "Sydney\n2000\n" // also tests case insensitivity
        val suburbMap = HashMap<String, Suburb>()
        val stateMap = HashMap<String, ArrayList<Suburb>>()
        stateMap["NSW"] = ArrayList()

        var suburb = Suburb(2000, "SYDNEY", "NSW", "", "",
                BigDecimal(151.2099), BigDecimal(-33.8697))
        suburbMap["SYDNEY-2000"] = suburb
        stateMap["NSW"]?.add(suburb)

        suburb = Suburb(2010, "SURRY HILLS", "NSW", "", "",
                BigDecimal(151.21), BigDecimal(-33.8849))
        suburbMap["SURRY HILLS-2010"] = suburb
        stateMap["NSW"]?.add(suburb)

        suburb = Suburb(2136, "BURWOOD HEIGHTS", "NSW", "", "",
                BigDecimal(151.1039), BigDecimal(-33.8893))
        suburbMap["BURWOOD HEIGHTS-2136"] = suburb
        stateMap["NSW"]?.add(suburb)

        processSuburbs(suburbMap, stateMap, Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Nearby Suburbs:\r\n\tSURRY HILLS  2010"))
        Assert.assertTrue(output.contains("Fringe Suburbs:\r\n\tBURWOOD HEIGHTS  2136"))
    }

    @Test
    fun testProcessSuburbs_noneFound() {
        val input = "Sydney\n2000\n" // also tests case insensitivity
        val suburbMap = HashMap<String, Suburb>()
        val stateMap = HashMap<String, ArrayList<Suburb>>()
        stateMap["NSW"] = ArrayList()

        val suburb = Suburb(2000, "SYDNEY", "NSW", "", "",
                BigDecimal(151.2099), BigDecimal(-33.8697))
        suburbMap["SYDNEY-2000"] = suburb
        stateMap["NSW"]?.add(suburb)

        processSuburbs(suburbMap, stateMap, Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Nothing found for SYDNEY, 2000!!"))
    }

    @Test
    fun testProcessSuburbs_2close() {
        val input = "Sydney\n2000\n" // also tests case insensitivity
        val suburbMap = HashMap<String, Suburb>()
        val stateMap = HashMap<String, ArrayList<Suburb>>()
        stateMap["NSW"] = ArrayList()

        var suburb = Suburb(2000, "SYDNEY", "NSW", "", "",
                BigDecimal(151.2099), BigDecimal(-33.8697))
        suburbMap["SYDNEY-2000"] = suburb
        stateMap["NSW"]?.add(suburb)

        suburb = Suburb(2203, "DULWICH HILL", "NSW", "", "",
                BigDecimal(151.1382), BigDecimal(-33.9046))
        suburbMap["DULWICH HILL-2203"] = suburb
        stateMap["NSW"]?.add(suburb)

        suburb = Suburb(2010, "SURRY HILLS", "NSW", "", "",
                BigDecimal(151.21), BigDecimal(-33.8849))
        suburbMap["SURRY HILLS-2010"] = suburb
        stateMap["NSW"]?.add(suburb)

        processSuburbs(suburbMap, stateMap, Scanner(input))
        val output = outContent.toString()
        Assert.assertTrue(output.contains("Nearby Suburbs:\r\n\tSURRY HILLS  2010\r\n\tDULWICH HILL  2203"))
    }

    @Test
    fun testFindDistance_nulls() {
        val result = findDistance(Suburb(0, "", ""), Suburb(0, "", ""))

        val expected = BigDecimal(100)
        Assert.assertEquals(expected, result)
    }

    @Test
    fun testFindDistance() {
        var home = Suburb(2000, "SYDNEY", "NSW", "", "",
                BigDecimal(151.2099), BigDecimal(-33.8697))
        var nearby = Suburb(2010, "SURRY HILLS", "NSW", "", "",
                BigDecimal(151.21), BigDecimal(-33.8849))

        var result = findDistance(home, nearby)

        var expected = BigDecimal(1.6890).setScale(2, RoundingMode.HALF_EVEN)
        Assert.assertEquals(expected, result)

        home = Suburb(2018, "ROSEBERY", "NSW", "", "",
                BigDecimal(151.2048), BigDecimal(-33.9186))
        nearby = Suburb(2060, "WAVERTON", "NSW", "", "",
                BigDecimal(151.1988), BigDecimal(-33.8381))

        result = findDistance(home, nearby)

        expected = BigDecimal(8.9724469).setScale(2, RoundingMode.HALF_EVEN)
        Assert.assertEquals(expected, result)
    }

    @Test
    fun testDegreesToRadians() {
        // A quick test to see if it lines up with the 5 results from an external trusted source
        var degrees = BigDecimal(50)
        var radians = degreesToRadians(degrees)

        var rightAnswer = BigDecimal(0.872665).setScale(6, RoundingMode.HALF_EVEN)
        Assert.assertEquals(rightAnswer, radians.setScale(6, RoundingMode.HALF_EVEN))

        degrees = BigDecimal(15)
        radians = degreesToRadians(degrees)

        rightAnswer = BigDecimal(0.261799).setScale(6, RoundingMode.HALF_EVEN)
        Assert.assertEquals(rightAnswer, radians.setScale(6, RoundingMode.HALF_EVEN))

        degrees = BigDecimal(5.678)
        radians = degreesToRadians(degrees)

        rightAnswer = BigDecimal(0.099099795).setScale(6, RoundingMode.HALF_EVEN)
        Assert.assertEquals(rightAnswer, radians.setScale(6, RoundingMode.HALF_EVEN))

        degrees = BigDecimal(8.347)
        radians = degreesToRadians(degrees)

        rightAnswer = BigDecimal(0.14568263).setScale(6, RoundingMode.HALF_EVEN)
        Assert.assertEquals(rightAnswer, radians.setScale(6, RoundingMode.HALF_EVEN))

        degrees = BigDecimal(22.765)
        radians = degreesToRadians(degrees)

        rightAnswer = BigDecimal(0.397324204).setScale(6, RoundingMode.HALF_EVEN)
        Assert.assertEquals(rightAnswer, radians.setScale(6, RoundingMode.HALF_EVEN))
    }

    @Test
    fun testLoadSuburbsSuccessfully() {
        val suburbs = loadSuburbs()

        // Check all the suburbs have been loaded
        Assert.assertFalse(suburbs.isEmpty())
        Assert.assertEquals(16544, suburbs.size) // the current number of suburbs in the JSON file

        // Check a couple of the objects to see if they have been populated correctly
        val firstOne = suburbs[0] // no long or lat though
        Assert.assertTrue(200 == firstOne.Pcode)
        Assert.assertEquals("AUSTRALIAN NATIONAL UNIVERSITY", firstOne.Locality)
        Assert.assertEquals("ACT", firstOne.State)
        Assert.assertNull(firstOne.Longitude)
        Assert.assertNull(firstOne.Latitude)

        val secondOne = suburbs[1] // has long and lat
        Assert.assertTrue(800 == secondOne.Pcode)
        Assert.assertEquals("DARWIN", secondOne.Locality)
        Assert.assertEquals("NT", secondOne.State)
        Assert.assertEquals(BigDecimal(-12.4633).setScale(4, RoundingMode.HALF_EVEN), secondOne.Latitude)
        Assert.assertEquals(BigDecimal(130.8434).setScale(4, RoundingMode.HALF_EVEN), secondOne.Longitude)
    }
}
