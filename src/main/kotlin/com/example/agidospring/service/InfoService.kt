package com.example.agidospring.service

import org.springframework.stereotype.Service

@Service
class InfoService {

    var base = "localhost:8080/"

    fun show():String{
        var frontPage=FrontPage()
        return frontPage.greeting +"<br>"+
                "<table>"+
        "<tr><th>RequestUrl</th><th>Parameter</th><th>Beschreibung</th></tr>"

        "</table>"



    }

  //  fun printHtmlRow()


}
class FrontPage()
{
    var greeting = "Willkommen zum Testprogramm."
    var customerFunctions= mapOf<String,String>(
    ).apply {
       "deposit" to "Einzahlung vornehmen"
        "withdraw" to "Auszahlung vornehmen"
        "sum" to "Summe der Ein- und Auszahlungen anzeigen lassen"
    }
    var serviceFunctions= mapOf<String,String>(
    ).apply{
        "richest" to "Kunden sortiert nach Vermögen anzeigen lassen."
        "showPendingWithdrawals" to "Auf Genehmigung wartenden Auszahlungen anzeigen lassen."
        "sum" to "Summe der Ein- und Auszahlungen anzeigen lassen"
        "authorizeWithdrawal" to "Auszahlung genehmigen"
    }
}