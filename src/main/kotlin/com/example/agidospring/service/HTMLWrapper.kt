package com.example.agidospring.service

import Transaction
import TransactionType
import com.example.agidospring.UserType
import com.example.agidospring.enum.ErrorMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import java.math.BigDecimal

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


    fun loadTestData(): String {
        return testDataService.loadTestData().joinToString("<br>") + "Für alle Test-Accounts ist das Passwort: test"
    }

    fun registerCustomer(username: String, password: String): String {
        return "Kunde \"$username\" erfolgreich angelegt.<br>${
            userService.registerNewUser(
                username,
                password,
                UserType.Customer
            ).print()
        }"
    }

    fun registerServiceEmployee(username: String, password: String, userType: UserType): String {
        return "Service-Mitarbeiter $username erfolgreich angelegt.<br>" + userService.registerNewUser(
            username,
            password,
            UserType.ServiceEmployee
        ).print()
    }

    fun deposit(user: User, amount: BigDecimal): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.deposit(appUser, amount).message
    }

    fun requestWithdrawal(user: User, amount: BigDecimal): String {

        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.requestWithdrawal(appUser, amount)
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

    fun showMyPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showMyPendingWithdrawals(appUser).htmlWrapTransactionList()

    }

    fun showPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showAllPendingWithdrawals(appUser).htmlWrapTransactionList()
    }

    fun authorizeWithdrawal(user: User, transaction: Transaction): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        transactionService.authorizeWithdrawal(appUser, transaction.transactionId, transaction.actor)
        return showPendingWithdrawals(user)
    }

    fun authorizeWithdrawal(
        user: User,
        transactionId: String
    )
            : String {
        var transaction = transactionService.getTransactionById(transactionId)
            ?: return ErrorMessage.NoSuchTransactionFound.send(transactionId)
        return authorizeWithdrawal(user, transaction)

    }

    fun sortAllCustomersByAmountOfMoney(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var ret = "<table><tr><th>Name</th><th>Kontostand</th><th>UserId</th></tr>"
        ret += userService.sortAllCustomersByAmountOfMoney(appUser)?.let { list ->
            list.joinToString(separator = "") { "<tr><td>${it.name}</td><td>${it.getBalance()}</td><td>${it.userId}</td></tr>" }
        } ?: ErrorMessage.NoRights.send()
        return "$ret</table>"
    }



    fun showTransactionsMeta(user: User, ofWhomId: String?, from: String?, to: String?): String {

        return  transactionService.getTransactionsMetaFunction(user, ofWhomId, from, to).htmlWrapTransactionList()
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

        return html

    }


    fun welcomePage(user: User): String {
        return "Agido-Testaufgabe<br>Willkommen ${user.username}!"
    }
}