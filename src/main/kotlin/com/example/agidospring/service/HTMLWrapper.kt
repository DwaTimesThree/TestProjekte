package com.example.agidospring.service

import Transaction
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
        return "Kunde \"$username\" erfolgreich angelegt.<br>${userService.registerNewUser(username, password, UserType.Customer).print()}"
    }

    fun registerServiceEmployee(username: String, password: String, userType: UserType): String {
        return "Service-Mitarbeiter $username erfolgreich angelegt.<br>" + userService.registerNewUser(username, password, UserType.ServiceEmployee).print()
    }

    fun deposit(user: User, amount: BigDecimal): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.deposit(appUser, amount).message
    }

    fun requestWithdrawal(user: User, amount: BigDecimal): String {

        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.requestWithdrawal(appUser, amount)
    }

    fun showPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showAllPendingWithdrawals(appUser).htmlWrapTransactionList()
    }

    fun MutableList<Transaction>?.htmlWrapTransactionList():String
    {
        return this?.let { list ->
            return "<table>" + if (list.isNotEmpty()) {
                list[0].htmlTableHeaders()+list.joinToString (separator = ""){ it.htmlTableRowPrint() }
            } else {
                ErrorMessage.NoPendingWithdrawalsFound.send()
            } + "</table>"
        } ?: return ErrorMessage.NoPendingWithdrawalsFound.send()
    }

    fun showMyPendingWithdrawals(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showMyPendingWithdrawals(appUser).htmlWrapTransactionList()

    }


    fun authorizeWithdrawal(user: User, transaction: Transaction): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        transactionService.authorizeWithdrawal(appUser, transaction.transactionId, transaction.actor)
        return showPendingWithdrawals(user)
    }

    fun authorizeWithdrawal(user: User,
                            transactionId: String)
            : String {
        var transaction = transactionService.getTransactionById(transactionId)
                ?: return ErrorMessage.NoSuchTransactionFound.send(transactionId)
        return authorizeWithdrawal(user, transaction)

    }

    fun sortAllCustomersByAmountOfMoney(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var ret = "<table>"
       ret+= userService.sortAllCustomersByAmountOfMoney(appUser)?.let { list ->
           list.joinToString(separator = "") { "<tr><td>${it.name}</td><td>${it.getBalance()}</td></tr>" }
        }?:ErrorMessage.NoRights.send()
        return "$ret</table>"
    }


    fun showMyTransactions(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showMyTransactions(appUser).htmlWrapTransactionList()
    }

    fun showMyTransactions(user: User, from: String, to: String): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.showMyTransactions(appUser,from,to).htmlWrapTransactionList()
    }

    fun showTransactionsOf(user: User,appUserId:String):String
    {
        var serviceUser= userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var appUser= userService.getAppUserById(appUserId) ?: return ErrorMessage.NoSuchUser.send()
      return transactionService.getAllTransactionsOf(serviceUser,appUser)?.htmlWrapTransactionList()?:return ErrorMessage.NoRights.send()


    }
    fun showTransactionsOf(user: User,appUserId:String,from:String,to:String):String
    {
        var serviceUser= userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var appUser= userService.getAppUserById(appUserId) ?: return ErrorMessage.NoSuchUser.send()
        return transactionService.getAllTransactionsOf(serviceUser,appUser,from,to)?.htmlWrapTransactionList()?:return ErrorMessage.NoRights.send()

    }



    fun showMyTransactionSum(user: User): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return "Gesamtsumme aller Transaktionen von User ${appUser.name} beträgt: ${transactionService.sumOfAllMyTransactions(appUser)}<br>"+transactionService.showMyTransactions(appUser).htmlWrapTransactionList()

    }

    fun showMyTransactionSum(user: User, from: String, to: String): String {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        return "Gesamtsumme aller Transaktionen im Zeitraum $from bis $to von User ${appUser.name} beträgt: ${transactionService.sumOfMyTransactionsInPeriod(from, to, appUser)}"+transactionService.showMyTransactions(appUser,from,to).htmlWrapTransactionList()
    }

    fun showTransactionSumOfAllUsers(user:User):String
    {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()

        var sum = transactionService.sumOfAllTransactions(appUser)?:return ErrorMessage.NoRights.send()+" or "+ErrorMessage.ParseErrorYYYY_MM_DD.send()
        return "Gesamtsumme aller Transaktionen beträgt: $sum"
    }
    fun showTransactionSumOfAllUsers(user:User,from:String,to:String):String
    {
        var appUser = userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var sum = transactionService.sumOfAllTransactions(appUser,from,to)?:return ErrorMessage.NoRights.send()+" or "+ErrorMessage.ParseErrorYYYY_MM_DD.send()

        return "Gesamtsumme aller Transaktionen im Zeitraum $from bis $to beträgt: $sum"
    }
    fun showTransactionSumOf(user: User,appUserId:String,from:String,to:String):String
    {
      var serviceUser= userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var appUser= userService.getAppUserById(appUserId) ?: return ErrorMessage.NoSuchUser.send()
        var sum = transactionService.sumOfOtherUserTransactions(serviceUser,appUser,from,to)?:return ErrorMessage.NoRights.send()+" or "+ErrorMessage.ParseErrorYYYY_MM_DD.send()
        return "Gesamtsumme aller Transaktionen von ${appUser.name} im Zeitraum $from bis $to beträgt: $sum"

    }
    fun showTransactionSumOf(user: User,appUserId:String):String
    {

        var serviceUser= userService.getAppUserById(userService.createId(user)) ?: return ErrorMessage.NoSuchUser.send()
        var appUser= userService.getAppUserById(appUserId) ?: return ErrorMessage.NoSuchUser.send()
        var sum = transactionService.sumOfOtherUserTransactions(serviceUser,appUser)?:return ErrorMessage.NoRights.send()+" or "+ErrorMessage.ParseErrorYYYY_MM_DD.send()
        return "Gesamtsumme aller Transaktionen von ${appUser.name} beträgt: $sum"
    }

    fun welcomePage(user: User): String {
        return "Agido-Testaufgabe<br>Willkommen ${user.username}!"
    }
}