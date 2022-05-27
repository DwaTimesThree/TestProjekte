package com.example.agidospring.service

import com.example.agidospring.AppUser
import com.example.agidospring.Sha
import com.example.agidospring.UserType
import com.example.agidospring.getUserDetailsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service


@Service
class UserService {
    var userList = mutableListOf<AppUser>()

    fun createId(user:User): String {
        return createId(user.username)
    }

    fun createId(username: String): String {
        var salt = "5xD1"
        return Sha().calculateSH256("$salt$username:")
    }

    private fun addAppUser(appUser:AppUser)
    {
        userList.add(appUser)
    }

    fun getAppUserById(id:String):AppUser?
    { return userList.find { it.userId == id }}


    private fun addAuthUser(username: String, password: String, vararg roles:String)
    {
        getUserDetailsManager.createUser(User.withDefaultPasswordEncoder().username(username).password(password).roles(*roles).build())

    }
    private fun createAndAddAppUser(username: String, password: String, userTypeX: UserType):AppUser
    {
        var user = AppUser().apply {
            name = username
            userId = createId(username)
            //  createId(username)
            userType = userTypeX
        }
        addAppUser(user)
        return user
    }

    fun sortAllCustomersByAmountOfMoney(requester:AppUser): MutableList<AppUser>? {
        if (requester.userType!=UserType.ServiceEmployee)  return null
        userList.toMutableList().run {
            sortByDescending { it.getBalance() }
            return this
        }
    }


    fun registerNewUser(username: String,password: String,userType: UserType):AppUser
    {
        addAuthUser(username,password,userType.name)
        return createAndAddAppUser(username,password,userType)

    }
}