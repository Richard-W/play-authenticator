play-authenticator
==================

This module provides user management and authentication to play 2.4+. It saves
the principals using a MongoDB database. The passwords are hashed using PBKDF2
with SHA256.

To use this module in a play project you have to add the following lines to your
conf/application.conf:

```scala
play.modules.enabled += "play.modules.reactivemongo.ReactiveMongoModule"
play.modules.enabled += "play.modules.authenticator.AuthenticatorModule"
mongo.uri = "mongodb://localhost/somedb"
```

After this you may inject `Authenticator` instances into your controllers.
