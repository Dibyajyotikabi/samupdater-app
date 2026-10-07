<?php
/**
 * Keeps the catalog in a single non-autoloaded option, seeded from data/catalog-seed.json.
 */

namespace SamUpdater\AppApi;

defined( 'ABSPATH' ) || exit;

final class Catalog_Store {

	const OPTION    = 'samupdater_app_catalog';
	const SEED_FILE = 'data/catalog-seed.json';

	/** Activation hook. Never overwrites a catalog you already edited. */
	public static function install() {
		if ( false === get_option( self::OPTION ) ) {
			add_option( self::OPTION, self::with_timestamp( self::seed() ), '', false );
		}
	}

	public static function get() {
		$stored = get_option( self::OPTION );
		return is_array( $stored ) ? $stored : self::seed();
	}

	/** Expects a catalog that already went through Catalog_Schema::sanitize(). */
	public static function save( array $catalog ) {
		return update_option( self::OPTION, self::with_timestamp( $catalog ), false );
	}

	public static function seed() {
		$path = SAMUPDATER_APP_API_DIR . self::SEED_FILE;
		$raw  = is_readable( $path ) ? file_get_contents( $path ) : false;
		if ( false === $raw ) {
			error_log( 'Sam Updater App API: seed file missing at ' . $path );
			return Catalog_Schema::sanitize( array() )['catalog'];
		}
		return Catalog_Schema::sanitize( json_decode( $raw, true ) )['catalog'];
	}

	private static function with_timestamp( array $catalog ) {
		return array( 'updatedAt' => gmdate( 'Y-m-d\TH:i:s\Z' ) ) + $catalog;
	}
}
