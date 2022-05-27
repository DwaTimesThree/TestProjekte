package com.example.agidospring.service

import Transaction
import TransactionType
import com.example.agidospring.AppUser
import com.example.agidospring.Controller.StandardResponse
import com.example.agidospring.UserType
import com.example.agidospring.enum.DBType
import com.example.agidospring.enum.ErrorMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TransactionService {


    val zonedDateTimeAdd="T00:00:00+01:00"

    @Autowired
    lateinit var externalServices: ExternalServices

    private var transactions = mutableListOf<Transaction>()

    fun getTransactionById(id: String): Transaction? {
        return transactions.find { it.transactionId == id }
    }

    private fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }

    fun requestWithdrawal(appUser: AppUser, amount: BigDecimal): String {
        if (!appUser.isAllowedToWithdraw()) return ErrorMessage.NoRightsToWithdraw.send()
        var transaction = Transaction(TransactionType.Withdrawal, amount, appUser).apply {
            status = if (appUser.getBalance() < amount) TransactionStatus.Failed else TransactionStatus.PermissionPending
        }
        transaction.writeTransactionIntoDB(DBType.Memory)
        return transaction.status.name//TODO
    }

    fun deposit(user: AppUser, amount: BigDecimal): StandardResponse {
        if (!user.isAllowedToMakeDisposits()) return StandardResponse(ErrorMessage.NoRightsToDeposit.send())
        var response = externalServices.startExternalDepositProcess(user)
        var transaction = Transaction(TransactionType.Deposit, amount, user).apply {
            status = if (response.successful) TransactionStatus.Finished else TransactionStatus.Failed
        }
        transaction.writeTransactionIntoDB(DBType.Memory)
        user.calcBalance(transactions)
        return StandardResponse("${transaction.transactionType.name}:${transaction.status}")
    }



    fun MutableList<Transaction>.checkValidityOfWithdrawalRequests():MutableList<Transaction> {
        this.forEach {
            if (it.amount > it.actor.getBalance()) {
                it.apply { status = TransactionStatus.Failed }
                return@forEach
            }
        }
        return this
    }

    fun showMyPendingWithdrawals(user: AppUser): MutableList<Transaction>? {
        if (user.userType != UserType.Customer) return null
        return  transactions.filter { transaction -> !transaction.permissionGranted && transaction.status == TransactionStatus.PermissionPending && transaction.actor.isSameUserAs(user) }.toMutableList().checkValidityOfWithdrawalRequests()

    }
    fun showAllPendingWithdrawals(user: AppUser): MutableList<Transaction>? {
        if (user.userType != UserType.ServiceEmployee) return null
       return  transactions.filter { transaction -> !transaction.permissionGranted && transaction.status == TransactionStatus.PermissionPending }.toMutableList().checkValidityOfWithdrawalRequests()

    }



    fun authorizeWithdrawal(appUser: AppUser, id: String, transactionUser: AppUser) {
        //transactionId wäre im Realsystem distinct
        if (!appUser.isAllowedToPermitWithdrawals()) return
        var transaction = transactions.first {
            !it.permissionGranted && (it.transactionId == id) && transactionUser.isSameUserAs(it.actor)
        }.apply {
            permissionGranted = true
        }
        val response = externalServices.sendMoneyToCustomerAccount(transaction)
        transactionUser.addBalance(-transaction.amount)
        transaction.apply { status = if (response.successful) TransactionStatus.Finished else TransactionStatus.Failed }
    }


    fun Transaction.writeTransactionIntoDB(dbType: DBType) {
        //         Aufgabenstellung: Speicherung in Memory
        when (dbType) {
            DBType.Memory -> {

                transactions.find { (it.transactionType == TransactionType.Deposit || it.transactionType == TransactionType.Withdrawal) && this.time == it.time }
                        ?.let {
                            this.time = ZonedDateTime.now()
                            this.writeTransactionIntoDB(dbType)
                            return

                        }
                addTransaction(this)


            }
            else -> {}

        }


    }


    fun String.YYYYMMDDtoParsableZonedDateTime():ZonedDateTime?
    {
        return ZonedDateTime.parse(this+zonedDateTimeAdd, DateTimeFormatter.ISO_ZONED_DATE_TIME)?:null
    }

    fun getAllTransactionsOf(user: AppUser,appUser: AppUser, from: String,to: String):MutableList<Transaction>?
    {
        if (user.userType != UserType.ServiceEmployee) return null
        var start =from.YYYYMMDDtoParsableZonedDateTime()?:return null
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?:return null
       return  transactions.filter { transaction -> transaction.actor.isSameUserAs(appUser)&& transaction.time >= start && transaction.time <= end  }.toMutableList()
    }

    fun getAllTransactionsOf(user:AppUser, appUser:AppUser):MutableList<Transaction>? {
        if (user.userType != UserType.ServiceEmployee) return null
        return transactions.filter { it.actor.isSameUserAs(appUser) }.asReversed().toMutableList()
    }


    fun showMyTransactions(appUser: AppUser,from: String,to:String):MutableList<Transaction>?
    {
        var start =from.YYYYMMDDtoParsableZonedDateTime()?:return null
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?:return null
       return transactions.filter { transaction -> transaction.actor.isSameUserAs(appUser)&& transaction.time >= start && transaction.time <= end  }.toMutableList()
    }

    fun showMyTransactions(user: AppUser): MutableList<Transaction> {
        return transactions.filter { transaction -> transaction.actor.isSameUserAs(user)}.asReversed().toMutableList()
    }


    fun reversedChronologicalOrder(): MutableList<Transaction> {
        return chronologicOrder().asReversed()
    }

    fun chronologicOrder(): MutableList<Transaction> {
        transactions.toMutableList().run {
            sortBy { transaction -> transaction.time }
            return this
        }
    }


    fun sumOfAllUserTransactionsInPeriod(from: String, to: String) {
        var sum = BigDecimal.ZERO
        var add = "T00:00:00+01:00"
        var fromx = from + add
        var tox = to + add
        var start = ZonedDateTime.parse(fromx, DateTimeFormatter.ISO_ZONED_DATE_TIME) ?: return
        var end = ZonedDateTime.parse(tox, DateTimeFormatter.ISO_ZONED_DATE_TIME) ?: return
        end = end.plusDays(1)

        transactions.filter { transaction -> transaction.time >= start && transaction.time <= end }.forEach {
            sum += it.amount
        }
        println(sum)
    }

    fun sumOfAllTransactions(serviceUser: AppUser):BigDecimal?
    {
        if(serviceUser.userType!=UserType.ServiceEmployee)return null
        var sum = BigDecimal.ZERO
        transactions.forEach { sum+=it.amount }
        return sum
    }

    fun sumOfAllTransactions(serviceUser: AppUser,from: String,to:String):BigDecimal?
    {

        if(serviceUser.userType!=UserType.ServiceEmployee)return null
        var start =from.YYYYMMDDtoParsableZonedDateTime()?:return null
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?:return null
        var sum = BigDecimal.ZERO
        transactions.filter {  transaction -> transaction.time >= start && transaction.time <= end  }.forEach { sum+=it.amount }
        return sum
    }


    fun sumOfAllMyTransactions(user: AppUser): String {
        var sum = BigDecimal.ZERO
        transactions.filter { transaction -> user.userId == transaction.actor.userId }.forEach {
            sum += it.amount
        }
        return "$sum"
    }



    fun sumOfMyTransactionsInPeriod(from: String, to: String, user: AppUser): String {

        var sum = BigDecimal.ZERO
        var start =from.YYYYMMDDtoParsableZonedDateTime()?:return return ErrorMessage.ParseErrorYYYY_MM_DD.send(from)
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?:return ErrorMessage.ParseErrorYYYY_MM_DD.send(to)

        transactions.filter { transaction -> transaction.time >= start && transaction.time <= end && user.userId == transaction.actor.userId }
                .forEach {
                    sum += it.amount

                }
        return "$sum"
    }


    fun sumOfOtherUserTransactions(serviceUser: AppUser,appUser: AppUser):BigDecimal?
    {
        if(serviceUser.userType!=UserType.ServiceEmployee)return null
        var sum = BigDecimal.ZERO
        transactions.filter { transaction -> appUser.userId == transaction.actor.userId }.forEach {
            sum+=it.amount
        }
        return sum
    }

    fun sumOfOtherUserTransactions(serviceUser: AppUser,appUser: AppUser,from: String,to:String):BigDecimal?
    {
        if(serviceUser.userType!=UserType.ServiceEmployee)return null
        var start =from.YYYYMMDDtoParsableZonedDateTime()?:return return null
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?:return null

        var sum = BigDecimal.ZERO
        transactions.filter { transaction -> appUser.userId == transaction.actor.userId  &&  transaction.time >= start && transaction.time <= end  }.forEach {
            sum+=it.amount
        }
        return sum
    }

}