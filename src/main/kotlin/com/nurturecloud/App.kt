package com.nurturecloud

import java.util.*

fun main(args: Array<String>) {
    val command = Scanner(System.`in`)
    val running = true

    while (running) {

        println("Please enter a suburb name: ")
        val suburbName = command.nextLine()

        println("Please enter the postcode: ")
        val postcode = command.nextLine()

        println("Nothing found for $suburbName, $postcode!!\n")

    }
    command.close()
}