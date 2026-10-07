=== Sam Updater App API ===
Requires at least: 6.0
Requires PHP: 7.4
Stable tag: 1.0.0
License: GPL-2.0-or-later

Read-only JSON catalog for the Sam Updater Android app.

== Description ==

Adds one public endpoint:

GET /wp-json/samupdater-app/v1/catalog

It returns the data the app can't get from Samsung's servers: how many OS upgrades each Galaxy model gets, its security update tier, One UI beta programs, upcoming major updates and a CSC list for the device picker.

You edit it under Settings > Sam Updater App. Every save is validated first, so a typo can't break the app. If anything is wrong you get a list of the problems and your edits are kept.

Firmware lookups still go through the existing samupdater_live_device_info ajax action. This plugin doesn't touch it.

== Installation ==

1. Zip the samupdater-app-api folder (leave out the tests folder) and upload it under Plugins > Add New > Upload Plugin.
2. Activate it. The catalog is seeded from data/catalog-seed.json, which is the same list the app ships with.
3. Open https://samupdater.com/wp-json/samupdater-app/v1/catalog to check it.

== Caching and limits ==

Responses carry Cache-Control: public, max-age=900, so a page cache or CDN can serve them.

Each IP gets 60 requests a minute. Behind Cloudflare, restore the visitor IP at the server, or change the limit:

add_filter( 'samupdater_app_api_rate_limit', fn() => 120 ); // 0 turns it off

== Changelog ==

= 1.0.0 =
* First release.
