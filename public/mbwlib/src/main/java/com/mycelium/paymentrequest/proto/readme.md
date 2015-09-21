Building the protobuffer
===

(only needed if there are changes in the paymentrequest.proto)

Square Wire
===

get the compiler

    cd public/mbwlib/src/main/java/com/mycelium/paymentrequests/proto
    wget -o wire-compiler.jar "https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-compiler&v=LATEST&c=jar-with-dependencies"

    java -jar wire-compiler.jar --proto_path=. --java_out=../../../../ paymentrequest.proto


