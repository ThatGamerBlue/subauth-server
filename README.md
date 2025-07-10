# SubAuth WebServer Component
Prerequisites
---
You'll need Java version 21 to build this project, and a Postgres database to run it

Building
---
Simply run `./gradlew bootJar`, and the built server .jar should appear in `build/libs/` as `subauth-server-*version*.jar`

Setup
---
Set up an application on [Twitch Dev](https://dev.twitch.tv), set the OAuth Redirect to the host where this app with be available, with the path `/twitch/oauth_callback`. Example: `http://localhost:8080/twitch/oauth_callback`  
Note that Twitch requires https:// URLs if you are not using localhost for testing, so if you wish to test on a live server, it needs to be behind a reverse proxy that supplies SSL support.

Copy `env.example.json` to `env.json`, and fill out the variables as follows:  
- `DATABASE_URL`: JDBC link to a Postgres database, such as `jdbc:postgresql://localhost:5432/subauth` where `subauth` is a database name that already exists  
- `DATABASE_USER`: Username of your Postgres user that has access to the given DB name  
- `DATABASE_PASSWORD`: Password for the Postgres user  
- `TWITCH_CLIENT_ID`: Client ID of the Twitch application used for linking Minecraft accounts to Twitch accounts  
- `TWITCH_CLIENT_SECRET`: Client secret of the Twitch application  
- `TWITCH_OAUTH_REDIRECT`: The redirect URL you supplied to Twitch earlier
- `JWT_VERIFY_SECRET`: 64 bytes of cryptographically secure random data, encoded as hexadecimal. Used to supply and verify JSON Web Tokens.  
- `TOKEN_PSK`: A pre-shared key, shared between this application and the plugin running on the authorization Minecraft server. Can be any string, should be treated like any password. Make it long and secure.

Running
---
This project is designed to run in a docker container, such as [this one](https://github.com/ThatGamerBlue/subauth-docker), but can be run standalone with `java -jar`.  
Make sure your firewall rules block direct access to the service, and ensure it's only available via your reverse proxy supplying the SSL encryption.  
The server will start on port 8080.

Routes
---
- /twitch - twitch-related routes
- - /generate_state_token - generates the state token for a user's trip through the twitch api
- - /generate_subscribe_token - generates the subscription token to get whitelist updates
- - /oauth_callback - stores the users twitch auth token to check their subscribers, and to store their minecraft uuid
- - /get_user_info - gets basic info for a user
- - /unlink - unlinks a user's twitch account
- /ws - the websocket server
- /backendws - websocket server used to tell the minecraft server that a link has been established