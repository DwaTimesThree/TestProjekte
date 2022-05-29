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
import java.net.URLEncoder


@RestController
class WebController {

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
    fun registerServiceEmployee(
        @AuthenticationPrincipal user: User,
        @RequestParam("name") username: String,
        @RequestParam("password") password: String
    ): String {
        user.authorities.find { it.authority == "ROLE_ADMIN" }
            ?: return ErrorMessage.NoRights.send() + user.authorities.joinToString { it.authority + "<br>" }
        return htmlWrapper.registerServiceEmployee(username, password, UserType.ServiceEmployee)
    }


    @RequestMapping("\${request.customer.deposit}")
    fun deposit(
        @AuthenticationPrincipal user: User,
        @RequestParam("amount") amount: BigDecimal
    ): String {
        return htmlWrapper.deposit(user, amount)
    }

    @RequestMapping("\${request.customer.withdraw}")
    fun requestWithdrawal(
        @AuthenticationPrincipal user: User,
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
    fun authorizeWithdrawal(
        @AuthenticationPrincipal user: User,
        @RequestParam("transactionId") transactionId: String
    )
            : String {
        return htmlWrapper.authorizeWithdrawal(user, transactionId)

    }

    @RequestMapping("\${request.serviceemployee.show.richest}")
    fun showRichestCustomers(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.sortAllCustomersByAmountOfMoney(user)
    }

    @RequestMapping("\${request.show.transactions}")
    fun showTransactionsFromTo(
        @AuthenticationPrincipal user: User,
        @RequestParam("userId", required = false) userId: String?,
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?
    ): String {
        return htmlWrapper.showTransactionsMeta(user, userId, from, to)
    }

    @RequestMapping()
    fun index(@AuthenticationPrincipal user: User?): String {
        return htmlWrapper.welcomePage(user)

    }

    @RequestMapping("\${request.show.transactionsum}")
    fun showTransactionSumOf(
        @AuthenticationPrincipal user: User,
        @RequestParam("userId", required = false) userId: String?,
        @RequestParam("from", required = false) from: String?, @RequestParam("to", required = false) to: String?
    ): String {
        return htmlWrapper.showTransactionSumMeta(user, userId, from, to)
    }

}

class DepositResponse {
    var successful = true
}

class WithdrawalResponse {
    var successful = true
}

class StandardResponse(val message: String)
