package com.example.agidospring

import Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime

class AppUser() {

    var userId= ""
    var name = "###Username###"
    var userType = UserType.NonAssigned
    private var balance = BigDecimal.ZERO
    private var balanceLastUpdated = ZonedDateTime.now()
    var specialStatus = SpecialStatus.None

fun print():String
{
    var ret="<table>"

    mutableMapOf<String,String>().apply {
        this["id"]= userId.toString()
        this["name"]= name
        this["type"]= userType.name
        this["special status"] = specialStatus.name
    }.forEach{k,v->
        ret+="<tr><td>$k</td><td>$v</tr>"
    }

    return "$ret</table>"
}

    fun addBalance(amount:BigDecimal)
    {
        balance+=amount
        balanceLastUpdated = ZonedDateTime.now()
    }


    fun getBalance(): BigDecimal {
        return balance
    }

    fun getLastUpdateTime(): ZonedDateTime {
        return balanceLastUpdated
    }

    fun isSameUserAs(user: AppUser):Boolean
    {
        return (this.userId==user.userId) &&(name==user.name)
    }
    fun isAllowedToWithdraw(): Boolean {
        return this.userType == UserType.Customer && specialStatus != SpecialStatus.Banned
    }

    fun isAllowedToPermitWithdrawals(): Boolean {
        return this.userType == UserType.ServiceEmployee
    }

    fun isAllowedToMakeDisposits(): Boolean {
        return this.userType == UserType.Customer && specialStatus != SpecialStatus.Banned
    }
    fun calcBalance(transactions: MutableList<Transaction>) {
        var bal = BigDecimal.ZERO
        transactions.filter { it.actor.userId == this.userId }.forEach {

            if (it.status != TransactionStatus.Finished) return@forEach
            if (it.transactionType == TransactionType.Deposit) bal += it.amount
            if (it.transactionType == TransactionType.Withdrawal) bal -= it.amount
        }
        this.addBalance(bal)
    }

    fun permitWithdrawal( transactionId: String, transactionUser: AppUser) {         //Service only
        if (!this.isAllowedToPermitWithdrawals()) return
       // this.permitTransaction(transactionId, transactionUser)

    }


    fun permitTransaction(transaction: Transaction, transactionUser: AppUser) {
        //transactionId wäre im Realsystem distinct
        transaction.apply { permissionGranted }
       /*
        var transaction = transactions.first {
            !it.permissionGranted && (it.transactionId == id) && transactionUser.isSameUserAs(it.actor)
        }.apply {
            permissionGranted = true
        }

        */
                /*
        val response = transaction.sendMoneyToCustomerAccount()
        transactionUser.addBalance(-transaction.amount)
        transaction.apply { status = if (response.successful) TransactionStatus.Finished else TransactionStatus.Failed }
  */
    }

}

enum class UserType {
    NonAssigned,
    ServiceEmployee,
    Customer
}

enum class SpecialStatus{
    None,
    Banned
}