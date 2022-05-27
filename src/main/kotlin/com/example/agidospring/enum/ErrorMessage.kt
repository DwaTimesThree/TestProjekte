package com.example.agidospring.enum

enum class ErrorMessage(val message:String) {
    NoSuchUser("Kein User mit den angegeben Werten in DB gefunden."),
    NoRights("User verfügt nicht über die erforderlichen Rechte."),
    ParseErrorYYYY_MM_DD("Wert entspricht nicht dem geforderten Pattern: YYYY-MM-DD"),
    NoRightsToDeposit("Nur Kunden dürfen Einzahlungen vornehmen. Ein gebannter Kunde darf auch keine Einzahlungen vornehmen."),
    NoRightsToWithdraw("Nur Kunden dürfen Auszahlungen vornehmen. Ein gebannter Kunde darf auch keine Auszahlungen vornehmen."),
    NoPendingWithdrawalsFound("Es wurden keine ausstehenden bzw. noch nicht genehmigten Auszahlungen gefunden."),
    NoSuchTransactionFound("Keine Transaktion mit der angegebenen ID gefunden."),

    ;


    fun send(string: String):String
    {
        return "$message<br>Ihr Wert: $string"
    }

    fun send():String
    {return message}
}