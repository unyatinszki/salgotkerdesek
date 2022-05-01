# 1.) Tranzakciókezelés JMS üzenetfogadás esetén:

Egy JMS alapú rendszer üzeneteket küld egy Spring Boot applikációnak. A cél az üzenetvesztésmentes üzenetfogadás. A Spring Boot applikáció a megkapott üzenetet feldolgozza és az eredményt egy relációs adatbázisba menti.

 

Milyen lehetséges architektúrális megoldások léteznek a tranzakció kezelésére? (Itt most csak a tranzakciókezelés a lényeges)

Milyen előnyei és hátrányai vannak ezeknek a megoldásoknak?

## Answer
In order to achieve message loss free end to end delivery, multiple factors need to be considered:
1) the JMS broker itself should not loose messages in case of a broker failure: in general this can be achieved by specifying perisstent mode on the sender side. (from the application's perspective pretty much this is the most that can be done. Ensuring that this is indeed "enough" is more an infrastructural question that can be guaranteed e.g. at the level of SAN/NAS storage used via using sufficient level of redundancy and backups (still - no 100% level of guarantee exists and the costs are increasing rapidly when trying to get close to is))
2) the receiver app must follow one of the folowing strategies - all of these can be implemented either at the level of the basic API-s, using spring abstractions like the JmsTemplate and JdbcTempalte or anything higher, or using a more specialised framework like Spring Integration or Camel also from Spring boot (for this particular use case most likely i would go with camel as it is especially good for such use cases):
### Auto Acknowledge with a message listener:
In this relative simple scenario because only 2 transactional resources are being used, it can be an option to use AUTO_ACKNOWLEDGE BUT strictly with a message listener. This can work, because in a message listener the message consumption will be acknowledge back to the broker only when the message listener completes without an exception. If writing into the DB happens in this message listener, we just need to ensure that in case of any DB write failure we let an exception be thrown out from the listener. This will trigger a NACK towards the broker that will then in return redeliver. 2 important notes though:
1) the broker will redeliver only up to a predetermined redelivery count. To handle this a dead letter queue can be specified on the message onto which the messages reaching max redelivery will be put by the broker. Consuming from this queue is like consuming from any other separate queue
2) based on the JMS spec a well behaved message listener should not throw - the above mechanism is there to be a safety net but not as a primary mechanism e.g. to do retries at the app level. In our case, because DB errors are indeed unexpected problems this can be still ok but in general the broker can mark the session as malfunctioning and e..g not delivering more messages to it if there are mulitple "healthy" sessions consuming from the same queue which might result in silently loosins app instances.

E.g. Camel will implement this differently: error handling with retries will happen at the Camel level and from the brokers perspective there will be an error only if doing the Camel level redelivery or putting into the error queeu fails (in which case all the above will still happen at the broker level). This can be a really lighweight and still safe solution if e.g. there are transiend DB errors and reatrying the message after some delay can indeed help

### Client acknowledge:
In contrast to this, upon receiving a message via any means, if the session was opened with client acknowledge, the messages won't be ACK-d towards the broker until an explicite message.acknowledge() is called. This gives the client full control in terms of when to send back an ACK or a NACK depending on the DB side outcomes (e.g. handling SQLExceltions on the DB side).  If a message is neither ACK-d nor NACK-d for a predefined time then the broker will consider a session failure and redeliver (and it might also invalidate the session considered to be slow now) However this can in extreme cases cause e.g. memory issues on the broker side (e.g. an application side bug where messages are systematically neither ACK-d nor NACK-d in certain cases) which makes this very manual appraoch more error prone.

### Using transacted sessions with a distributed transaction amanger
The most heavy weighted approach is to use a distributed transaction manager like Atomikos, use XA compliant resources like ActiveMQ as a JMS impl and PostgreSQL as a DB and operate with transacted sessions on the JMS side and transactional connetions on the DB side. In spring this can be achieved by using @Transactional annotations and setting e.g. Atomikos as the transaction manager. Because both the JMS and the JDBC resource will participate in the same transaction, the 2 phase commit protocol will ensure that the JMS side commit will only happen if the JDBC side commit was successful.

The problem with this approach is that it requires all XA aware resources - while many widely used components like RabbitMQ or MongoDB are not. It is also harder to configure and most iportantly in case of a probelm much harder to debug. Plus it is shown that the 2 phase commit protocol is not bullet proof. It can happen that a resource will report OK in the first commit phase but then still fail in the second phase. The fact that these cases are being cnsidered as bugs on the XA aware component doesn't help much - still we will end up having a partially commited transaction.




 

# 2.) REST alapú rendszerintegráció :

Egy belső Spring Boot applikáció személyek adatait dolgozza fel és tárolja egy relációs adatbázisban. Ezen személyek adatait egy külső rendszer is tárolja, ami REST API-n keresztül érhető el. A cél a két rendszer integrációja és az adatok szinkronban tartása.

 

A belső Spring Boot applikációba beérkezik egy kérés egy személy adatainak törlésére.

 

Milyen technikai problémákat és nehézségeket vet fel ez a feladat? Mire kell a fejlesztésnél figyelni?

 

Hogyan fejlesztenéd le ezt a felhasználási esetet? Milyen lépések és hívások hajtódnak végre a törlés alatt?

 

# 3.) Adatbázislekérdezés optimalizálás:

Egy olvasásintenzív Spring Boot alapú applikáció relációs adatbázisban tárolja az adatok. Az adatok a következők:

 

Táblák:

Folder, Item, Property, Label

 

Kapcsolatok:

Folder 1 ... n Item

Item 1 ... n Property

Label 0-n ... 0-m Item

 

a.) Az applikáció egyik REST végpontja lekéri az adatokat a Folder, Item és Property táblákból join segítségével. A produkcióban kiderül hogy a felhasznált query lassú több millió item esetén.

 

Hogyan azonosítanád a problémát ?

Milyen lehetséges megoldásokat próbálnál ki a query felgyorsítására?

 

b.) Hogyan modelleznéd le JPA (vagy Hibernate) annotációk segítségével a következő kapcsolatot:

Label 0-n ... 0-m Item

Mire kell figyelni?

Programozási feladat:

 

1.) Készíts egy REST alapú Spring Boot applikációt, ami egy "item" listát fogad, kiszámítja a SHA-256 kódját a fogadott "item" -ek password mezőjének és az Item-et lementi az adatbázisba. Minden egyes Item külön szálon kerül feldolgozásra és lementésre. A ThreadPool mérete konfigurálható, alapbeállítás 10 szál. Abban az esetben ha több Item van a Request -ben mint a ThreadPool méretete, akkor a következő item akkor kerül feldolgozásra, ha felszabadul egy szál.

 

A hívás akkor tér vissza és a Response akkor küldődik vissza ha az összes item feldolgozásra és lementésre került. Response tartalmazza az item ID -t (primary key) és a password hash -t.

 

X-Tracking-Id: Loggolásra kerül minden egyes fontos lépésnél. A külön szálon futó Item feldolgozásnál is loggolásra kerül.

 

Végpont:

POST /items

Request Header:

X-Tracking-Id: Ez arra szolgál hogy a hívást nyomon lehessen követni több szál esetén is. A kliens adja meg ezt az ID -t ami egy UUID.

 

Request body:

{ "items": [{

"name": "<NAME-1>,

"password": "PASSWORD-1"

},

...

,{

"name": "<NAME-n>,

"password": "PASSWORD-n"

}]

}

 

Response Header:

X-Tracking-Id: A Request-ben szereplő ID.

 

Response Body:

{ "items": [{

"id": "GENERATED UUID-1",

"name": "<NAME-1>,

"passwordHash": "SHA-256-PASSWORD-HASH-1"

},

...

,{

"name": "<NAME-n>,

"passwordHash": "SHA-256-PASSWORD-HASH-n"

}]

}
