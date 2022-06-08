package com.example.agidospring.service

import Transaction
import TransactionType
import com.example.agidospring.UserType
import com.example.agidospring.enum.ErrorMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URLEncoder

@Service
class HTMLWrapper {
    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var transactionService: TransactionService

    @Autowired
    protected lateinit var externalServices: ExternalServices

    @Autowired
    lateinit var testDataService: TestDataService

    @Autowired
    lateinit var env: Environment


    fun loadTestData(): String {
        return testDataService.loadTestData()
            .joinToString("<br>") + "Für alle Test-Accounts ist das Passwort: test" + backButton()
    }

    fun registerCustomer(username: String, password: String): String {
        return "Kunde \"$username\" erfolgreich angelegt.<br>${
            userService.registerNewUser(
                username,
                password,
                UserType.Customer
            ).print()
        }" + backButton()
    }

    fun registerServiceEmployee(username: String, password: String, userType: UserType): String {
        return "Service-Mitarbeiter $username erfolgreich angelegt.<br>" + userService.registerNewUser(
            username,
            password,
            UserType.ServiceEmployee
        ).print() + backButton()
    }

    fun deposit(user: User, amount: BigDecimal): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.deposit(appUser, amount).message + backButton()
    }

    fun requestWithdrawal(user: User, amount: BigDecimal): String {

        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.requestWithdrawal(appUser, amount) + backButton()
    }


    fun MutableList<Transaction>?.htmlWrapTransactionList(): String {
        return this?.let { list ->
            return "<table>" + if (list.isNotEmpty()) {
                list[0].htmlTableHeaders() + list.joinToString(separator = "") { it.htmlTableRowPrint() }
            } else {
                ErrorMessage.NoPendingWithdrawalsFound.send()
            } + "</table>"
        } ?: return ErrorMessage.NoPendingWithdrawalsFound.send()
    }

    fun MutableList<Transaction>?.htmlWrapTransactionListWithButtons(): String {
        return this?.let { list ->
            return "<table>" + if (list.isNotEmpty()) {
                list[0].htmlTableHeaders() + list.joinToString(separator = "") {
                    it.htmlTableRowPrint() +
                    "\"<td><form action=\"${env.getProperty("request.customer.authorize.withdrawal")!!}\" method=\"get\" target=\"_blank\">" +
                            "<input type=\"text\" id=\"transactionId\" name=\"transactionId\" value=\"${it.transactionId}\"></input>" +
                            "<input type=\"submit\" value=\"freigeben\">" +
                            "</form></td>"
                }
            } else {
                ErrorMessage.NoPendingWithdrawalsFound.send()
            } + "</table>"
        } ?: return ErrorMessage.NoPendingWithdrawalsFound.send()
    }

    fun showMyPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showMyPendingWithdrawals(appUser).htmlWrapTransactionList() + backButton()

    }

    fun showPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showAllPendingWithdrawals(appUser).htmlWrapTransactionListWithButtons() + backButton()
    }

    fun authorizeWithdrawal(user: User, transaction: Transaction): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        transactionService.authorizeWithdrawal(appUser, transaction.transactionId, transaction.actor)
        return showPendingWithdrawals(user) + backButton()
    }

    fun authorizeWithdrawal(
        user: User,
        transactionId: String
    )
            : String {
        var transaction = transactionService.getTransactionById(transactionId)
            ?: return ErrorMessage.NoSuchTransactionFound.send(transactionId)
        return authorizeWithdrawal(user, transaction) + backButton()

    }

    fun sortAllCustomersByAmountOfMoney(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var ret = "<table><tr><th>Name</th><th>Kontostand</th><th>UserId</th></tr>"
        ret += userService.sortAllCustomersByAmountOfMoney(appUser)?.let { list ->
            list.joinToString(separator = "") { "<tr><td>${it.name}</td><td>${it.getBalance()}</td><td>${it.userId}</td></tr>" }
        } ?: ErrorMessage.NoRights.send()
        return "$ret</table>" + backButton()
    }


    fun showTransactionsMeta(user: User, ofWhomId: String?, from: String?, to: String?): String {

        return transactionService.getTransactionsMetaFunction(user, ofWhomId, from, to)
            .htmlWrapTransactionList() + backButton()
    }

    fun showTransactionSumMeta(user: User, ofWhomId: String?, from: String?, to: String?): String {
        var depositSum = BigDecimal.ZERO
        var withdrawalSum = BigDecimal.ZERO
        var totalSum = BigDecimal.ZERO

        var deposits = mutableListOf<Transaction>()
        var withdrawals = mutableListOf<Transaction>()
        var transactions = transactionService.getTransactionsMetaFunction(user, ofWhomId, from, to)

        transactions?.forEach {
            if (it.status != TransactionStatus.Finished) return@forEach


            when (it.transactionType) {
                TransactionType.Withdrawal -> {
                    withdrawals.add(it);withdrawalSum += it.amount
                    totalSum += it.amount
                }
                TransactionType.Deposit -> {
                    deposits.add(it);depositSum += it.amount
                    totalSum += it.amount
                }
                else -> return@forEach
            }

        } ?: return "Fehler aufgetreten oder keine Transaktionen gefunden."

        var html = "<table><tr><th>alle</th><th>nur Einzahlungen</th><th>nur Auszahlungen</th></tr>"
        html += "<tr><td>Gesamtsumme: $totalSum</td><td>Gesamtsumme: $depositSum</td><td>Gesamtsumme: $withdrawalSum</td></tr>"
        html += "<tr><td>${transactions.htmlWrapTransactionList()}</td><td>${deposits.htmlWrapTransactionList()}</td><td></td></tr></table>"

        return html + backButton()

    }


    fun welcomePage(user: User?): String {

        var add = ""

        userService.identify(user)?.let {

            when (it.userType) {
                UserType.Customer -> {
                    var map = mutableMapOf<String, String>(

                        "Einzahlen" to env.getProperty("request.customer.deposit")!!,
                        "Auszahlen" to env.getProperty("request.customer.withdraw")!!
                    )
                    map.forEach { (k, v) ->
                        add += numberInput("/$v", k)
                    }
                    var map2 = mutableMapOf<String, String>(
                        "Transaktionen zeigen" to env.getProperty("request.show.transactions")!!,
                        "Transaktionssumme zeigen" to env.getProperty("request.show.transactionsum")!!
                    )

                    map2.forEach { (k, v) ->
                        add += fromToDisplay("/$v", k)
                    }

                    mutableMapOf<String, String>("auf Bestätigung wartende Transaktionen anzeigen." to env.getProperty("request.customer.show.pendingwithdrawals")!!).forEach { k, v ->
                        add += button("/$v", k)

                    }

                }
                UserType.ServiceEmployee -> {
                    mutableMapOf<String, String>().apply {

                    }


                    mutableMapOf<String, String>(
                        "Transaktionen zeigen" to env.getProperty("request.show.transactions")!!,
                        "Transaktionssumme zeigen" to env.getProperty("request.show.transactionsum")!!,
                          ).forEach { k, v ->
                        add += fromToDisplay("/$v", k)
                    }

                    mutableMapOf<String, String>("auf Bestätigung wartende Transaktionen anzeigen." to env.getProperty("request.serviceemployee.show.pendingwithdrawals")!!).forEach { k, v ->
                        add += button("/$v", k)

                    }
                    add+=button("/${env.getProperty("request.serviceemployee.show.richest")}","reichsten User anzeigen")

                }
                else -> {


                }
            }

        }


        var notLoggedin = "<br>Bitte zuerst als Admin mit Username:abc und Passwort:123 anmelden."
        var b = ""
        if (user?.username == "abc") {
            b =
                "<br><form action=\"${env.getProperty("request.admin.register.serviceemployee.url")}\" method=\"get\" target=\"_blank\">" +
                        "Name<input type=\"text\" id=\"name\" name=\"name\">" +
                        "Passwort<input type=\"text\" id=\"password\" name=\"password\">" +
                        "<input type=\"submit\" value=\"Service-Mitarbeiter anlegen\">" +
                        "</form><br><form action=\"${env.getProperty("request.all.register.customer.url")}\" method=\"get\" target=\"_blank\">" +
                        "Name<input type=\"text\" id=\"name\" name=\"name\">" +
                        "Passwort<input type=\"text\" id=\"password\" name=\"password\">" +
                        "<input type=\"submit\" value=\"Kunde anlegen\">" +
                        "</form><br>" + button(
                    "/${env.getProperty("request.admin.load.testdata.url")!!}",
                    "Testdaten laden"
                )
        }
        return "Agido-Testaufgabe<br>Willkommen ${user?.username ?: notLoggedin}<br>$add${b}"
    }

    fun fromToDisplay(url: String, infoText: String): String {
        return "<form action=\"${url}\" method=\"get\" target=\"_blank\">\n" +
                "  <label for=\"from\">von:</label>\n" +
                "  <input type=\"text\" id=\"from\" name=\"from\">\n" +
                "  <label for=\"to\">bis</label>\n" +
                "  <input type=\"text\" id=\"to\" name=\"to\">  <input type=\"submit\" value=\"$infoText\">\n" +
                "</form>"
    }

    fun numberInput(url: String, text: String): String {
        return "<form action=\"$url\" method=\"get\" target=\"_blank\">" +
                "<input type=\"text\" id=\"amount\" name=\"amount\">" +
                "<input type=\"submit\" value=\"$text\">\n" +
                "</form>"
    }

    fun button(url: String, text: String): String {
        return "<form action=\"${url}\" method=\"get\" target=\"_blank\">" +
                "<input type=\"submit\" value=\"$text\">" +
                "</form>"
    }

    fun backButton(): String {
        return button("/", "zurück")
    }

}