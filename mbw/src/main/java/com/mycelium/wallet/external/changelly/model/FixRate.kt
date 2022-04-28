package com.mycelium.wallet.external.changelly.model


//"id": "f4dd43106d63b65b88955a0b362645ce960987c7ffb7a8480dd32e799431177f",
//"result": "0.02556948",
//"from": "eth",
//"to": "btc",
//"maxFrom": "50.000000000000000000",
//"maxTo": "1.27847400",
//"minFrom": "0.148414210000000000",
//"minTo": "0.00379488"

class FixRate(val id: String,
              val result: Double,
              val from: String,
              val to: String,
              val maxFrom: Double,
              val maxTo: Double,
              val minFrom: Double,
              val minTo: Double)