This is a commandline tool to restore encrypted key backups with the same tools the Android wallet would.
For example with the encrypted private key

> xEncGXICZE1_eVYfGWDioNu_8hA6RZzep4XqwPGRtcKb01MDg3s1XFntJYI9Dw
 
and its password
 
> QDTDXOYFBXBKKMKR

running

```
./gradlew :backuputil:run --args "xEncGXICZE1_eVYfGWDioNu_8hA6RZzep4XqwPGRtcKb01MDg3s1XFntJYI9Dw QDTDXOYFBXBKKMKR"
```

yields

```
Private key (Wallet Import Format): cRS3zDecX6c8UF9mtmh5vkB8CQ4nCNn1bjPQayXpt3fSLwSPi1LF
                   Bitcoin Address: n4J5FqC89EnV8hikctDs6njmG2cwxS8cM5
                   Bitcoin Address: 2NBzQZLt2MQkJpYGp66b2GQfR5BAQAHtXoU
                   Bitcoin Address: tb1ql8d5hlsgee3qaes32tnqljr4394wes5cnq8yrd
```
