package com.example.agidospring.Controller

import com.example.agidospring.UserType
import com.example.agidospring.enum.ErrorMessage
import com.example.agidospring.service.HTMLWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal


@RestController
class WebController  {

    @Autowired
    lateinit var htmlWrapper: HTMLWrapper

    @RequestMapping("\${request.admin.load.testdata.url}")
    fun loadTestData(): String {
        return htmlWrapper.loadTestData()
    }

    @RequestMapping("\${request.all.register.customer.url}")
    fun register(@RequestParam("name") username: String, @RequestParam("password") password: String): String {

        return htmlWrapper.registerCustomer(username, password)
    }

    @RequestMapping("\${request.admin.register.serviceemployee.url}")
    fun registerServiceEmployee(@AuthenticationPrincipal user: User,@RequestParam("name") username: String, @RequestParam("password") password: String): String {
        user.authorities.find { it.authority=="ROLE_ADMIN" }?:return ErrorMessage.NoRights.send()+user.authorities.joinToString { it.authority+"<br>" }
        return htmlWrapper.registerServiceEmployee(username,password, UserType.ServiceEmployee)
    }


    @RequestMapping("\${request.customer.deposit}")
    fun deposit(
            @AuthenticationPrincipal user: User,
            @RequestParam("amount") amount: BigDecimal
    ): String {
        return htmlWrapper.deposit(user, amount)
    }

    @RequestMapping("\${request.customer.withdraw}")
    fun requestWithdrawal(@AuthenticationPrincipal user: User,
                          @RequestParam("amount") amount: BigDecimal
    ): String {
        return htmlWrapper.requestWithdrawal(user, amount)
    }

    @RequestMapping("\${request.customer.show.pendingwithdrawals}")
    fun showMyPendingWithdrawals(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.showMyPendingWithdrawals(user)
    }
    @RequestMapping("\${request.serviceemployee.show.pendingwithdrawals}")
    fun showPendingWithdrawals(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.showPendingWithdrawals(user)
    }

    @RequestMapping("\${request.customer.authorize.withdrawal}")
    fun authorizeWithdrawal(@AuthenticationPrincipal user: User,
                            @RequestParam("transactionId") transactionId: String)
            : String {
        return htmlWrapper.authorizeWithdrawal(user, transactionId)

    }

    @RequestMapping("\${request.serviceemployee.show.richest}")
    fun showRichestCustomers(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.sortAllCustomersByAmountOfMoney(user)
    }

    @RequestMapping("\${request.customer.show.transactions}")
    fun showMyTransactions(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.showMyTransactions(user)
    }

    @RequestMapping("\${request.customer.show.transactions.fromto}")
    fun showMyTransactionsFromTo(@AuthenticationPrincipal user: User, @RequestParam("from") from: String, @RequestParam("to") to: String): String {
        return htmlWrapper.showMyTransactions(user, from, to)
    }
    @RequestMapping("\${request.serviceemployee.show.transactions.fromUser.byId}")
    fun showTransactions(@AuthenticationPrincipal user: User,@RequestParam("userId") userId :String,): String {
        return htmlWrapper.showTransactionsOf(user,userId)
    }

    @RequestMapping("\${request.serviceemployee.show.transactions.fromUser.byId.fromto}")
    fun showTransactionsFromTo(@AuthenticationPrincipal user: User,@RequestParam("userId") userId :String, @RequestParam("from") from: String, @RequestParam("to") to: String): String {
        return htmlWrapper.showTransactionsOf(user,userId, from, to)
    }

    @RequestMapping()
    fun index(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.welcomePage(user)

    }

    @RequestMapping("\${request.customer.show.transactionsum.fromto}")
    fun showMyTransactionSumFromTo(
            @AuthenticationPrincipal user: User,
            @RequestParam("from") from: String, @RequestParam("to") to: String,
    ): String {
        return htmlWrapper.showMyTransactionSum(user, from, to)
    }
    @RequestMapping("\${request.customer.show.transactionsum}")
    fun showMyTransactionSumFromTo(
            @AuthenticationPrincipal user: User,
    ): String {
        return htmlWrapper.showMyTransactionSum(user)
    }
    @RequestMapping("\${request.serviceemployee.show.transactionsum.ofAll}")
    fun showTransactionSumOfAllUsers(
            @AuthenticationPrincipal user: User,
    ): String {
        return htmlWrapper.showTransactionSumOfAllUsers(user)
    }
    @RequestMapping("\${request.serviceemployee.show.transactionsum.ofAll.fromto}")
    fun showTransactionSumOfAllUsersFromTo(
            @AuthenticationPrincipal user: User,

            @RequestParam("from") from: String, @RequestParam("to") to: String,
    ): String {
        return htmlWrapper.showTransactionSumOfAllUsers(user,from,to)
    }

    @RequestMapping("\${request.serviceemployee.show.transactionsum.ofUser.fromto}")

    fun showTransactionSumOf(  @AuthenticationPrincipal user: User,
@RequestParam("userId") userId:String,
                               @RequestParam("from") from: String, @RequestParam("to") to: String,

     ):String
    {
      return   htmlWrapper.showTransactionSumOf(user,userId,from,to)
    }
    @RequestMapping("\${request.serviceemployee.show.transactionsum.ofUser}")

    fun showTransactionSumOf(  @AuthenticationPrincipal user: User,
                               @RequestParam("userId") userId:String,

                               ):String
    {
        return   htmlWrapper.showTransactionSumOf(user,userId)
    }


}

class DepositResponse {
    var successful = true
}

class WithdrawalResponse {
    var successful = true
}

class StandardResponse(val message: String)
