package com.example.agidospring.service

import Transaction
import com.example.agidospring.AppUser
import com.example.agidospring.Controller.DepositResponse
import com.example.agidospring.Controller.WithdrawalResponse
import org.springframework.stereotype.Service

@Service
class ExternalServices {


    fun startExternalDepositProcess(appUser: AppUser): DepositResponse {
        return DepositResponse()
    }

    fun sendMoneyToCustomerAccount(transaction:Transaction): WithdrawalResponse {
        return WithdrawalResponse()
    }

}