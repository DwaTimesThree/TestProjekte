package com.example.agidospring.service

import Transaction
import TransactionType
import com.example.agidospring.AppUser
import com.example.agidospring.Controller.StandardResponse
import com.example.agidospring.UserType
import com.example.agidospring.enum.DBType
import com.example.agidospring.enum.ErrorMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TransactionService {
    @Autowired
    lateinit var userService: UserService

    val zonedDateTimeAdd = "T00:00:00+01:00"

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
            status =
                if (appUser.getBalance() < amount) TransactionStatus.Failed else TransactionStatus.PermissionPending
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


    fun MutableList<Transaction>.checkValidityOfWithdrawalRequests(): MutableList<Transaction> {
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
        return transactions.filter { transaction ->
            !transaction.permissionGranted && transaction.status == TransactionStatus.PermissionPending && transaction.actor.isSameUserAs(
                user
            )
        }.toMutableList().checkValidityOfWithdrawalRequests()

    }

    fun showAllPendingWithdrawals(user: AppUser): MutableList<Transaction>? {
        if (user.userType != UserType.ServiceEmployee) return null
        return transactions.filter { transaction -> !transaction.permissionGranted && transaction.status == TransactionStatus.PermissionPending }
            .toMutableList().checkValidityOfWithdrawalRequests()

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


    fun String.YYYYMMDDtoParsableZonedDateTime(): ZonedDateTime? {
        return ZonedDateTime.parse(this + zonedDateTimeAdd, DateTimeFormatter.ISO_ZONED_DATE_TIME) ?: null
    }

    fun getAllTransactionsOf(user: AppUser, appUser: AppUser, from: String, to: String): MutableList<Transaction>? {
        if (user.userType != UserType.ServiceEmployee) return null
        var start = from.YYYYMMDDtoParsableZonedDateTime() ?: return null
        var end = to.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?: return null
        return transactions.filter { transaction ->
            transaction.fromToUserFilter(appUser.userId, start, end)
        }
            .toMutableList()
    }



    fun showMyTransactions(user: AppUser): MutableList<Transaction> {
        return transactions.filter { transaction -> transaction.actor.isSameUserAs(user) }.asReversed().toMutableList()
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




    fun Transaction.fromToUserFilter(
        userId: String? = null,
        from: ZonedDateTime? = null,
        to: ZonedDateTime? = null
    ): Boolean {
        userId ?: from ?: to?.let { return time <= to } ?: return true
        userId ?: to ?: from?.let { return time >= from }
        userId ?: return time <= to && time >= from
        from ?: to?.let { return time <= to && userId == actor.userId } ?: return userId == actor.userId
        to ?: from?.let { return time >= from && userId == actor.userId } ?: return userId == actor.userId
        return time >= from && userId == actor.userId && time <= to
    }

    fun transactionsFromUserFromTo(
        appUser: AppUser,
        from: String? = null,
        to: String? = null
    ): MutableList<Transaction>? {
        return transactionsFromUserFromTo(appUser.userId, from, to)
    }

    fun transactionsFromUserFromTo(userId: String? = null, from: String?, to: String?): MutableList<Transaction>? {
        var start: ZonedDateTime? = null
        var end: ZonedDateTime? = null
        var fromx= if(!from.isNullOrEmpty())from else null
        var tox= if(!to.isNullOrEmpty())to else null
        fromx?.let { start = it.YYYYMMDDtoParsableZonedDateTime() ?: return null }
        tox?.let { end = it.YYYYMMDDtoParsableZonedDateTime()?.plusDays(1) ?: return null }
        return transactionsFromUserFromTo(userId, start, end)
    }

    fun transactionsFromUserFromTo(
        userId: String? = null,
        from: ZonedDateTime? = null,
        to: ZonedDateTime? = null
    ): MutableList<Transaction>? {
        return transactions.filter { transaction -> transaction.fromToUserFilter(userId, from, to) }.toMutableList()
    }


    fun getTransactionsMetaFunction(
        user: User,
        ofWhomId: String?,
        from: String?,
        to: String?,
        sum: Boolean? = false
    ): MutableList<Transaction>? {
        var requester = userService.identify(user) ?: return null
        //check Rights
        if (requester.userType == UserType.Customer && (ofWhomId != requester.userId && ofWhomId != null)) return null
        if (requester.userType == UserType.Customer) {
            return transactionsFromUserFromTo(requester.userId, from, to)
        }
        //determine Whose
        if (requester.userType == UserType.ServiceEmployee) {
            var whom: AppUser? = null
            ofWhomId?.let {whom = userService.getAppUserById(it)?: return null } ?: return transactionsFromUserFromTo(
                null,
                from,
                to
            )
            return transactionsFromUserFromTo(ofWhomId, from, to)
        }
        return null
    }


}



