package com.mycelium.testhelper

object SignatureTestVectors {
    private val explictSpace = " " // android studio kills trailing spaces even in multiline strings!!
    val bitcoinMessageTestVectors = arrayOf(
            TV("multiline 1",
                    """
                    I am the owner of Instagram account "bitcoin".
                    The email it was registered with is prostoosloshnom@gmail.com
                    Account was hacked and deactivated,  I have no access to it.
                    Please restore access to my account  with the private key it associated with.$explictSpace
                    This message is signed with this private key.
                    Thanks.$explictSpace""".trimIndent(),
                    "18Sn6Vs65xCJRGshBoiaYX9mmmzXxnkoFq",
                    "IMsV/oKkUwS5XPhQIjPY7X0tzd9NL/nUt8UaE8EUZi54SDNyrAk0ogzH8YsYzSkNsuujPrjtTBhXjO66fMA5374="),
            TV("multiline 2",
                    """
                    Hello.
                    Ok""".trimIndent(),
                    "1NdeBBoqx8nyhRif3jULyLhECrpVersogN",
                    "H1KinN1aIeXJpRyqUcwNovOkE1rVPE5ZELqbVrooBQZOamT/sUmciq0wGIcSJRj4SUIz9fFAFlFWmPvO5QAnbEM="),
            TV("single line",
                    "thisisatest",
                    "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                    "IB066t2s+CpjlCJx00oERatuz/jaQRDvms9zx9kH2DW2dvUVoJsSIKCrxI419mJtpvyZoqm5eLNTbtBQWmqM324="),
            TV("space newline space",
                    " \n ",
                    "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                    "H2Kv4LAl5NARmpQ6EWY7vnT18fI8M84QK2sD54PaqYdOBVFaF6PnxfZaLyDF2akuOsP+KpebW4OVOGL8VU0daKw="),
            TV("long line",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                    "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                    "H36ukDLATPxOUNizAkABOUvRoMqvWfd8FYWnRVH46Y9lSPvvrjYqhFGx2KWshOBICsOk6Qn+CmRoE79Sfk0E9xw="),
            TV("regexp",
                    "..?.*.+[A-Z]+123[\\^abc][a-c&&[\\^b-c]]",
                    "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                    "IME5kO4S7rFZabmV2/7ZlJqLt/BVWmjXel2gOtHubaVWMVyuaND9xkxtlBvUN7XAjdHShavbZ2d1OtmRNmfN+ug="),
            TV("hello",
                    "Hello",
                    "mg4u71KgaMToQ1GnV7eXkRifrdv9moNqoA",
                    "IP1nHlu95JS4VYV92diIh+SYONiNbVlpUdtcZuaNQEL6RKQ5f9DnTj63+BBH+/rIQx6H4Fcc3sG+AHdUW7ym+mE="),
            TV("empty string",
                    "",
                    "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                    "HyVnOv6mTYO2BoCynsOzBNA1VSPStnKj9rhS2VjzO6anRn6ahLjCZwcoHxTgGJNz+KBnzgNNlbeGovWnmcWfbww="),
            TV("Spanish",
                    "¡Hola!",
                    "1Lz6d5eDz89jkH8yZi2rW36sauZxt5qTWz",
                    "H5e+ZVuyVTXIjjkTb4JBaUGCfn0VOO0Not1XZuJpYnNSB+M1hiSUfD9zmKDhF6tHnzTkO5Plvox1BP04zo4d2d0=")
    )
    data class TV(val name: String, val message: String, val address: String, val signature: String)
}