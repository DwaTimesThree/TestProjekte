package com.example.agidospring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.provisioning.InMemoryUserDetailsManager


@SpringBootApplication
class AgidoSpringApplication


@EnableWebSecurity
class KotlinSecurityConfiguration : WebSecurityConfigurerAdapter() {

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(getUserDetailsManager)

    }


    override fun configure(http: HttpSecurity) {
        http.formLogin().loginProcessingUrl("/LoadTestData").and().authorizeRequests().anyRequest().permitAll().and().logout()

    }
}


@get:Bean
var getUserDetailsManager = InMemoryUserDetailsManager()

fun newUser(user: String, pw: String, vararg roles: String) =
        User.withDefaultPasswordEncoder().username(user).password(pw).roles(*roles).build()

fun main(args: Array<String>) {
    runApplication<AgidoSpringApplication>(*args)
    with(getUserDetailsManager) {
       createUser(newUser("abc", "123", "ADMIN"))


    }
}
