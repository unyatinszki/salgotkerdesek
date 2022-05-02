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

## Answer
The above scenario raises the concern of split ownership on data - ussually considered as a hard to solve problem, if not an antipattern. When solving this we will ussually face the cconsequences of the CAP theroem: if we want to be totally consistent then we will risk availability (if the 2 services can't access eachother temporarily then i can't accept changes if i must ensure that no inconsistencies can happen) while the opposite is also true: if i want to be always available then i must accept that there might be at least eventual inconsistencies worst case eliminating inconsistencies might reuqire human interaction.

### If we can introduce observable events...
In the question only rest API-s are being mentioned. If this can be extended with making the events apearing on both side available to the other side then that can already simplify the situation: e.g. having a Kafka or any JMS topic where changes made on each side are vitibaile, then the other side can alter its data representation accordingly. This could be exposed via Web aware interfaces as well e.g. having websocket/HTTP2 connections between the services could also go through proxies.
This would allow the 2 sides to keep their local data representation up to data with all changes. However there could still be concurrent cases where the same entity would be updated on both sides at the same time on a conflicting way.

To handle this, we could say that one of the services is the actual owner of the data: the other side would accept chang requests only in a "proxy mode": it would forward the change request to the primary side which would then either acept or reject the request. Upon accepting that data change event would be made visible as an event as per the above and the other side could also update its DB. This schema would be very good in terms of consistency but worse in throughput and also availability (see the bove reference to the CAP theroem)

The other option still assuming observable events is to let the 2 services operate autonomously and handle the potentially apearing conflict: e.g. when differnet atributes are being updated we can just take them as is while if the same attribute is updated then we might still define a preference order on the systems or th eusers doing the change plust we might also introduce some human process where such cases would be flagged to the involved users/operators. This solution would be good in terms of throughput and availability but as detailed consistency would need such handling and in some case might be surprising to users.

### If we can not introduce observable events...
If this is all not available, there is just my service and something external e.g. an SAP deploymnet, a social media site or anything else, introducing event is not an option, also then the basic question is the same, the solutions will be just harder. i'll continue more from the perspective of my service as that is what i've control over.

In terms of initializing the DB there could be an initial dump from the external service using its exposed interface. After this, keeping the data in-sync could happen on a scheduled way: a frequency could be determined depending on the affordable network, DB etc load on both sides and my service would repeatedly query the external one for user details to detect changes. If the external service allows the querying of recently changed users then that could enahnce this process a lot. Doing this i would eventually  capture the updates originated on the external side.

In terms of updates on my side we are back to the question of consistency vs availability: if consistency is the priority then before every write i can initiate the same at the external system and see if tha tis possible. If yes then i can make the write on my side. This in itself might result in race conditions just on my side as even on my side a change for the same user might appear concurrently that might cause consistency issues on my side alone. This can be handled wiht solutions like optimistic locking where updates are always considered in the context of a givenversion otherwise they fail. If this is too heavy then also within my own service i might do the same conflict resolution tactics like detailed above between the services.

If trigering an external update in case of every update is considered too heavy or as for CAP i want to have higher availability being able to update locally when the external service is not available then i can take the updates locally and actualize the external system e.g. at the same time when i query the changews on that side effectively exchanging updates and then resolving conflicts. Like in case of assuming events conflicst will need to be handled potentially also requiring human intervention as a last resort.

As for the deletes - in general deletes can be turned into just updates if they are logical deletes. With this data integrity won't be broken as the DB row will still be there, the services can just not alow certain operations if the user is already not active. With this bein just an update (changing an active boolean flag e.g.) everything i wrote above applies including the 2 main options we have to wether always check with the exernal service or not. On the top of this, long inactive users can be eliminated from the DB on a scheduled way (the external service might do the same) - this might entail deleting/long term persisting other content as well (to conform GDPR rules etc...)

If deletes must be physical then it is still similar to an update (e.g. doing optimistic locking etc - not repeating here) we just need to ensure data consistency right there (=no other record should reference the user in the DBs) The main problem is that conflict resoltuion might be harder as nasty conflicts like changing an attribute externally of a locally deleted user might eppear that might entail recreating the user locally. Because of this for physical deletes i would always go with initiating a delete on the external service first and if that was successful then doing the same locally. Deletes are probably rare enough for this to be manageable from the availability perspective.
 

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

## Answer

a) as the exercise stated the query is already using joins, so the typical JPA illness of running too many queries to fetch the needed data can be ruled out (loading a folder with N items would result in N+1 queries without joining). In my solution i achieve this by having LAZY fetch mode for all mapped relations but also having a JPQL query in FolderRepository.loadAllFolders that prescribes doing a join fetch. With this the advantages of lazy loading and batch loading can be combined as needed, depending on the use case.

Knowing this the most likely root cause will be not having sufficient indexes on the child tables. To identify the Items of a Folder, Items will most likely have a folder_id and to identify the Properties of an Item, Properties will most likely have an item_id. Not having an index on these item.folder_id and peroperty.item_id will result in doing full table scans on the item and property tables which can be a performance killer.

In case if the indexes are there, but the performance is still poor then the 2 most likely reasons are:
1) the index does not fit into memory still requiring disc scan. While an index is much smaller than the table this can still be significant if there is a lot of data. Handling this will be engine dependent - e.g. on Sybase indexes are stored in separate memory pools the size of which can be increased if there is enough RAM in the machine (if scaling up is not possible then scaling out is needed)
2) the index needs to be rebuilt (typical Sybase illness - afaik MS Sql has laready solved this)

In general most RDBMS does support generating query plans along with cost estimates. i would use this to see if the indexes are indeed being picked up and what the estimated cost is.

b)

For the annotated classes please refer to the classes under package unyat.salgot.question3.dao
The DB schema can be found under db/changelog/001-question3-schema.sql

In general with N-M relations the usual mistake is making the fetch mode EAGER as that will result in loading entities repeatedly from both sides of the relation potentially loading a huge number of entities. Even worse depending on the data size and DB latency and performance this might remain hidden in lower level envs only being noticed later in the process. This is why by default for N-M relations the fetch mode is LAZY (so it is also for OneToMany).

Also when using @ManyToMany deleting is not trivial: the owner of the relation is the entity NOT having the mappedBy clause. In our case this means deleting an Item entity will result in also deleting the rows in ITEM_LABEL. But deleting a Label needs deleting all the references from Items otherwise data consistency would be violated (and an SQLException would be thrown).

Because of the later, modeling N-M relations as 2 1-M relation making the relation itself an entity can be more beneficial as in that case this relation entity can be deleted on its own right e.g. from JPQL or via cascading deletes from parent entities to the relation entity.


# Programozási feladat:

 

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

## Answer

The solution can be found under package unyat.salgot.question4

Notes:
- there is a uniqueness check in place for the name -> a constraint violation will fail the entire request (http 400) also deleting the items those would have been successfully persisted on other threads
- the format of writing the hash was not specified - i'm using base64 both to save into the DB as well as to return in JSON
- the main class of the app is unyat.salgot.question4.SalgotApplication
- using liquibase to spin up the schema and H2 for the purposes of the exercise
- no authentication and authorization as that was not specified
- the thread pool size can be configured with item.service.thread.pool.size
- for the purposes of the exercise i'm logging at INFO level (could be reconsidered for real prod usage)
- to test via curl:
Will create 2 items:
curl -v -X POST -H 'X-Tracking-Id: 456dfghfdgh88' -H 'Content-Type: application/json' -d '{"items":[{"name": "item1", "password": "password123"},{"name": "item2", "password": "password456"}]}' localhost:8080/items

Will dump the 2 items:
curl -X GET localhost:8080/items

Using the same run running this will fail because of the duplicate item. The non-duplicate item will also be deleted:
curl -v -X POST -H 'X-Tracking-Id: 456dfghfdgh88' -H 'Content-Type: application/json' -d '{"items":[{"name": "item1", "password": "password123"},{"name": "non-duplicate-item", "password": "password456"}]}' localhost:8080/items

So this will dump the previous 2 items:
curl -X GET localhost:8080/items